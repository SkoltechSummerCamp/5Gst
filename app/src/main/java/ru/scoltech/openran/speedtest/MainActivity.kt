package ru.scoltech.openran.speedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import ru.scoltech.openran.speedtest.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val PING_SERVER_UDP_PORT = 49121
    lateinit var binding: ActivityMainBinding
    lateinit var iperfRunner: IperfRunner

    @Volatile
    private lateinit var pcs: PingCheckServer

    private val justICMPPingInChecking = AtomicBoolean(false)
    val pingerByICMP = ICMPPing()


    private lateinit var pingTestButtonDispatcher: RunForShortTimeButtonDispatcher
    private lateinit var pingServerButtonDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var justICMPPingDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var startStopButtonDispatcher: ButtonDispatcherOfTwoStates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ArrayAdapter.createFromResource(
            this,
            R.array.commands,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter
        }
        binding.refreshButton.setOnClickListener { refreshAddresses() }
        refreshAddresses()

        iperfRunner = IperfRunner(applicationContext.filesDir.absolutePath).also {
            it.stdoutHandler = ::handleIperfOutput
            it.stderrHandler = ::handleIperfOutput
        }

        startStopButtonDispatcher = ButtonDispatcherOfTwoStates(
            binding.startStopButton, this,
            applicationContext.getString(R.string.stopIperf)
        )

        startStopButtonDispatcher.firstAction = {
            Log.d("spiner", binding.spinner.selectedItemPosition.toString())
            when (binding.spinner.selectedItemPosition) {
                0 -> startIperf()
                1 -> runIcmpPingAsCommand()
            }
            binding.spinner.isEnabled = false
        }
        startStopButtonDispatcher.secondAction = {
            when (binding.spinner.selectedItemPosition) {
                0 -> stopIperf()
                1 -> stopICMPPing()
            }
            binding.spinner.isEnabled = true
        }



        pingTestButtonDispatcher = RunForShortTimeButtonDispatcher(
            binding.pingTestButt,
            this,
            applicationContext.getString(R.string.pingTesting)
        ) { resetAct ->
            pingTestButtonAction(resetAct)
        }

        pingServerButtonDispatcher = ButtonDispatcherOfTwoStates(
            binding.pingServerButt,
            this,
            applicationContext.getString(R.string.bigStop)
        )
        pingServerButtonDispatcher.firstAction = { startPingCheckServer() }
        pingServerButtonDispatcher.secondAction = { stopPingServer() }

        binding.iperfOutput.movementMethod = ScrollingMovementMethod()

        justICMPPingDispatcher = ButtonDispatcherOfTwoStates(
            binding.justPingButt,
            this,
            applicationContext.getString(R.string.bigStop)
        )
        justICMPPingDispatcher.firstAction = {
            justICMPPing()
        }
        justICMPPingDispatcher.secondAction = {
            stopICMPPing()
        }

        binding.expandButton.setOnClickListener {
            if (binding.pingLayout.isVisible) {
                binding.pingLayout.isVisible = false
                binding.expandButton.setImageResource(android.R.drawable.arrow_down_float)

            } else {
                binding.pingLayout.isVisible = true
                binding.expandButton.setImageResource(android.R.drawable.arrow_up_float)

            }
        }
        binding.pingLayout.isVisible = false

    }

    private fun startPingCheckServer() {
        binding.pingServerButt.text = getString(R.string.bigStop)
        CoroutineScope(Dispatchers.IO).launch {
            pcs = PingCheckServer(PING_SERVER_UDP_PORT)
            pcs.start()
        }
    }

    private fun stopPingServer() {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("ping server", "pcs thread is alive: ${pcs.isAlive}")
            if (pcs.isAlive)
                pcs.interrupt()
            binding.pingServerButt.text = getString(R.string.startUdpPingServer)
            delay(500)
            Log.d("ping server:", "pcs thread is alive: ${pcs.isAlive}")
        }
    }

    private fun pingTestButtonAction(afterWorkAct: () -> Unit) = runBlocking {
        val pcl = PingCheckClient()
        Log.d("pingTestButtonAction", "started")
        binding.pingTestButt.text = getString(R.string.pingTesting)
        CoroutineScope(Dispatchers.IO).launch {
            pcl.doPingTest(
                { value: String ->
                    runOnUiThread {
                        binding.pingValue.text = value
                    }
                },
                binding.serverIP.text.toString()
            )
            afterWorkAct()
            Log.d("pingTestButtonAction", "ended")
        }
    }


    private fun refreshAddresses() {
        binding.ipInfo.text = NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.inetAddresses.hasMoreElements() }
            .joinToString(separator = System.lineSeparator()) { networkInterface ->
                val addresses = networkInterface.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .joinToString(separator = ", ")
                "${networkInterface.displayName}: $addresses"
            }
    }

    private fun handleIperfOutput(text: String) {
        runOnUiThread {
            binding.iperfOutput.append(text)
        }
    }

    private fun startIperf() {
        iperfRunner.start(binding.iperfArgs.text.toString())

        binding.iperfArgs.isEnabled = false
        binding.startStopButton.text = applicationContext.getString(R.string.stopIperf)
    }

    private fun stopIperf() {
        iperfRunner.stop()

        binding.iperfArgs.isEnabled = true
        binding.startStopButton.text = applicationContext.getString(R.string.startIperf)
    }

    private fun runIcmpPingAsCommand() = runBlocking {
        val args = binding.iperfArgs.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            pingerByICMP.performPingWithArgs(args) { line ->
                runOnUiThread {
                    binding.iperfOutput.append(line + "\n")
                }
            }
        }
    }

    private fun stopICMPPing() {
        pingerByICMP.stopExecuting()
    }


    private fun justICMPPing() = runBlocking {
        justICMPPingInChecking.set(true)
        binding.justPingButt.text = getString(R.string.bigStop)
        CoroutineScope(Dispatchers.IO).launch {
            pingerByICMP.justPingByHost(
                binding.serverIP.text.toString()
            ) { value -> runOnUiThread { binding.pingValue.text = value } }
            justICMPPingInChecking.set(false)
        }
    }
}
