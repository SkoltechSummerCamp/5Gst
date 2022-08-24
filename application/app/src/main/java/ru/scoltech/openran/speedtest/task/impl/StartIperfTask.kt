package ru.scoltech.openran.speedtest.task.impl

import ru.scoltech.openran.speedtest.backend.IperfException
import ru.scoltech.openran.speedtest.backend.IperfRunner
import ru.scoltech.openran.speedtest.parser.IperfOutputParser
import ru.scoltech.openran.speedtest.task.FatalException
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.task.impl.model.ServerAddress
import ru.scoltech.openran.speedtest.util.Equalizer
import ru.scoltech.openran.speedtest.util.IdleTaskKiller
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class StartIperfTask(
    private val writableDir: String,
    private val args: String,
    private val speedParser: IperfOutputParser,
    private val speedEqualizer: Equalizer<*>,
    private val idleTimeoutMillis: Long,
    private val onSpeedUpdate: (LongSummaryStatistics, Long) -> Unit,
    private val onFinish: (LongSummaryStatistics) -> Unit,
    private val onLog: (String, String, Exception?) -> Unit,
) : Task<ServerAddress, ServerAddress> {
    override fun prepare(
        argument: ServerAddress,
        killer: TaskKiller
    ): Promise<(ServerAddress) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, _ ->
        val idleTaskKiller = IdleTaskKiller()
        val processor = IperfOutputProcessor(idleTaskKiller, speedEqualizer.copy()) {
            onSuccess?.invoke(argument)
        }

        val iperfRunner = IperfRunner.Builder(writableDir)
            .stdoutLinesHandler(processor::onIperfStdoutLine)
            .stderrLinesHandler(processor::onIperfStderrLine)
            .onFinishCallback(processor::onIperfFinish)
            .build()

        while (true) {
            try {
                // TODO validate not to have -c and -p in command
                iperfRunner.start(
                    "-c ${argument.host} -p ${argument.port} $args"
                )

                val task = {
                    try {
                        iperfRunner.sendSigKill()
                    } catch (e: IperfException) {
                        onLog(LOG_TAG, "Could not stop iPerf", e)
                    }
                }
                idleTaskKiller.registerBlocking(idleTimeoutMillis, task)
                killer.register(task)
                break
            } catch (e: InterruptedException) {
                onLog(LOG_TAG, "Interrupted iPerf start. Ignoring...", e)
            } catch (e: IperfException) {
                throw FatalException("Could not start iPerf", e)
            }
        }
    }

    private inner class IperfOutputProcessor(
        private val idleTaskKiller: IdleTaskKiller,
        private val speedEqualizer: Equalizer<*>,
        private val onFinish: () -> Unit,
    ) {
        private val lock = ReentrantLock()
        private val speedStatistics = LongSummaryStatistics()

        fun onIperfStdoutLine(line: String) {
            onLog("iPerf stdout", line, null)
            idleTaskKiller.updateTaskState()
            val speed = try {
                speedParser.parseSpeed(line)
            } catch (e: IOException) {
                onLog("Speed parser", "Invalid stdout format", e)
                return
            }

            lock.withLock {
                if (speedEqualizer.accept(speed)) {
                    speedStatistics.accept(speed)
                    val equalizedSpeed = try {
                        speedEqualizer.getEqualized()
                    } catch (e: Equalizer.NoValueException) {
                        return
                    }
                    onSpeedUpdate(speedStatistics, equalizedSpeed.toLong())
                }
            }
        }

        fun onIperfStderrLine(line: String) {
            onLog("iPerf stderr", line, null)
            idleTaskKiller.updateTaskState()
        }

        fun onIperfFinish() {
            lock.withLock {
                onFinish(speedStatistics)
            }
            onFinish()
        }
    }

    companion object {
        const val LOG_TAG = "StartIperfTask"
    }
}
