package ru.scoltech.openran.speedtest.iperf

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.jvm.Throws

/**
 * Iperf process runner.
 *
 * Creates named pipes in `writableDir` which are used to redirect iperf `stdout` and `stderr`.
 * One can handle them by setting [stdoutHandler] and [stderrHandler].
 * On finish [onFinishCallback] is called.
 *
 * Note, that [start] method kills previous iperf process with `SIGKILL` if it is still running.
 */
class IperfRunner(writableDir: String) {
    /** Set to non null value if and only if the process is running */
    private var processWaiterThread: Thread? = null
    private var processPid: Long = 0L

    private val lock: Lock = ReentrantLock()
    private val finishedCondition: Condition = lock.newCondition()

    private val stdoutPipePath = "$writableDir/iperfStdout"
    private val stderrPipePath = "$writableDir/iperfStderr"

    /** Function to which the process `stdout` will be redirected */
    var stdoutHandler: (String) -> Unit = {}

    /** Function to which the process `stderr` will be redirected */
    var stderrHandler: (String) -> Unit = {}

    /**
     * Function that will be called on finish.
     * Note that execution will be synchronized on the internal lock.
     */
    var onFinishCallback: () -> Unit = {}

    /**
     * Kills previous running process with `SIGKILL` (if any) and runs a new.
     * Iperf process is created by
     * [forking](https://man7.org/linux/man-pages/man2/fork.2.html) from current process.
     * Communication between processes is done using
     * [named pipes](https://man7.org/linux/man-pages/man3/mkfifo.3.html).
     *
     * @throws IperfException if `SIGKILL` could not be sent,
     *                        named pipe could not be created
     *                        or process could not be forked.
     * @throws InterruptedException if thread was interrupted during waiting
     *                              for previous process finish.
     */
    @Throws(IperfException::class, InterruptedException::class)
    fun start(args: String) {
        lock.lock()
        if (processWaiterThread != null) {
            sendSigKill()
            finishedCondition.await()
        }

        forceMkfifo(stdoutPipePath, "stdout")
        forceMkfifo(stderrPipePath, "stderr")

        val pidHolder = longArrayOf(0)
        runCheckingErrno("Could not fork process and launch iperf") {
            start(stdoutPipePath, stderrPipePath, splitArgs(args), pidHolder)
        }
        processPid = pidHolder[0]

        val outputHandlers = listOf(
            stderrPipePath to stderrHandler,
            stdoutPipePath to stdoutHandler,
        ).map { (pipePath, handler) ->
            CoroutineScope(Dispatchers.IO).launch { handlePipe(pipePath, handler) }
        }

        processWaiterThread = Thread({
            waitForProcessNoDestroy(processPid)
            onFinish(outputHandlers)
        }, "Iperf Waiter")
            .also { it.start() }
        lock.unlock()
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
            InputStreamReader(FileInputStream(pipePath), Charsets.UTF_8)
                .buffered().use { reader ->
                    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                    var length = 0
                    while (length >= 0) {
                        if (length > 0) {
                            handler(buffer.concatToString(endIndex = length))
                        }
                        length = reader.read(buffer)
                    }
                }
        } catch (e: IOException) {
            val message = "Could not handle iperf output"
            Log.e(LOG_TAG, message, e)
            stderrHandler("$message: ${e::class.simpleName} (${e.message})")
        }
    }

    private fun splitArgs(args: String): Array<String> {
        return args.split(SPACES_REGEX).filter { it.isNotBlank() }.toTypedArray()
    }

    private fun onFinish(outputHandlers: List<Job>) {
        lock.lock()
        waitForProcess(processPid)

        runBlocking {
            outputHandlers.forEach {
                it.join()
            }
        }

        onFinishCallback()
        processWaiterThread = null
        processPid = 0
        finishedCondition.signalAll()
        lock.unlock()
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
        lock.lock()
        if (processWaiterThread != null) {
            sendSigInt()
            finishedCondition.await()
        }
        lock.unlock()
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
        lock.lock()
        if (processWaiterThread != null) {
            runCheckingErrno(getKillErrorMessage(signalName)) { sendSignal(processPid) }
        }
        lock.unlock()
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

    companion object {
        private val SPACES_REGEX = Regex("\\s+")
        private const val LOG_TAG = "IperfRunner"

        init {
            System.loadLibrary("iperf2")
        }
    }
}
