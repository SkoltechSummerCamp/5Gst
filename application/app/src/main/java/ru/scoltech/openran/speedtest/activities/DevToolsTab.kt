package ru.scoltech.openran.speedtest.activities

import android.app.Activity
import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.activities.DevToolsTab.Companion.TAG
import ru.scoltech.openran.speedtest.backend.IcmpPinger
import ru.scoltech.openran.speedtest.customViews.HeaderView
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
        val header = view.findViewById<HeaderView>(R.id.option_header)
        ipInfo = view.findViewById<TextView>(R.id.ipInfo)
        pingValue = view.findViewById<TextView>(R.id.pingValue)
        serverIP = view.findViewById<EditText>(R.id.serverIP)
        icmpPing = view.findViewById<Button>(R.id.icmpPingButton)
        icmpPing!!.setOnClickListener(View.OnClickListener { view: View -> startIcmpPing() })
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
                    null
                }
                .onError { e: Exception ->
                    onPingError(e)
                    null
                }.start()
            pingValue!!.text = "Err"
            icmpPing!!.setOnClickListener { view: View? -> stopIcmpPing() }
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
            //        Log.d("device ip", ip);
        }


}
