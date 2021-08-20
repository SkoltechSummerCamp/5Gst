package ru.scoltech.openran.speedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.core.view.isVisible
import ru.scoltech.openran.speedtest.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.iperf.IperfRunner
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {


    lateinit var binding: ActivityMainBinding
    lateinit var iperfRunner: IperfRunner

    @Volatile
    private lateinit var pcs: PingCheckServer

    private val justICMPPingInChecking = AtomicBoolean(false)
    val pingerByICMP = ICMPPing()

    private lateinit var pingByUDPButtonDispatcher: RunForShortTimeButtonDispatcher
    private lateinit var pingServerButtonDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var justICMPPingDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var startStopButtonDispatcher: ButtonDispatcherOfTwoStates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.refreshButton.setOnClickListener {
            refreshAddresses()
        }

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
            binding.thisISserver.isEnabled = false
            binding.startStopButton.isEnabled = false
            Log.d("getReq", "preparing")
            CoroutineScope(Dispatchers.IO).launch {
                if (binding.thisISserver.isChecked) {
                    startIperf()
                }

                val value = sendGETRequest(
                    binding.serverIpField.text.toString(),
                    RequestType.START,
                    1000,
                    binding.serverArgs.text.toString()
                )
                Log.d("requestValue", value)
                runOnUiThread {
                    if (value != "error") {
                        if (!binding.thisISserver.isChecked) {
                            startIperf()
                        }
                        binding.startStopButton.isEnabled = true
                    } else {
                        binding.startStopButton.isEnabled = true
                        binding.thisISserver.isEnabled = true
                        startStopButtonDispatcher.changeState()
                    }
                    binding.iperfOutput.append("Remote server: $value\n")
                }
            }
        }

        startStopButtonDispatcher.secondAction = {
            stopIperf()
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("stop request", "sent")
                sendGETRequest(binding.serverIpField.text.toString(), RequestType.STOP, 1000)
            }
            binding.thisISserver.isEnabled = true
        }

        pingByUDPButtonDispatcher = RunForShortTimeButtonDispatcher(
            binding.pingUDPButton,
            this,
            applicationContext.getString(R.string.pingTesting)
        ) { resetAct ->
            binding.icmpPingButton.isEnabled = false
            pingUDPButtonAction {
                runOnUiThread { binding.icmpPingButton.isEnabled = true }
                resetAct()
            }
        }

        pingServerButtonDispatcher = ButtonDispatcherOfTwoStates(
            binding.pingServerButton,
            this,
            applicationContext.getString(R.string.bigStop)
        )
        pingServerButtonDispatcher.firstAction = { startPingCheckServer() }
        pingServerButtonDispatcher.secondAction = { stopPingServer() }

        binding.iperfOutput.movementMethod = ScrollingMovementMethod()

        justICMPPingDispatcher = ButtonDispatcherOfTwoStates(
            binding.icmpPingButton,
            this,
            applicationContext.getString(R.string.bigStop)
        )
        justICMPPingDispatcher.firstAction = {
            binding.pingUDPButton.isEnabled = false
            justICMPPing()
        }
        justICMPPingDispatcher.secondAction = {
            binding.pingUDPButton.isEnabled = true
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


        binding.expandButton2.setOnClickListener {
            if (binding.deviceInfoLayout.isVisible) {
                binding.deviceInfoLayout.isVisible = false
                binding.expandButton2.setImageResource(android.R.drawable.arrow_down_float)

            } else {
                binding.deviceInfoLayout.isVisible = true
                binding.expandButton2.setImageResource(android.R.drawable.arrow_up_float)

            }
        }
        binding.deviceInfoLayout.isVisible = false
    }

    private fun startPingCheckServer() {
        binding.pingServerButton.text = getString(R.string.bigStop)
        CoroutineScope(Dispatchers.IO).launch {
            pcs = PingCheckServer(ApplicationConstants.PING_SERVER_UDP_PORT)
            pcs.start()
        }
    }

    private fun stopPingServer() {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("ping server", "pcs thread is alive: ${pcs.isAlive}")
            if (pcs.isAlive) {
                pcs.interrupt()
            }
            Log.d("ping server:", "pcs thread is alive: ${pcs.isAlive}")
        }
    }

    private fun pingUDPButtonAction(afterWorkAct: () -> Unit) = runBlocking {
        val pcl = PingCheckClient()
        Log.d("pingTestButtonAction", "started")
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
        val info = NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.inetAddresses.hasMoreElements() }
            .joinToString(separator = System.lineSeparator()) { networkInterface ->
                val addresses = networkInterface.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .joinToString(separator = ", ")
                "${networkInterface.displayName}: $addresses"
            }
        binding.ipInfo.text = info
        Log.d("interfaces", info)
    }

    private fun handleIperfOutput(text: String) {
        runOnUiThread {
            binding.iperfOutput.append(text)
        }
    }

    private fun startIperf() {
        iperfRunner.start(binding.iperfArgs.text.toString())

    }

    private fun stopIperf() {
        iperfRunner.killAndWait()
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
        binding.icmpPingButton.text = getString(R.string.bigStop)
        CoroutineScope(Dispatchers.IO).launch {
            pingerByICMP.justPingByHost(
                binding.serverIP.text.toString()
            ) { value -> runOnUiThread { binding.pingValue.text = value } }
            justICMPPingInChecking.set(false)
        }
    }
}
