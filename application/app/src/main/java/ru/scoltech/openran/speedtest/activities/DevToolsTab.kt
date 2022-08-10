package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.backend.IcmpPinger
import ru.scoltech.openran.speedtest.backend.IperfException
import ru.scoltech.openran.speedtest.backend.IperfRunner
import java.net.Inet4Address
import java.net.NetworkInterface

class DevToolsTab : Fragment() {
    companion object {
        private val TAG = DevToolsTab::class.java.simpleName
    }

    private var ipInfo: TextView? = null
    private var pingValue: TextView? = null
    private var serverIP: EditText? = null
    private var icmpPing: Button? = null
    private var icmpPinger: IcmpPinger? = null
    private var addressUpdater: Thread? = null
    private lateinit var iperfLogsScrollView: ScrollView
    private lateinit var iperfLogsTextView: TextView
    private lateinit var iperfArgsEditText: EditText
    private lateinit var iperfStartButton: Button
    private lateinit var iperfLogsClearButton: Button
    private lateinit var iperfRunner: IperfRunner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dev_tools_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        init(view, activity)
        refreshAddresses()
        val updater = Runnable {
            while (true) {
                activity.runOnUiThread { refreshAddresses() }
                try {
                    Thread.sleep(1000)
                } catch (ignored: Exception) {
                }
            }
        }
        addressUpdater = Thread(updater)
        addressUpdater!!.start()
    }

    private fun init(view: View, activity: FragmentActivity) {
        ipInfo = view.findViewById(R.id.ipInfo)
        pingValue = view.findViewById(R.id.pingValue)
        serverIP = view.findViewById(R.id.serverIP)
        icmpPing = view.findViewById(R.id.icmpPingButton)
        icmpPing!!.setOnClickListener { startIcmpPing() }
        icmpPinger = IcmpPinger()

        configureIperf(view, activity)
    }

    private fun configureIperf(view: View, activity: FragmentActivity) {
        iperfRunner = IperfRunner.Builder(requireContext().filesDir.absolutePath)
            .stderrLinesHandler(this::appendLineToIperfLogs)
            .stdoutLinesHandler(this::appendLineToIperfLogs)
            .onFinishCallback(this::onIperfFinish)
            .build()
        iperfLogsScrollView = view.findViewById(R.id.dev_tools_iperf_scroll)
        iperfLogsTextView = view.findViewById(R.id.dev_tools_iperf_logs)
        iperfStartButton = view.findViewById(R.id.dev_tools_iperf_start_button)
        iperfStartButton.setOnClickListener { onStartIperf() }
        iperfLogsClearButton = view.findViewById(R.id.dev_tools_iperf_clear_button)
        iperfLogsClearButton.setOnClickListener { onIperfLogsClear() }

        val iperfPreferences = activity.getSharedPreferences(
            getString(R.string.iperfSharedPreferences),
            AppCompatActivity.MODE_PRIVATE,
        )
        val defaultIperfArgs = iperfPreferences.getString(getString(R.string.dev_iperf_args), "")
        iperfArgsEditText = view.findViewById(R.id.dev_tools_iperf_args)
        iperfArgsEditText.setText(defaultIperfArgs)
        iperfArgsEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.dev_iperf_args), s.toString())
                editor.apply()
                Log.d(TAG, "Update dev iperf args = $s")
            }
        })
    }

    override fun onDestroyView() {
        try {
            iperfRunner.sendSigKill()
        } catch (e: IperfException) {
            Log.e(TAG, "Could not stop iPerf", e)
        }
        super.onDestroyView()
    }

    private fun onPingError(e: Exception) {
        icmpPinger!!.stop()
        Log.d(TAG, "Ping failed$e")
        requireActivity().runOnUiThread { stopIcmpPing() }
    }

    private fun startIcmpPing() {
        icmpPing!!.text = getString(R.string.bigStop)
        icmpPinger!!.start(serverIP!!.text.toString()) // TODO если указан ip на который нельзя подключиться, то приложение зависнет
            .onSuccess { aLong: Long ->
                requireActivity().runOnUiThread { pingValue!!.text = aLong.toString() }
            }
            .onError { e: Exception ->
                onPingError(e)
            }.start()
        pingValue!!.text = "Err"
        icmpPing!!.setOnClickListener { stopIcmpPing() }
    }

    private fun stopIcmpPing() {
        icmpPing!!.text = getString(R.string.icmpPing)
        icmpPinger!!.stop()
        icmpPing!!.setOnClickListener { startIcmpPing() }
    }

    private fun onIperfLogsClear() {
        iperfLogsTextView.text = ""
    }

    private fun onStartIperf() {
        try {
            val args = iperfArgsEditText.text.toString()
            appendLineToIperfLogs(">> Starting `iperf $args`")
            iperfRunner.start(args)
            requireActivity().runOnUiThread {
                iperfStartButton.text = getString(R.string.stopIperf)
                iperfStartButton.setOnClickListener { onStopIperf() }
            }
        } catch (e: IperfException) {
            appendLineToIperfLogs(e.message ?: "Error occurred during iperf start")
        } catch (e: InterruptedException) {
            appendLineToIperfLogs(e.message ?: "Error occurred during iperf start")
        }
    }

    private fun onStopIperf() {
        try {
            appendLineToIperfLogs(">> Sending SIGINT signal to iperf")
            iperfRunner.sendSigInt()
            requireActivity().runOnUiThread {
                iperfStartButton.text = getString(R.string.forceStopIperf)
                iperfStartButton.setOnClickListener { onForceStopIperf() }
            }
        } catch (e: IperfException) {
            appendLineToIperfLogs(e.message ?: "Error occurred during iperf stop")
        }
    }

    private fun onForceStopIperf() {
        try {
            appendLineToIperfLogs(">> Sending SIGKILL signal to iperf")
            iperfRunner.sendSigKill()
        } catch (e: IperfException) {
            appendLineToIperfLogs(e.message ?: "Error occurred during iperf stop")
        }
    }

    private fun appendLineToIperfLogs(line: String) {
        val lineSeparator = System.lineSeparator()
        requireActivity().runOnUiThread {
            iperfLogsTextView.append("$line$lineSeparator")
            iperfLogsScrollView.post { iperfLogsScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun onIperfFinish() {
        activity?.let { activity ->
            activity.runOnUiThread {
                iperfStartButton.text = getString(R.string.startIperf)
                iperfStartButton.setOnClickListener { onStartIperf() }
            }
            appendLineToIperfLogs(">> Finished executing iPerf")
        }
    }

    private fun refreshAddresses() {
        var ip = "no connection"
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) ip =
                        inetAddress.toString().substring(1)
                }
            }
        } catch (ignored: Exception) {
        }
        ipInfo!!.text = ip
    }
}
