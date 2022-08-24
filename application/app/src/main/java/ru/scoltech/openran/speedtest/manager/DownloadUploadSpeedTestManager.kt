package ru.scoltech.openran.speedtest.manager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.squareup.okhttp.HttpUrl
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.domain.StageConfiguration
import ru.scoltech.openran.speedtest.parser.MultithreadedIperfOutputParser
import ru.scoltech.openran.speedtest.parser.StageConfigurationParser
import ru.scoltech.openran.speedtest.task.*
import ru.scoltech.openran.speedtest.task.impl.*
import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder
import ru.scoltech.openran.speedtest.util.SkipThenAverageEqualizer
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.LongConsumer
import kotlin.concurrent.withLock

class DownloadUploadSpeedTestManager
private constructor(
    private val context: Context,
    private val onPingUpdate: (Long) -> Unit,
    private val onStageStart: (StageConfiguration) -> Unit,
    private val onStageSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onStageFinish: (LongSummaryStatistics) -> Unit,
    private val onFinish: () -> Unit,
    private val onStop: () -> Unit,
    private val onLog: (String, String, Exception?) -> Unit,
    private val onFatalError: (String, Exception?) -> Unit,
) {
    private val lock = ReentrantLock()
    private var taskChain: TaskChain<*>? = null
    private val stageConfigurationParser = StageConfigurationParser()

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
        val chainBuilder = TaskChainBuilder<String>().onFatalError(onFatalError).onStop(onStop)
        val taskConsumer = chainBuilder.initializeNewChain()
            .andThen(ParseAddressTask())
            .andThenUnstoppable { balancerAddress ->
                val balancerBasePath = HttpUrl.Builder()
                    .scheme("http")  // TODO https
                    .host(balancerAddress.host)
                    .port(balancerAddress.port)
                    .build()
                    .toString()
                    .dropLast(1)  // drops trailing '/'

                BalancerApiBuilder()
                    .setConnectTimeout(DEFAULT_TIMEOUT)
                    .setReadTimeout(DEFAULT_TIMEOUT)
                    .setWriteTimeout(DEFAULT_TIMEOUT)
                    .setBasePath(balancerBasePath)
                    .let { BalancerApi(it) }
            }
            .andThenTry(FiveGstLoginTask()) {
                val obtainServiceAddressTask = ObtainServiceAddressTask(
                    DEFAULT_TIMEOUT,
                    DEFAULT_TIMEOUT,
                    DEFAULT_TIMEOUT,
                )

                initializeNewChain()
                    .andThen(obtainServiceAddressTask)
                    .andThenTry {
                        initializeNewChain()
                            .andThen(StartServiceSessionTask())
                            .let { addStagesToChain(it, idleBetweenTasksMelees) }
                    }
                    .andThenFinally(StopServiceSessionTask())
            }
            .andThenFinally(FiveGstLogoutTask())

        return chainBuilder.finishChainCreation(taskConsumer) {
            onFinish()
        }
    }

    private fun addStagesToChain(
        taskConsumer: TaskConsumer<ApiClientHolder>,
        idleBetweenTasksMelees: Long,
    ): TaskConsumer<ApiClientHolder> {
        val pipelinePreferences = context.getSharedPreferences(
            "iperf_args_pipeline",
            AppCompatActivity.MODE_PRIVATE,
        )
        val immutableDeviceArgsPrefix = context.getString(R.string.immutable_device_args)
        val immutableServerArgsPrefix = context.getString(R.string.immutable_server_args)

        var mutableTaskConsumer = taskConsumer
        stageConfigurationParser.parseFromPreferences(
            pipelinePreferences,
            context::getString,
        ).forEach {  (_, stageConfiguration) ->
            if (stageConfiguration == StageConfiguration.EMPTY) {
                return@forEach
            }

            val startServiceIperfTask = StartServiceIperfTask(
                "$immutableServerArgsPrefix ${stageConfiguration.serverArgs}",
            )
            val stopServiceIperfTask = StopServiceIperfTask()

            val startIperfTask = StartIperfTask(
                context.filesDir.absolutePath,
                "$immutableDeviceArgsPrefix ${stageConfiguration.deviceArgs}",
                MultithreadedIperfOutputParser(),
                SkipThenAverageEqualizer(
                    DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP,
                    DEFAULT_EQUALIZER_MAX_STORING
                ),
                DEFAULT_TIMEOUT.toLong(),
                onStageSpeedUpdate,
                onStageFinish,
                onLog
            )

            mutableTaskConsumer = mutableTaskConsumer
                .withArgumentExtracted { it.iperfAddress }
                .doTask(PingAddressTask(DEFAULT_TIMEOUT.toLong(), onPingUpdate))
                .andThenTry {
                    initializeNewChain()
                        .andThen(startServiceIperfTask)
                        .withArgumentKeptDoUnstoppableTask { onStageStart(stageConfiguration) }
                        .withArgumentExtracted { it.iperfAddress }
                        .doTask(startIperfTask)
                }
                .andThenFinally(stopServiceIperfTask)
                .andThen(DelayTask(idleBetweenTasksMelees))
        }
        return mutableTaskConsumer
    }

    fun buildDirectIperfChain(): TaskChain<String> {
        val deviceArgs = context.getString(R.string.immutable_device_args) +
                " " +
                context.getString(R.string.download_device_iperf_args)

        val chainBuilder = TaskChainBuilder<String>().onFatalError(onFatalError).onStop(onStop)
        val taskConsumer = chainBuilder.initializeNewChain()
            .andThen(ParseAddressTask())
            .andThen(PingAddressTask(DEFAULT_TIMEOUT.toLong(), onPingUpdate))
            .andThenUnstoppable {
                onStageStart(StageConfiguration("Direct iperf stage", "?", deviceArgs))
                it
            }
            .andThen(
                StartIperfTask(
                    context.filesDir.absolutePath,
                    deviceArgs,
                    MultithreadedIperfOutputParser(),
                    SkipThenAverageEqualizer(
                        DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP,
                        DEFAULT_EQUALIZER_MAX_STORING
                    ),
                    DEFAULT_TIMEOUT.toLong(),
                    onStageSpeedUpdate,
                    onStageFinish,
                    onLog
                )
            )
        return chainBuilder.finishChainCreation(taskConsumer) {
            onFinish()
        }
    }

    fun stop() {
        lock.withLock {
            taskChain?.stop()
        }
    }

    class Builder(private val context: Context) {
        private var onPingUpdate: LongConsumer = LongConsumer {}
        private var onStageStart: Consumer<StageConfiguration> = Consumer {}
        private var onStageSpeedUpdate: BiConsumer<LongSummaryStatistics, Long> =
            BiConsumer { _, _ -> }
        private var onStageFinish: Consumer<LongSummaryStatistics> = Consumer {}
        private var onFinish: Runnable = Runnable {}
        private var onStop: Runnable = Runnable {}
        private var onLog: (String, String, Exception?) -> Unit = { _, _, _ -> }
        private var onFatalError: BiConsumer<String, Exception?> = BiConsumer { _, _ -> }

        fun build(): DownloadUploadSpeedTestManager {
            return DownloadUploadSpeedTestManager(
                context,
                onPingUpdate::accept,
                onStageStart::accept,
                onStageSpeedUpdate::accept,
                onStageFinish::accept,
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

        fun onStageStart(onDownloadStart: Consumer<StageConfiguration>): Builder {
            this.onStageStart = onDownloadStart
            return this
        }

        fun onStageSpeedUpdate(onDownloadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long>): Builder {
            this.onStageSpeedUpdate = onDownloadSpeedUpdate
            return this
        }

        fun onStageFinish(onDownloadFinish: Consumer<LongSummaryStatistics>): Builder {
            this.onStageFinish = onDownloadFinish
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
        private const val DEFAULT_TIMEOUT = 5_000
        private const val DEFAULT_EQUALIZER_MAX_STORING = 4
        private const val DEFAULT_EQUALIZER_DOWNLOAD_VALUES_SKIP = 0
        private const val DEFAULT_EQUALIZER_UPLOAD_VALUES_SKIP = 1
    }
}
