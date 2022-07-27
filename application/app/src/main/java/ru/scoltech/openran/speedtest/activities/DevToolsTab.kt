package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.backend.IcmpPinger
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dev_tools_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        init(view)
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

    private fun init(view: View) {
        ipInfo = view.findViewById(R.id.ipInfo)
        pingValue = view.findViewById(R.id.pingValue)
        serverIP = view.findViewById(R.id.serverIP)
        icmpPing = view.findViewById(R.id.icmpPingButton)
        icmpPing!!.setOnClickListener { startIcmpPing() }
        icmpPinger = IcmpPinger()



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
