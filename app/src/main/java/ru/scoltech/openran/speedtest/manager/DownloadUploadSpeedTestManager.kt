package ru.scoltech.openran.speedtest.manager

import android.content.Context
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.ApplicationConstants
import ru.scoltech.openran.speedtest.backend.parseInetSocketAddress
import java.lang.Runnable
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.LongConsumer
import java.util.function.ToLongFunction
import kotlin.concurrent.withLock

class DownloadUploadSpeedTestManager
private constructor(
    private val context: Context,
    private val onPingUpdate: (Long) -> Unit,
    private val onDownloadStart: () -> Unit,
    private val onDownloadSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onDownloadFinish: (LongSummaryStatistics) -> Long,
    private val onUploadStart: () -> Unit,
    private val onUploadSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onUploadFinish: (LongSummaryStatistics) -> Unit,
    private val onFinish: () -> Unit,
    private val onStop: () -> Unit,
    private val onLog: (String, String) -> Unit,
    private val onFatalError: (String) -> Unit,
) {
    private val lock = ReentrantLock()

    private var downloadManager: SpeedTestManager? = null
    private var uploadManager: SpeedTestManager? = null
    private var delayJob: Job? = null

    fun start(useBalancer: Boolean, mainAddress: String, idleBetweenTasksMelees: Long) {
        val startLock = ReentrantLock()
        val startedCondition = startLock.newCondition()

        startLock.withLock {
            CoroutineScope(Dispatchers.IO).launch {
                lock.withLock {
                    startLock.withLock(startedCondition::signal)
                    internalStart(useBalancer, mainAddress, idleBetweenTasksMelees)
                }
            }
            // guarantees that the `lock` was acquired
            // so render thread won't try to stop execution before actual start
            startedCondition.await()
        }
    }

    private fun internalStart(
        useBalancer: Boolean,
        mainAddress: String,
        idleBetweenTasksMelees: Long,
    ) {
        val address = try {
            val defaultPort = if (useBalancer) {
                ApplicationConstants.DEFAULT_BALANCER_PORT
            } else {
                ApplicationConstants.DEFAULT_IPERF_SERVER_PORT
            }
            parseInetSocketAddress(mainAddress, defaultPort)
        } catch (e: UnknownHostException) {
            onFatalError("Unknown host ($mainAddress): ${e.message}")
            return
        }

        stop()
        val builder = SpeedTestManager.Builder(
            context,
            "$DEFAULT_COMMON_CLIENT_ARGS $DEFAULT_UPLOAD_CLIENT_ARGS",
            address,
        )
            .serverArgs("-s $DEFAULT_UPLOAD_SERVER_ARGS")
            .useBalancer(useBalancer)
            .checkPing(false)
            .onPingUpdate(onPingUpdate)
            .onStart(onUploadStart)
            .onSpeedUpdate(onUploadSpeedUpdate)
            .onLog(onLog)
            .onFinish {
                onUploadFinish(it)
                onFinish()
            }
            .onStop(onStop)
            .onFatalError(onFatalError)

        val localUploadManager = builder.build()
        val localDelayJob = CoroutineScope(Dispatchers.Default)
            .launch(start = CoroutineStart.LAZY) {
                try {
                    delay(idleBetweenTasksMelees)
                } catch (e: CancellationException) {
                    onStop()
                    return@launch
                }
                localUploadManager.start()
            }
        delayJob = localDelayJob
        uploadManager = localUploadManager

        downloadManager = builder
            .clientArgs("$DEFAULT_COMMON_CLIENT_ARGS $DEFAULT_DOWNLOAD_CLIENT_ARGS")
            .serverArgs("-s $DEFAULT_DOWNLOAD_SERVER_ARGS")
            .checkPing(true)
            .onStart(onDownloadStart)
            .onSpeedUpdate(onDownloadSpeedUpdate)
            .onFinish {
                onDownloadFinish(it)
                if (useBalancer) {
                    localDelayJob.start()
                } else {
                    onFinish()
                }
            }
            .build()
            .apply { start() }
    }

    fun stop() {
        lock.withLock {
            downloadManager?.stop()
            uploadManager?.stop()
            delayJob?.cancel()
        }
    }

    class Builder(private val context: Context) {
        private var onPingUpdate: LongConsumer = LongConsumer {}
        private var onDownloadStart: Runnable = Runnable {}
        private var onDownloadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long> =
            BiConsumer { _, _ -> }
        private var onDownloadFinish: ToLongFunction<LongSummaryStatistics> = ToLongFunction { 0 }
        private var onUploadStart: Runnable = Runnable {}
        private var onUploadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long> =
            BiConsumer { _, _ -> }
        private var onUploadFinish: Consumer<LongSummaryStatistics> = Consumer {}
        private var onFinish: Runnable = Runnable {}
        private var onStop: Runnable = Runnable {}
        private var onLog: BiConsumer<String, String> = BiConsumer { _, _ -> }
        private var onFatalError: Consumer<String> = Consumer {}

        fun build(): DownloadUploadSpeedTestManager {
            return DownloadUploadSpeedTestManager(
                context,
                onPingUpdate::accept,
                onDownloadStart::run,
                onDownloadSpeedUpdate::accept,
                onDownloadFinish::applyAsLong,
                onUploadStart::run,
                onUploadSpeedUpdate::accept,
                onUploadFinish::accept,
                onFinish::run,
                onStop::run,
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

        fun onDownloadSpeedUpdate(onDownloadSpeedUpdate: BiConsumer<LongSummaryStatistics, Long>): Builder {
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

        fun onLog(onLog: BiConsumer<String, String>): Builder {
            this.onLog = onLog
            return this
        }

        fun onFatalError(onFatalError: Consumer<String>): Builder {
            this.onFatalError = onFatalError
            return this
        }
    }

    companion object {
        private const val DEFAULT_COMMON_CLIENT_ARGS = "-f b -P 10 --sum-only -i 0.1 -b 120m"
        private const val DEFAULT_DOWNLOAD_CLIENT_ARGS = "-u -R"
        private const val DEFAULT_DOWNLOAD_SERVER_ARGS = "-u"
        private const val DEFAULT_UPLOAD_CLIENT_ARGS = ""
        private const val DEFAULT_UPLOAD_SERVER_ARGS = ""
    }
}
