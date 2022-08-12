package ru.scoltech.openran.speedtest.manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.client.balancer.model.ServerAddressResponse
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
    private val onDownloadStart: (StageInfo) -> Unit,
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

        val pipelineCount = context.getSharedPreferences("pipeline_count",
            Context.MODE_PRIVATE).getString("0", "0").toString().toInt()

        val balancerAddress = AtomicReference<InetSocketAddress>()
        val chainBuilder = TaskChainBuilder<String>().onFatalError(onFatalError).onStop(onStop)
        var taskConsumer = chainBuilder.initializeNewChain()
            .andThen(ParseAddressTask())
            .andThenUnstoppable {
                balancerAddress.set(it)
                it
            }

//TODO ewdvebre
        for(index in 0 until pipelineCount) {
            val (str1, str2, str3) = context.getSharedPreferences(
            "iperf_args_pipeline_$index", AppCompatActivity.MODE_PRIVATE)
            .getString("0", "\n\n").toString().split('\n')
            println("$str1 $str2")
            val startServiceIperfTask = StartServiceIperfTask(
                balancerApiBuilder,
                str2
            )
            val stopServiceIperfTask = StopServiceIperfTask(balancerApiBuilder)

            val startIperfTask = StartIperfTask(
                context.filesDir.absolutePath,
                str3,
                MultithreadedIperfOutputParser(),
                SkipThenAverageEqualizer(
                    DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP,
                    DEFAULT_EQUALIZER_MAX_STORING
                ),
                balancerApiBuilder.connectTimeout.toLong(),
                onDownloadSpeedUpdate,
                onDownloadFinish,
                onLog
            )

            val stageInfo = StageInfo(str1)

            taskConsumer = taskConsumer.andThen(obtainServiceAddressesTask)
                .andThenUnstoppable { listOf(it) }
                .andThen(
                    PingServiceAddressesTask(
                        balancerApiBuilder.connectTimeout.toLong(),
                        onPingUpdate
                    )
                )
                .andThenTry(startServiceIperfTask) {
                    andThenUnstoppable {
                        onDownloadStart(stageInfo)
                        it
                    }
                        .andThen(startIperfTask)
                }.andThenFinally(stopServiceIperfTask)
                //
                .andThen(DelayTask(idleBetweenTasksMelees))
                .andThenUnstoppable { balancerAddress.get() }
        }
            taskConsumer.andThenUnstoppable { onFinish() }
// TODO rebewrbe
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
            .andThenUnstoppable {
                onDownloadStart(StageInfo("Direct iperf stage"))
                it
            }
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

    // TODO use in SetupPipelineTab
    data class StageInfo(
        val name: String,
    )

    class Builder(private val context: Context) {
        private var onPingUpdate: LongConsumer = LongConsumer {}
        private var onDownloadStart: Consumer<StageInfo> = Consumer {}
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
                onDownloadStart::accept,
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

        fun onDownloadStart(onDownloadStart: Consumer<StageInfo>): Builder {
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

        private const val DEFAULT_TIMEOUT = 5_000
        private const val DEFAULT_EQUALIZER_MAX_STORING = 4
        private const val DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP = 0
        private const val DEFAULT_EQUALIZER_UPLOAD_VALUES_SKIP = 1
    }
}
