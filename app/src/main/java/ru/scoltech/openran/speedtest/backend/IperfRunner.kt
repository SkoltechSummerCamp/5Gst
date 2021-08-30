package ru.scoltech.openran.speedtest.backend

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.jvm.Throws

/**
 * Iperf process runner.
 *
 * Creates named pipes in `writableDir` which are used to redirect iperf `stdout` and `stderr`.
 * One can handle them by setting [stdoutLinesHandler] and [stderrLinesHandler].
 * On finish [onFinishCallback] is called.
 *
 * Note, that [start] method kills previous iperf process with `SIGKILL` if it is still running.
 *
 * @author Mihail Kazakov
 */
class IperfRunner(
    writableDir: String,

    /** Function to which the process `stdout` will be redirected line by line. */
    private val stdoutLinesHandler: (String) -> Unit = {},

    /** Function to which the process `stderr` will be redirected line by line. */
    private val stderrLinesHandler: (String) -> Unit = {},

    /**
     * Function that will be called on finish.
     * Note that execution will be synchronized on the internal lock.
     */
    private val onFinishCallback: () -> Unit = {},
) {
    /** Set to non null value if and only if the process is running */
    private var processWaiterThread: Thread? = null
    private var processPid: Long = 0L

    /** Lock used for synchronization */
    val lock: Lock = ReentrantLock()
    private val finishedCondition: Condition = lock.newCondition()

    private val stdoutPipePath = "$writableDir/iperfStdout"
    private val stderrPipePath = "$writableDir/iperfStderr"

    /**
     * Kills previous running process with `SIGKILL` (if any) and runs a new.
     * Iperf process is created by
     * [forking](https://man7.org/linux/man-pages/man2/fork.2.html) from current process.
     * Communication between processes is done using
     * [named pipes](https://man7.org/linux/man-pages/man3/mkfifo.3.html).
     *
     * It is guaranteed that no process is started on [IperfException] or [InterruptedException].
     *
     * @throws IperfException if `SIGKILL` could not be sent,
     *                        named pipe could not be created
     *                        or process could not be forked.
     * @throws InterruptedException if thread was interrupted during waiting
     *                              for previous process finish.
     */
    @Throws(IperfException::class, InterruptedException::class)
    fun start(args: String) {
        lock.withLock {
            while (processWaiterThread != null) {
                sendSigKill()
                finishedCondition.await()
            }
            Log.d(LOG_TAG, "Starting with command: iperf $args")

            forceMkfifo(stdoutPipePath, "stdout")
            forceMkfifo(stderrPipePath, "stderr")

            val pidHolder = longArrayOf(0)
            runCheckingErrno("Could not fork process and launch iperf") {
                start(stdoutPipePath, stderrPipePath, splitArgs(args), pidHolder)
            }
            processPid = pidHolder[0]

            val outputHandlers = listOf(
                stderrPipePath to stderrLinesHandler,
                stdoutPipePath to stdoutLinesHandler,
            ).map { (pipePath, handler) ->
                CoroutineScope(Dispatchers.IO).launch { handlePipe(pipePath, handler) }
            }

            processWaiterThread = thread(start = true, name = "Iperf Waiter") {
                waitForProcessNoDestroy(processPid)
                onFinish(outputHandlers)
            }
        }
    }

    private fun forceMkfifo(path: String, redirectingFile: String) {
        val previousFile = File(path)
        previousFile.delete()
        if (previousFile.exists()) {
            val type = if (previousFile.isDirectory) "directory" else "file"
            throw IperfException("Could not delete $type $path to create named pipe")
        }
        runCheckingErrno(getMkfifoErrorMessage(path, redirectingFile)) {
            mkfifo(path)
        }
    }

    private fun handlePipe(pipePath: String, handler: (String) -> Unit) {
        try {
            FileReader(pipePath)
                .buffered()
                .useLines { lines -> lines.forEach { handler(it) } }
        } catch (e: IOException) {
            val message = "Could not handle iperf output"
            Log.e(LOG_TAG, message, e)
            stderrLinesHandler("$message: ${e::class.simpleName} (${e.message})")
        }
    }

    private fun splitArgs(args: String): Array<String> {
        return args.split(SPACES_REGEX).filter { it.isNotBlank() }.toTypedArray()
    }

    private fun onFinish(outputHandlers: List<Job>) {
        lock.withLock {
            waitForProcess(processPid)

            runBlocking {
                outputHandlers.forEach {
                    it.join()
                }
            }

            onFinishCallback()
            Log.d(LOG_TAG, "Finished executing")
            processWaiterThread = null
            processPid = 0
            finishedCondition.signalAll()
        }
    }

    /**
     * Kills current running process with `SIGINT` (if any) and waits until it finishes.
     *
     * @throws IperfException if `SIGINT` could not be sent.
     * @throws InterruptedException if thread was interrupted during waiting.
     * @see sendSigInt
     * @see sendSigKill
     * @see onFinishCallback
     */
    @Throws(IperfException::class, InterruptedException::class)
    fun killAndWait() {
        lock.withLock {
            if (processWaiterThread != null) {
                sendSigInt()
                finishedCondition.await()
            }
        }
    }

    /**
     * Sends `SIGINT` to the process if it is running.
     *
     * @throws IperfException if signal could not be sent.
     */
    @Throws(IperfException::class)
    fun sendSigInt() {
        kill("SIGINT", ::sendSigInt)
    }

    /**
     * Sends `SIGKILL` to the process if it is running.
     *
     * @throws IperfException if signal could not be sent.
     */
    @Throws(IperfException::class)
    fun sendSigKill() {
        kill("SIGKILL", ::sendSigKill)
    }

    private fun kill(signalName: String, sendSignal: (Long) -> Int) {
        lock.withLock {
            if (processWaiterThread != null) {
                runCheckingErrno(getKillErrorMessage(signalName)) { sendSignal(processPid) }
            }
        }
    }

    private fun getMkfifoErrorMessage(pipePath: String, redirectingFile: String) =
        "Could not create named pipe $pipePath for redirecting $redirectingFile"

    private fun getKillErrorMessage(signalName: String) =
        "Could not send $signalName to a process with pid $processPid"

    private inline fun runCheckingErrno(errorMessage: String, block: () -> Int) {
        val errno = block()
        if (errno != 0) {
            throw IperfException(errorMessage, errno)
        }
    }

    private external fun sendSigInt(pid: Long): Int

    private external fun sendSigKill(pid: Long): Int

    private external fun mkfifo(pipePath: String): Int

    private external fun start(
        stdoutPipePath: String,
        stderrPipePath: String,
        args: Array<String>,
        pidHolder: LongArray,
    ): Int

    private external fun waitForProcessNoDestroy(pid: Long) : Int

    private external fun waitForProcess(pid: Long): Int

    class Builder(private val writableDir: String) {
        private var stdoutLinesHandler: (String) -> Unit = {}
        private var stderrLinesHandler: (String) -> Unit = {}
        private var onFinishCallback: () -> Unit = {}

        fun stdoutLinesHandler(stdoutLinesHandler: (String) -> Unit): Builder {
            this.stdoutLinesHandler = stdoutLinesHandler
            return this
        }

        fun stderrLinesHandler(stderrLinesHandler: (String) -> Unit): Builder {
            this.stderrLinesHandler = stderrLinesHandler
            return this
        }

        fun onFinishCallback(onFinishCallback: () -> Unit): Builder {
            this.onFinishCallback = onFinishCallback
            return this
        }

        fun build(): IperfRunner {
            return IperfRunner(
                writableDir,
                stdoutLinesHandler,
                stderrLinesHandler,
                onFinishCallback,
            )
        }
    }

    companion object {
        private val SPACES_REGEX = Regex("\\s+")
        private const val LOG_TAG = "IperfRunner"

        init {
            System.loadLibrary("iperf2")
        }
    }
}
