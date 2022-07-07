package ru.scoltech.openran.speedtest.activities

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.*
import ru.scoltech.openran.speedtest.backend.*
import ru.scoltech.openran.speedtest.databinding.ActivityOptionsBinding
import java.net.*

class OptionsActivity : AppCompatActivity() {


    lateinit var binding: ActivityOptionsBinding
    lateinit var iperfRunner: IperfRunner

    @Volatile
    private lateinit var pcs: PingCheckServer

    val icmpPinger = IcmpPinger()

    private lateinit var pingByUDPButtonDispatcher: RunForShortTimeButtonDispatcher
    private lateinit var pingServerButtonDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var icmpPingDispatcher: ButtonDispatcherOfTwoStates
    private lateinit var startStopButtonDispatcher: ButtonDispatcherOfTwoStates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.refreshButton.setOnClickListener {
            refreshAddresses()
        }

        refreshAddresses()

        iperfRunner = IperfRunner.Builder(applicationContext.filesDir.absolutePath)
            .stdoutLinesHandler(this::handleIperfLines)
            .stderrLinesHandler(this::handleIperfLines)
            .build()

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

        icmpPingDispatcher = ButtonDispatcherOfTwoStates(
            binding.icmpPingButton,
            this,
            applicationContext.getString(R.string.bigStop)
        )
        icmpPingDispatcher.firstAction = {
            binding.pingUDPButton.isEnabled = false
            startIcmpPing()
        }
        icmpPingDispatcher.secondAction = {
            binding.pingUDPButton.isEnabled = true
            stopIcmpPing()
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

        binding.optionHeader.disableButtonGroup();

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
        val pcl = UdpPingCheckClient()
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

    private fun handleIperfLines(text: String) {
        runOnUiThread {
            binding.iperfOutput.append(text)
            binding.iperfOutput.append(System.lineSeparator())
        }
    }

    private fun startIperf() {
        iperfRunner.start(binding.iperfArgs.text.toString())

    }

    private fun stopIperf() {
        iperfRunner.killAndWait()
    }

    private fun startRawIcmpPing() {
        val args = binding.iperfArgs.text.toString()
        // TODO split address and args?
        icmpPinger.startRaw(args)
            .onSuccess { line ->
                runOnUiThread {
                    binding.iperfOutput.append(line + "\n")
                }
            }
            .onError {
                // TODO show error to the user
                Log.e(LOG_TAG, "Ping failed", it)
            }
            .start()
    }

    private fun stopIcmpPing() {
        icmpPinger.stop()
    }

    private fun startIcmpPing() {
        binding.icmpPingButton.text = getString(R.string.bigStop)
        icmpPinger.start(binding.serverIP.text.toString())
            .onSuccess {
                runOnUiThread {
                    binding.pingValue.text = it.toString()
                }
            }
            .onError {
                // TODO show error to the user
                Log.e(LOG_TAG, "Ping failed", it)
            }
            .start()
    }

    companion object {
        private const val LOG_TAG = "OptionsActivity"
    }
}
