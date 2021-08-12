package ru.scoltech.openran.speedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import ru.scoltech.openran.speedtest.databinding.ActivityMainBinding
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var iperfRunner: IperfRunner

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
        binding.startStopButton.setOnClickListener { startIperf() }
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
        binding.startStopButton.setOnClickListener { stopIperf() }
    }

    private fun stopIperf() {
        iperfRunner.stop()

        binding.iperfArgs.isEnabled = true
        binding.startStopButton.text = applicationContext.getString(R.string.startIperf)
        binding.startStopButton.setOnClickListener { startIperf() }
    }
}
