package ru.scoltech.openran.speedtest.manager

import android.content.Context
import io.swagger.client.model.ServerAddressResponse
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.parser.MultithreadedIperfOutputParser
import ru.scoltech.openran.speedtest.task.TaskChain
import ru.scoltech.openran.speedtest.task.TaskChainBuilder
import ru.scoltech.openran.speedtest.task.impl.*
import ru.scoltech.openran.speedtest.util.SkipThenAverageEqualizer
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.LongConsumer
import kotlin.concurrent.withLock

class DownloadUploadSpeedTestManager
private constructor(
    private val context: Context,
    private val onPingUpdate: (Long) -> Unit,
    private val onDownloadStart: () -> Unit,
    private val onDownloadSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onDownloadFinish: (LongSummaryStatistics) -> Unit,
    private val onUploadStart: () -> Unit,
    private val onUploadSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onUploadFinish: (LongSummaryStatistics) -> Unit,
    private val onFinish: () -> Unit,
    private val onStop: () -> Unit,
    private val onLog: (String, String, Exception?) -> Unit,
    private val onFatalError: (String, Exception?) -> Unit,
) {
    private val lock = ReentrantLock()
    private var taskChain: TaskChain<*>? = null

    fun start(useBalancer: Boolean, mainAddress: String, idleBetweenTasksMelees: Long) {
        val localTaskChain = if (useBalancer) {
            buildChainUsingBalancer(idleBetweenTasksMelees)
        } else {
            buildDirectIperfChain()
        }

        lock.withLock {
            taskChain = localTaskChain.apply { start(mainAddress) }
        }
    }

    fun buildChainUsingBalancer(idleBetweenTasksMelees: Long): TaskChain<String> {
        val balancerApiBuilder = BalancerApiBuilder()
            .setConnectTimeout(DEFAULT_TIMEOUT)
            .setReadTimeout(DEFAULT_TIMEOUT)
            .setWriteTimeout(DEFAULT_TIMEOUT)

        val obtainServiceAddressesTask = ObtainServiceAddressesTask(balancerApiBuilder)

        // get IPERF arguments
        val iperfPref = context.getSharedPreferences(
            context.getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE)
        val DOWNLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            context.getString(R.string.download_device_args),
            context.getString(R.string.default_download_device_iperf_args)
        )
        val DOWNLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            context.getString(R.string.download_server_args),
            context.getString(R.string.default_download_server_iperf_args)
        )
        val UPLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            context.getString(R.string.upload_device_args),
            context.getString(R.string.default_upload_device_iperf_args)
        )
        val UPLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            context.getString(R.string.upload_server_args),
            context.getString(R.string.default_upload_server_iperf_args)
        )

        val startServiceIperfTask = StartServiceIperfTask(
            balancerApiBuilder,
            "-s ${context.getString(R.string.immutable_download_server_args)} $DOWNLOAD_SERVER_IPERF_ARGS"
        )
        val stopServiceIperfTask = StopServiceIperfTask(balancerApiBuilder)

        val startIperfTask = StartIperfTask(
            context.filesDir.absolutePath,
            "${context.getString(R.string.immutable_download_device_args)} $DOWNLOAD_DEVICE_IPERF_ARGS",
            MultithreadedIperfOutputParser(),
            SkipThenAverageEqualizer(
                DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP,
                DEFAULT_EQUALIZER_MAX_STORING
            ),
            balancerApiBuilder.connectTimeout.toLong(),
            onDownloadStart,
            onDownloadSpeedUpdate,
            onDownloadFinish,
            onLog
        )

        val balancerAddress = AtomicReference<InetSocketAddress>()
        val chainBuilder = TaskChainBuilder<String>().onFatalError(onFatalError).onStop(onStop)
        chainBuilder.initializeNewChain()
            .andThen(ParseAddressTask())
            .andThenUnstoppable {
                balancerAddress.set(it)
                it
            }
            .andThen(obtainServiceAddressesTask)
            .andThenUnstoppable { listOf(it) }
            .andThen(
                PingServiceAddressesTask(
                    balancerApiBuilder.connectTimeout.toLong(),
                    onPingUpdate
                )
            )
            .andThenTry(startServiceIperfTask) {
                andThen(startIperfTask)
            }.andThenFinally(stopServiceIperfTask)
            .andThen(DelayTask(idleBetweenTasksMelees))
            .andThenUnstoppable { balancerAddress.get() }
            .andThen(obtainServiceAddressesTask)
            .andThenTry(startServiceIperfTask.copy(args = "-s ${context.getString(R.string.immutable_upload_server_args)} $UPLOAD_SERVER_IPERF_ARGS")) {
                andThen(
                    startIperfTask.copy(
                        args = "${context.getString(R.string.immutable_upload_device_args)} $UPLOAD_DEVICE_IPERF_ARGS",
                        speedEqualizer = SkipThenAverageEqualizer(
                            DEFAULT_EQUALIZER_UPLOAD_VALUES_SKIP,
                            DEFAULT_EQUALIZER_MAX_STORING
                        ),
                        onStart = onUploadStart,
                        onSpeedUpdate = onUploadSpeedUpdate,
                        onFinish = onUploadFinish,
                    )
                )
            }.andThenFinally(stopServiceIperfTask)
            .andThenUnstoppable { onFinish() }
        return chainBuilder.finishChainCreation()
    }

    fun buildDirectIperfChain(): TaskChain<String> {
        val iperfPref = context.getSharedPreferences(
            context.getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE)
        val DOWNLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            context.getString(R.string.download_device_args),
            context.getString(R.string.default_download_device_iperf_args)
        )

        val chainBuilder = TaskChainBuilder<String>().onFatalError(onFatalError).onStop(onStop)
        chainBuilder.initializeNewChain()
            .andThen(ParseAddressTask())
            .andThenUnstoppable {
                listOf(ServerAddressResponse().ip(it.address.hostAddress).portIperf(it.port))
            }
            .andThen(PingServiceAddressesTask(DEFAULT_TIMEOUT.toLong(), onPingUpdate))
            .andThen(
                StartIperfTask(
                    context.filesDir.absolutePath,
                    "${context.getString(R.string.immutable_download_device_args)} $DOWNLOAD_DEVICE_IPERF_ARGS",
                    MultithreadedIperfOutputParser(),
                    SkipThenAverageEqualizer(
                        DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP,
                        DEFAULT_EQUALIZER_MAX_STORING
                    ),
                    DEFAULT_TIMEOUT.toLong(),
                    onDownloadStart,
                    onDownloadSpeedUpdate,
                    onDownloadFinish,
                    onLog
                )
            )
            .andThenUnstoppable { onFinish() }
        return chainBuilder.finishChainCreation()
    }

    fun stop() {
        lock.withLock {
            taskChain?.stop()
        }
    }

    class Builder(private val context: Context) {
        private var onPingUpdate: LongConsumer = LongConsumer {}
        private var onDownloadStart: Runnable = Runnable {}
        private var onDownloadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long> =
            BiConsumer { _, _ -> }
        private var onDownloadFinish: Consumer<LongSummaryStatistics> = Consumer {}
        private var onUploadStart: Runnable = Runnable {}
        private var onUploadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long> =
            BiConsumer { _, _ -> }
        private var onUploadFinish: Consumer<LongSummaryStatistics> = Consumer {}
        private var onFinish: Runnable = Runnable {}
        private var onStop: Runnable = Runnable {}
        private var onLog: (String, String, Exception?) -> Unit = { _, _, _ -> }
        private var onFatalError: BiConsumer<String, Exception?> = BiConsumer { _, _ -> }

        fun build(): DownloadUploadSpeedTestManager {
            return DownloadUploadSpeedTestManager(
                context,
                onPingUpdate::accept,
                onDownloadStart::run,
                onDownloadSpeedUpdate::accept,
                onDownloadFinish::accept,
                onUploadStart::run,
                onUploadSpeedUpdate::accept,
                onUploadFinish::accept,
                onFinish::run,
                onStop::run,
                onLog::invoke,
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

        fun onDownloadSpeedUpdate(onDownloadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long>): Builder {
            this.onDownloadSpeedUpdate = onDownloadSpeedUpdate
            return this
        }

        fun onDownloadFinish(onDownloadFinish: Consumer<LongSummaryStatistics>): Builder {
            this.onDownloadFinish = onDownloadFinish
            return this
        }

        fun onUploadStart(onUploadStart: Runnable): Builder {
            this.onUploadStart = onUploadStart
            return this
        }

        fun onUploadSpeedUpdate(onUploadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long>): Builder {
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

        fun onStop(onStop: Runnable): Builder {
            this.onStop = onStop
            return this
        }

        fun onLog(onLog: (String, String, Exception?) -> Unit): Builder {
            this.onLog = onLog
            return this
        }

        fun onFatalError(onFatalError: BiConsumer<String, Exception?>): Builder {
            this.onFatalError = onFatalError
            return this
        }
    }

    companion object {
//        private const val IMMUTABLE_COMMON_DEVICE_ARGS = "-f b -i 0.1 --sum-only"
//        private const val IMMUTABLE_DOWNLOAD_DEVICE_ARGS = "-u -R" + IMMUTABLE_COMMON_DEVICE_ARGS
//        private const val IMMUTABLE_DOWNLOAD_SERVER_ARGS = "-u"
//        private const val IMMUTABLE_UPLOAD_DEVICE_ARGS   = "" + IMMUTABLE_COMMON_DEVICE_ARGS
//        private const val IMMUTABLE_UPLOAD_SERVER_ARGS   = ""

        private const val DEFAULT_TIMEOUT = 1000
        private const val DEFAULT_EQUALIZER_MAX_STORING = 4
        private const val DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP = 0
        private const val DEFAULT_EQUALIZER_UPLOAD_VALUES_SKIP = 1
    }
}
