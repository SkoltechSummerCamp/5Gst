package ru.scoltech.openran.speedtest

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

class ICMPPing {
    private val inExecuting = AtomicBoolean(false)

    @Volatile
    private var currentProcess: Process? = null

    private fun executePing(args: String, outputHandler: (String) -> Unit) {
        if (!inExecuting.get()) {
            inExecuting.set(true)
            currentProcess = ProcessBuilder("sh").redirectErrorStream(true).start()
            val os = DataOutputStream(currentProcess!!.outputStream)
            os.writeBytes("ping $args\n")
            os.flush()
            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    outputHandler(line!!)
                }
            } catch (e: IOException) {
                Log.d("", "external interruption")
            }
            inExecuting.set(false)
        }
    }

    fun performPingWithArgs(args: String, linesTaker: (String) -> Unit) {
        executePing(args) { line ->
            linesTaker(line)
            line.let { Log.d("Ping", it) }
        }
    }

    fun justPingByHost(host: String, currValueTaker: (String) -> Unit) {
        executePing(host) { line ->
            line.split(" ").forEach {
                if (it.contains("time=")) {
                    currValueTaker(it.split("=")[1])
                }
            }
        }
    }


    fun stopExecuting() {
        if (inExecuting.get()) {
            currentProcess!!.destroy()
        }
    }
}
