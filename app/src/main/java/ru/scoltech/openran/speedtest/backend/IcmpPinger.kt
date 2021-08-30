package ru.scoltech.openran.speedtest.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.scoltech.openran.speedtest.util.IdleTaskKiller
import ru.scoltech.openran.speedtest.util.Promise
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class IcmpPinger {
    private var process: Process? = null
    private val lock = ReentrantLock()

    private fun ping(
        host: String,
        args: String,
        process: Process,
        onNewLine: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("ping $host $args${System.lineSeparator()}")
            os.flush()
            InputStreamReader(process.inputStream)
                .buffered()
                .useLines { lines ->
                    lines.forEach { line ->
                        onNewLine(line)
                    }
                }
        } catch (e: IOException) {
            onError(e)
        }
    }

    fun startRaw(host: String, args: String = ""): Promise<(String) -> Unit, (Exception) -> Unit> {
        return Promise { onNewLine, onError ->
            val createdProcess = lock.withLock {
                stop()
                process?.waitFor()
                try {
                    ProcessBuilder("sh")
                        .redirectErrorStream(true)
                        .start()
                        .also { process = it }
                } catch (e: IOException) {
                    onError?.invoke(e)
                    return@Promise
                } catch (e: SecurityException) {
                    onError?.invoke(e)
                    return@Promise
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                ping(host, args, createdProcess, onNewLine ?: {}, onError ?: {})
            }
        }
    }

    fun start(host: String): Promise<(Long) -> Unit, (Exception) -> Unit> {
        return Promise { onPingUpdate, onError ->
            startRaw(host)
                .onSuccess { line ->
                    pingRegex.matchEntire(line)?.let { matchResult ->
                        matchResult.groupValues[1].toLongOrNull()?.let {
                            onPingUpdate?.invoke(it)
                        }
                    }
                }
                .onError(onError ?: {})
                .start()
        }
    }

    fun pingOnce(host: String, timeoutMillis: Long): Promise<(Long) -> Unit, (Exception) -> Unit> {
        return Promise { onSuccess, onError ->
            val success = AtomicBoolean(false)
            val timedOut = AtomicBoolean(false)
            val timeoutKiller = IdleTaskKiller()

            lock.withLock {
                start(host)
                    .onSuccess {
                        onSuccess?.invoke(it)
                        success.set(true)
                        stop()
                        lock.withLock(timeoutKiller::unregisterBlocking)
                    }
                    .onError {
                        if (!success.get()) {
                            if (timedOut.get()) {
                                onError?.invoke(IOException("Timed out"))
                            } else {
                                onError?.invoke(it)
                            }
                        }
                        lock.withLock(timeoutKiller::unregisterBlocking)
                    }
                    .start()
                timeoutKiller.registerBlocking(timeoutMillis) {
                    timedOut.set(true)
                    stop()
                }
            }
        }
    }

    fun stop() {
        lock.withLock {
            process?.destroy()
        }
    }

    companion object {
        private val pingRegex = Regex(
            "^\\s*\\d+\\s+bytes\\s+from\\s+.+:\\s+icmp_seq=\\d+\\s+" +
                    "ttl=\\d+\\s+time=(\\d+)(\\.\\d+)?\\s+ms\\s*$"
        )
    }
}
