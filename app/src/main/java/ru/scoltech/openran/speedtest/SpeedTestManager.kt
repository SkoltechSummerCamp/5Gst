package ru.scoltech.openran.speedtest

import android.content.Context
import android.util.Log
import io.swagger.client.ApiClient
import io.swagger.client.ApiException
import io.swagger.client.api.ClientApi
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.iperf.IperfException
import ru.scoltech.openran.speedtest.iperf.IperfRunner
import ru.scoltech.openran.speedtest.parser.IperfOutputParser
import ru.scoltech.openran.speedtest.parser.MultithreadedIperfOutputParser
import ru.scoltech.openran.speedtest.util.IdleTaskKiller
import java.io.IOException
import java.lang.Exception
import java.lang.Runnable
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.LongConsumer
import java.util.function.ToLongFunction
import kotlin.concurrent.withLock
import kotlin.math.roundToLong

class SpeedTestManager
private constructor(
    context: Context,
    private val onPingUpdate: (Long) -> Unit,
    private val onDownloadStart: () -> Unit,
    private val onDownloadSpeedUpdate: (Long) -> Unit,
    private val onDownloadFinish: (LongSummaryStatistics) -> Long,
    private val onUploadStart: () -> Unit,
    private val onUploadSpeedUpdate: (Long) -> Unit,
    private val onUploadFinish: (LongSummaryStatistics) -> Unit,
    private val onFinish: () -> Unit,
    private val onStopped: () -> Unit,
    private val onLog: (String, String) -> Unit,
    private val onFatalError: (String) -> Unit,
) {
    private var downloadSpeedStatistics = LongSummaryStatistics()
    private var uploadSpeedStatistics = LongSummaryStatistics()

    @Volatile
    private var state = State.NONE

    @Volatile
    private lateinit var serverAddress: MyServerAddr

    @Volatile
    private lateinit var mainAddress: InetSocketAddress

    @Volatile
    private var useBalancer: Boolean = true

    private val lock = ReentrantLock()

    private val idleTaskKiller: IdleTaskKiller = IdleTaskKiller()
    private val speedParser: IperfOutputParser = MultithreadedIperfOutputParser()

    private val iperfRunner = IperfRunner.Builder(context.filesDir.absolutePath)
        .stdoutLinesHandler(this::handleIperfStdout)
        .stderrLinesHandler(this::onIperfStderrLine)
        .onFinishCallback(this::onIperfFinish)
        .build()

    fun start(useBalancer: Boolean, mainAddress: String) {
        // TODO wait until stop
        lock.withLock {
            downloadSpeedStatistics = LongSummaryStatistics()
            uploadSpeedStatistics = LongSummaryStatistics()

            // TODO check if current state is not NONE
            state = State.NONE
        }

        this.useBalancer = useBalancer
        CoroutineScope(Dispatchers.IO).launch {
            runCatchingStop {
                this@SpeedTestManager.mainAddress = try {
                    parseInetSocketAddress(
                        mainAddress,
                        if (this@SpeedTestManager.useBalancer) {
                            ApplicationConstants.DEFAULT_BALANCER_PORT
                        } else {
                            ApplicationConstants.DEFAULT_IPERF_SERVER_PORT
                        }
                    )
                } catch (e: UnknownHostException) {
                    throw FatalException(
                        "Could not parse address: ${e::class.qualifiedName}: ${e.message}"
                    )
                }

                val addresses = if (useBalancer) {
                    obtainAddresses() ?: return@launch
                } else {
                    val iperfAddress = this@SpeedTestManager.mainAddress
                    listOf(MyServerAddr(iperfAddress.address.hostAddress, 0, iperfAddress.port))
                }

                checkStop()
                val serverInfo = addresses
                    .map { it to getPing(it) }
                    .minByOrNull { it.second }
                    ?: return@launch stopWithFatalError("No servers are available right now")

                onPingUpdate(serverInfo.second)
                serverAddress = serverInfo.first
                checkStop()
                startDownload()
            }
        }
    }

    private fun obtainAddresses(): List<MyServerAddr>? {
        val balancerAddress = "http://$mainAddress/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0"
        val addresses = try {
            ClientApi(ApiClient().setBasePath(balancerAddress)).clientObtainIp()
        } catch (e: ApiException) {
            stopWithFatalError("Could not connect to balancer", e)
            return null
        }

        if (addresses.isEmpty()) {
            stopWithFatalError("Could not obtain server ip")
            return null
        }

        if (addresses.any { it.ip == null }) {
            // TODO make required at api level
            stopWithFatalError("Balancer did not provide required field (ip)")
            return null
        }
        return addresses.map { MyServerAddr(it.ip!!, it.port, it.portIperf) }
    }

    private suspend fun getPing(address: MyServerAddr): Long {
        val icmpPing = ICMPPing()

        val deferred = CoroutineScope(Dispatchers.IO).async {
            var ping = ""
            icmpPing.justPingByHost(address.ip) {
                ping = it
                icmpPing.stopExecuting()
            }
            ping
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            icmpPing.stopExecuting()
        }
        val ping = deferred.await()
        checkStop()
        return ping.toDoubleOrNull()?.roundToLong()
            ?: throw FatalException(
                if (ping == "") "Icmp ping timed out" else "Could not parse ping value $ping"
            )
    }

    private suspend fun startDownload() {
        startTest("-u -R", "-u") {
            onDownloadStart()
            state = State.DOWNLOAD
        }
    }

    private suspend fun startUpload() {
        runCatchingStop {
            checkStop()
            if (useBalancer) {
                serverAddress = obtainAddresses()?.first() ?: return
            }
            startTest {
                onUploadStart()
                state = State.UPLOAD
            }
        }
    }

    private suspend inline fun startTest(
        additionalClientArgs: String = "",
        additionalServerArgs: String = "",
        beforeStart: () -> Unit,
    ) {
        runCatchingStop {
            sendRequestToService("-s $additionalServerArgs", RequestType.START)
            checkStop()
            lock.withLock {
                beforeStart()
            }

            while (true) {
                try {
                    iperfRunner.start(
                        "-c ${serverAddress.ip} -p ${serverAddress.portIperf} " +
                                "-f b -P 10 --sum-only -i 0.1 -b 120m $additionalClientArgs"
                    )
                    idleTaskKiller.register(IPERF_IDLE_TIME) {
                        iperfRunner.sendSigKill()
                    }
                    break
                } catch (e: InterruptedException) {
                    val message = "Interrupted iPerf start. Ignoring..."
                    Log.e(LOG_TAG, message, e)
                    onLog(LOG_TAG, message)
                } catch (e: IperfException) {
                    stopWithFatalError("Could not start iPerf", e)
                    return
                }
            }
        }
    }

    private suspend fun sendRequestToService(serverArgs: String, requestType: RequestType) {
        if (useBalancer) {
            val serverMessage = try {
                sendGETRequest(
                    "${serverAddress.ip}:${serverAddress.port}",
                    requestType,
                    1000L,
                    serverArgs,
                )
            } catch (e: Exception) {
                throw FatalException(
                    "Could not connect to server: ${e::class.qualifiedName}: ${e.message}"
                )
            }
            if (serverMessage == "error") {
                // TODO server message
                throw FatalException(serverMessage)
            }
        }
    }

    fun stop() {
        lock.withLock {
            state = State.STOPPED
        }

        try {
            iperfRunner.sendSigKill()
        } catch (e: IperfException) {
            lock.withLock {
                stopWithFatalError("Could not send SIGKILL to iPerf", e)
                state = State.STOPPED
            }
        }
    }

    // TODO stop without checking field
    private fun checkStop() {
        lock.withLock {
            if (state == State.STOPPED) {
                throw StopException()
            }
        }
    }

    private inline fun runCatchingStop(block: () -> Unit) {
        try {
            block()
        } catch (e: StopException) {
            Log.i(LOG_TAG, "Stopped", e)
            onStopped()
            lock.withLock {
                state = State.NONE
            }
        } catch (e: FatalException) {
            stopWithFatalError("Fatal error", e)
        }
    }

    private fun stopWithFatalError(message: String, exception: Exception? = null) {
        lock.withLock {
            if (exception != null) {
                Log.e(LOG_TAG, message, exception)
                onFatalError("$message: ${exception.message}")
            } else {
                Log.e(LOG_TAG, message)
                onFatalError(message)
            }
            state = State.NONE
        }
    }

    private fun handleIperfStdout(line: String) {
        onLog("iPerf stdout", line)
        lock.withLock {
            idleTaskKiller.updateTaskState()
            val speed = try {
                speedParser.parseSpeed(line)
            } catch (e: IOException) {
                val message = "Invalid stdout format: $line"
                if (e !is MultithreadedIperfOutputParser.BadFormatException) {
                    Log.e(LOG_TAG, message, e)
                } else {
                    Log.e(LOG_TAG, message)
                    Log.e(LOG_TAG, e.message.toString())
                }
                onLog(LOG_TAG, message)
                e.message?.let { onLog(LOG_TAG, it) }
                return
            }
            if (state == State.DOWNLOAD) {
                onDownloadSpeedUpdate(speed)
                downloadSpeedStatistics.accept(speed)
            } else if (state == State.UPLOAD) {
                onUploadSpeedUpdate(speed)
                uploadSpeedStatistics.accept(speed)
            }
        }
    }

    private fun onIperfStderrLine(line: String) {
        idleTaskKiller.updateTaskState()
        onLog("iPerf stderr", line)
    }

    private fun onIperfFinish() {
        lock.withLock {
            runBlocking {
                idleTaskKiller.unregister()
            }
        }

        runCatchingStop {
            if (useBalancer) {
                runBlocking {
                    sendRequestToService("", RequestType.STOP)
                }
            }
            lock.withLock {
                when (state) {
                    State.DOWNLOAD -> {
                        val delayBeforeUpload = onDownloadFinish(downloadSpeedStatistics)
                        if (useBalancer) {
                            CoroutineScope(Dispatchers.IO).launch {
                                // TODO cancel on stop
                                delay(delayBeforeUpload)
                                startUpload()
                            }
                        } else {
                            onFinish()
                            state = State.NONE
                        }
                    }
                    State.UPLOAD -> {
                        onUploadFinish(uploadSpeedStatistics)
                        onFinish()
                        state = State.NONE
                    }
                    State.STOPPED -> {
                        onStopped()
                        state = State.NONE
                    }
                    State.NONE -> {
                        val message = "Invalid manager state (NONE) on iperf stop"
                        Log.e(LOG_TAG, message)
                        onLog(LOG_TAG, message)
                    }
                }
            }
        }
    }

    private enum class State {
        NONE, DOWNLOAD, UPLOAD, STOPPED
    }

    // TODO remove when balancer api is ready
    private data class MyServerAddr(val ip: String, val port: Int, val portIperf: Int)

    class Builder(private val context: Context) {
        private var onPingUpdate: LongConsumer = LongConsumer {}
        private var onDownloadStart: Runnable = Runnable {}
        private var onDownloadSpeedUpdate: LongConsumer = LongConsumer {}
        private var onDownloadFinish: ToLongFunction<LongSummaryStatistics> = ToLongFunction { 0 }
        private var onUploadStart: Runnable = Runnable {}
        private var onUploadSpeedUpdate: LongConsumer = LongConsumer {}
        private var onUploadFinish: Consumer<LongSummaryStatistics> = Consumer {}
        private var onFinish: Runnable = Runnable {}
        private var onStopped: Runnable = Runnable {}
        private var onLog: BiConsumer<String, String> = BiConsumer { _, _ -> }
        private var onFatalError: Consumer<String> = Consumer {}

        fun build(): SpeedTestManager {
            return SpeedTestManager(
                context,
                onPingUpdate::accept,
                onDownloadStart::run,
                onDownloadSpeedUpdate::accept,
                onDownloadFinish::applyAsLong,
                onUploadStart::run,
                onUploadSpeedUpdate::accept,
                onUploadFinish::accept,
                onFinish::run,
                onStopped::run,
                onLog::accept,
                onFatalError::accept,
            )
        }

        fun onPingUpdate(onPingUpdate: LongConsumer): Builder {
            this.onPingUpdate = onPingUpdate
            return this
        }

        fun onDownloadStart(onDownloadStart: Runnable): Builder {
            this.onDownloadStart = onDownloadStart
            return this
        }

        fun onDownloadSpeedUpdate(onDownloadSpeedUpdate: LongConsumer): Builder {
            this.onDownloadSpeedUpdate = onDownloadSpeedUpdate
            return this
        }

        fun onDownloadFinish(onDownloadFinish: ToLongFunction<LongSummaryStatistics>): Builder {
            this.onDownloadFinish = onDownloadFinish
            return this
        }

        fun onUploadStart(onUploadStart: Runnable): Builder {
            this.onUploadStart = onUploadStart
            return this
        }

        fun onUploadSpeedUpdate(onUploadSpeedUpdate: LongConsumer): Builder {
            this.onUploadSpeedUpdate = onUploadSpeedUpdate
            return this
        }

        fun onUploadFinish(onUploadFinish: Consumer<LongSummaryStatistics>): Builder {
            this.onUploadFinish = onUploadFinish
            return this
        }

        fun onFinish(onFinish: Runnable): Builder {
            this.onFinish = onFinish
            return this
        }

        fun onStopped(onStopped: Runnable): Builder {
            this.onStopped = onStopped
            return this
        }

        fun onLog(onLog: BiConsumer<String, String>): Builder {
            this.onLog = onLog
            return this
        }

        fun onFatalError(onFatalError: Consumer<String>): Builder {
            this.onFatalError = onFatalError
            return this
        }
    }

    private class StopException : RuntimeException()
    private class FatalException(message: String) : RuntimeException(message)

    companion object {
        private const val LOG_TAG = "SpeedTestManager"
        private const val IPERF_IDLE_TIME = 1000L
    }
}
