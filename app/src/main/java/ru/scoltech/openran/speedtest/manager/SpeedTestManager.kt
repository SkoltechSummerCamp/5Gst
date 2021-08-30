package ru.scoltech.openran.speedtest.manager

import android.content.Context
import android.util.Log
import com.squareup.okhttp.HttpUrl
import io.swagger.client.model.ServerAddr
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.backend.*
import ru.scoltech.openran.speedtest.parser.IperfOutputParser
import ru.scoltech.openran.speedtest.parser.MultithreadedIperfOutputParser
import ru.scoltech.openran.speedtest.util.IdleTaskKiller
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.*
import kotlin.concurrent.withLock

typealias BalancerApi = io.swagger.client.api.ClientApi
typealias BalancerApiBuilder = io.swagger.client.ApiClient
typealias BalancerApiCallback<T> = io.swagger.client.ApiCallback<T>
typealias BalancerApiException = io.swagger.client.ApiException

class SpeedTestManager
private constructor(
    private val context: Context,
    private val clientArgs: String,
    private val serverArgs: String,
    private val mainAddress: InetSocketAddress,
    private val balancerPathSegments: List<String>,
    private val balancerApiBuilder: BalancerApiBuilder,
    private val speedParser: IperfOutputParser,
    private val useBalancer: Boolean,
    private val checkPing: Boolean,
    private val onPingUpdate: (Long) -> Unit,
    private val onStart: () -> Unit,
    private val onSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onLog: (String, String) -> Unit,
    private val onFinish: (LongSummaryStatistics) -> Unit,
    private val onStop: () -> Unit,
    private val onFatalError: (String) -> Unit,
) {
    private val speedStatistics = LongSummaryStatistics()

    @Volatile
    private var stopped = false

    private val lock = ReentrantLock()

    private val idleTaskKiller: IdleTaskKiller = IdleTaskKiller()
    private val taskKiller = TaskKiller()

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatchingStop(::obtainAddresses)
        }
    }

    private fun obtainAddresses() {
        if (!useBalancer) {
            runCatchingStop {
                val iperfAddress = ServerAddr()
                    .ip(mainAddress.address.hostAddress)
                    .portIperf(mainAddress.port)
                    .port(0)
                ObtainIpCallback().onSuccess(listOf(iperfAddress), 0, mutableMapOf())
            }
            return
        }

        val balancerAddress = HttpUrl.Builder()
            .scheme("http")
            .host(mainAddress.address.hostAddress)
            .port(mainAddress.port)
            .addPathSegments(balancerPathSegments)
            .build()
            .toString()

        lock.withLock {
            requireNotStopped()
            try {
                val call = BalancerApi(balancerApiBuilder.setBasePath(balancerAddress))
                    .clientObtainIpAsync(ObtainIpCallback())
                taskKiller.register {
                    call.cancel()
                }
            } catch (e: BalancerApiException) {
                throw FatalException("Could not create balancer api call", e)
            }
        }
    }

    private inner class ObtainIpCallback : BalancerApiCallback<List<ServerAddr>> {
        override fun onFailure(
            e: BalancerApiException,
            statusCode: Int,
            responseHeaders: MutableMap<String, MutableList<String>>?,
        ) {
            lock.withLock {
                if (stopped) {
                    onStop()
                } else {
                    val statusCodeMessage = if (statusCode != 0) {
                        " (status code = $statusCode)"
                    } else {
                        ""
                    }
                    stopWithFatalError(
                        "Could not connect to balancer$statusCodeMessage",
                        e
                    )
                }
            }
        }

        override fun onSuccess(
            result: List<ServerAddr>,
            statusCode: Int,
            responseHeaders: MutableMap<String, MutableList<String>>,
        ) {
            when {
                result.isEmpty() ->
                    stopWithFatalError("Could not obtain server ip")
                result.any { it.ip == null } ->
                    stopWithFatalError("Balancer did not provide required field (ip)")
                result.size != 1 ->
                    // TODO support many addresses
                    stopWithFatalError("Right now more than one ip is not supported")
                else -> runCatchingStop {
                    val address = result[0]
                    val myAddress = MyServerAddr(address.ip, address.port, address.portIperf)
                    if (!checkPing) {
                        requestServiceStart(myAddress)
                        return@runCatchingStop
                    }
                    getPing(myAddress)
                        .onSuccess {
                            onPingUpdate(it)
                            runCatchingStop {
                                requestServiceStart(myAddress)
                            }
                        }
                        .onError {
                            if (stopped) {
                                onStop()
                            } else {
                                stopWithFatalError("Could not get ping using icmp", it)
                            }
                        }
                        .start()
                }
            }
        }

        override fun onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {}
        override fun onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean) {}
    }

    private fun getPing(address: MyServerAddr): Promise<(Long) -> Unit, (Exception) -> Unit> {
        return Promise { onSuccess, onError ->
            val icmpPinger = IcmpPinger()
            lock.withLock {
                requireNotStopped()
                taskKiller.register { icmpPinger.stop() }
                val promise = icmpPinger.pingOnce(
                    address.ip,
                    balancerApiBuilder.connectTimeout.toLong()
                )
                onSuccess?.let { promise.onSuccess(it) }
                onError?.let { promise.onError(it) }
                promise.start()
            }
        }
    }

    private fun requestServiceStart(serviceAddress: MyServerAddr) {
        if (!useBalancer) {
            runCatchingStop {
                startIperfTest(serviceAddress)
            }
            return
        }
        val serviceApi = ServiceApi(balancerApiBuilder.httpClient)

        lock.withLock {
            requireNotStopped()
            taskKiller.register(serviceApi::cancelStartIperf)
            // TODO check request/response body for memory leaks
            serviceApi.startIperf(serviceAddress.ip, serviceAddress.port, serverArgs)
                .onSuccess {
                    runCatchingStop {
                        startIperfTest(serviceAddress)
                    }
                }
                .onError { _, e ->
                    if (stopped) {
                        onStop()
                    } else {
                        stopWithFatalError("Could not connect to the server", e)
                    }
                }
                .start()
        }
    }

    private fun startIperfTest(serviceAddress: MyServerAddr) {
        val iperfRunner = IperfRunner.Builder(context.filesDir.absolutePath)
            .stdoutLinesHandler(this::onIperfStdoutLine)
            .stderrLinesHandler(this::onIperfStderrLine)
            .onFinishCallback {
                lock.withLock {
                    runCatchingStop {
                        requestServiceStop(serviceAddress)
                    }
                }
            }
            .build()

        onStart()
        lock.withLock {
            requireNotStopped()
            while (true) {
                try {
                    // TODO validate not to have -c and -p in command
                    iperfRunner.start(
                        "-c ${serviceAddress.ip} -p ${serviceAddress.portIperf} $clientArgs"
                    )

                    val task = {
                        try {
                            iperfRunner.sendSigKill()
                        } catch (e: IperfException) {
                            onLog(LOG_TAG, "Could not stop iPerf".withExceptionLogMessage(e))
                        }
                    }
                    idleTaskKiller.registerBlocking(balancerApiBuilder.connectTimeout.toLong(), task)
                    taskKiller.register(task)
                    break
                } catch (e: InterruptedException) {
                    val message = "Interrupted iPerf start. Ignoring..."
                    onLog(LOG_TAG, message)
                } catch (e: IperfException) {
                    stopWithFatalError("Could not start iPerf", e)
                    return
                }
            }
        }
    }

    private fun onIperfStdoutLine(line: String) {
        onLog("iPerf stdout", line)
        lock.withLock {
            idleTaskKiller.updateTaskState()
            val speed = try {
                speedParser.parseSpeed(line)
            } catch (e: IOException) {
                onLog(LOG_TAG, "Invalid stdout format".withExceptionLogMessage(e))
                return
            }
            speedStatistics.accept(speed)
            onSpeedUpdate(speedStatistics, speed)
        }
    }

    private fun onIperfStderrLine(line: String) {
        onLog("iPerf stderr", line)
        idleTaskKiller.updateTaskState()
    }

    private fun requestServiceStop(serviceAddress: MyServerAddr) {
        if (useBalancer) {
            ServiceApi(balancerApiBuilder.httpClient)
                .stopIperf(serviceAddress.ip, serviceAddress.port)
        }
        requireNotStopped()
        onFinish(speedStatistics)
    }

    fun stop() {
        lock.withLock {
            stopped = true
            taskKiller.kill()
        }
    }

    private fun requireNotStopped() {
        lock.withLock {
            if (stopped) {
                throw StopException()
            }
        }
    }

    private inline fun runCatchingStop(block: () -> Unit) {
        try {
            block()
        } catch (e: StopException) {
            Log.i(LOG_TAG, "Stopped", e)
            onStop()
        } catch (e: FatalException) {
            stopWithFatalError("Fatal error", e.cause)
        }
    }

    private fun stopWithFatalError(message: String, cause: Throwable? = null) {
        onFatalError(message.withExceptionLogMessage(cause))
    }

    private fun String.withExceptionLogMessage(t: Throwable? = null): String {
        val errorMessage = t?.let { "; ${t::class.qualifiedName}: ${t.message}" } ?: ""
        return "$this$errorMessage"
    }

    // TODO remove when balancer api is ready
    private data class MyServerAddr(val ip: String, val port: Int, val portIperf: Int)

    class Builder(
        private val context: Context,
        private var clientArgs: String,
        private var mainAddress: InetSocketAddress,
    ) {
        private var serverArgs: String = "-s"
        private var balancerRequestSegments: List<String> = listOf(
            "Skoltech_OpenRAN_5G", "iperf_load_balancer", io.swagger.client.Version.VERSION
        )
        private var balancerApiBuilder = BalancerApiBuilder()
            .setConnectTimeout(DEFAULT_TIMEOUT)
            .setReadTimeout(DEFAULT_TIMEOUT)
            .setWriteTimeout(DEFAULT_TIMEOUT)
        private var speedParser: IperfOutputParser = MultithreadedIperfOutputParser()
        private var useBalancer: Boolean = true
        private var checkPing: Boolean = true
        private var onPingUpdate: (Long) -> Unit = {}
        private var onStart: () -> Unit = {}
        private var onSpeedUpdate: (LongSummaryStatistics, Long) -> Unit = { _, _ -> }
        private var onLog: (String, String) -> Unit = { _, _ -> }
        private var onFinish: (LongSummaryStatistics) -> Unit = {}
        private var onStop: () -> Unit = {}
        private var onFatalError: (String) -> Unit = {}

        fun build(): SpeedTestManager {
            return SpeedTestManager(
                context,
                clientArgs,
                serverArgs,
                mainAddress,
                balancerRequestSegments,
                balancerApiBuilder,
                speedParser,
                useBalancer,
                checkPing,
                onPingUpdate,
                onStart,
                onSpeedUpdate,
                onLog,
                onFinish,
                onStop,
                onFatalError,
            )
        }

        fun clientArgs(clientArgs: String): Builder {
            this.clientArgs = clientArgs
            return this
        }

        fun mainAddress(mainAddress: InetSocketAddress): Builder {
            this.mainAddress = mainAddress
            return this
        }

        fun serverArgs(serverArgs: String): Builder {
            this.serverArgs = serverArgs
            return this
        }

        fun balancerRequestSegments(balancerRequestSegments: List<String>): Builder {
            this.balancerRequestSegments = balancerRequestSegments
            return this
        }

        fun speedParser(speedParser: IperfOutputParser): Builder {
            this.speedParser = speedParser
            return this
        }

        fun balancerApiBuilder(balancerApiBuilder: BalancerApiBuilder): Builder {
            this.balancerApiBuilder = balancerApiBuilder
            return this
        }

        fun useBalancer(useBalancer: Boolean): Builder {
            this.useBalancer = useBalancer
            return this
        }

        fun directIperfMode(): Builder {
            useBalancer = false
            return this
        }

        fun balancerMode(): Builder {
            useBalancer = true
            return this
        }

        fun checkPing(checkPing: Boolean): Builder {
            this.checkPing = checkPing
            return this
        }

        fun onPingUpdate(onPingUpdate: (Long) -> Unit): Builder {
            this.onPingUpdate = onPingUpdate
            return this
        }

        fun onStart(onStart: () -> Unit): Builder {
            this.onStart = onStart
            return this
        }

        fun onSpeedUpdate(onSpeedUpdate: (LongSummaryStatistics, Long) -> Unit): Builder {
            this.onSpeedUpdate = onSpeedUpdate
            return this
        }

        fun onLog(onLog: (String, String) -> Unit): Builder {
            this.onLog = onLog
            return this
        }

        fun onFinish(onFinish: (LongSummaryStatistics) -> Unit): Builder {
            this.onFinish = onFinish
            return this
        }

        fun onStop(onStop: () -> Unit): Builder {
            this.onStop = onStop
            return this
        }

        fun onFatalError(onFatalError: (String) -> Unit): Builder {
            this.onFatalError = onFatalError
            return this
        }
    }

    private class StopException : RuntimeException()
    private class FatalException(message: String, cause: Exception? = null) :
        RuntimeException(message, cause)

    companion object {
        private const val LOG_TAG = "SpeedTestManager"
        private const val DEFAULT_TIMEOUT = 1000
    }
}
