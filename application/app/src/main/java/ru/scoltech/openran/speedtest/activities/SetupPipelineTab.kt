package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.adapter.ParentFragmentPagerAdapter
import ru.scoltech.openran.speedtest.backend.IcmpPinger
import ru.scoltech.openran.speedtest.customViews.HeaderView
import java.lang.Exception
import java.net.Inet4Address
import java.net.NetworkInterface

class SetupPipelineTab : Fragment() {
    companion object {
        private val TAG = SetupPipelineTab::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_pipeline, container, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        // get IPERF arguments
        val iperfPref = activity.getSharedPreferences(
            getString(R.string.iperfSharedPreferences), AppCompatActivity.MODE_PRIVATE
        )
        val DOWNLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            getString(R.string.download_device_args),
            getString(R.string.default_download_device_iperf_args)
        )
        val DOWNLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            getString(R.string.download_server_args),
            getString(R.string.default_download_server_iperf_args)
        )
        val UPLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
            getString(R.string.upload_device_args),
            getString(R.string.default_upload_device_iperf_args)
        )
        val UPLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
            getString(R.string.upload_server_args),
            getString(R.string.default_upload_server_iperf_args)
        )
        val iperfUploadDevText = view.findViewById<EditText>(R.id.upload_device_args)
        iperfUploadDevText.setText(UPLOAD_DEVICE_IPERF_ARGS)
        iperfUploadDevText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.upload_device_args), s.toString())
                editor.apply()
                Log.d(TAG, "update UploadDeviceArgs = $s")
            }
        })
        val iperfUploadServText = view.findViewById<EditText>(R.id.upload_server_args)
        iperfUploadServText.setText(UPLOAD_SERVER_IPERF_ARGS)
        iperfUploadServText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.upload_server_args), s.toString())
                editor.apply()
                Log.d(TAG, "update UploadServerArgs = $s")
            }
        })

        val iperfDownloadDevText = view.findViewById<EditText>(R.id.download_device_args)
        iperfDownloadDevText.setText(DOWNLOAD_DEVICE_IPERF_ARGS)
        iperfDownloadDevText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                Log.e(TAG, "update now DownloadDeviceArgs = $s")
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.download_device_args), s.toString())
                editor.apply()
                Log.d(TAG, "update DownloadDeviceArgs = $s")
            }
        })
        val iperfDownloadServText = view.findViewById<EditText>(R.id.download_server_args)
        iperfDownloadServText.setText(DOWNLOAD_SERVER_IPERF_ARGS)
        iperfDownloadServText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val editor = activity.getSharedPreferences(
                    getString(R.string.iperfSharedPreferences),
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString(getString(R.string.download_server_args), s.toString())
                editor.apply()
                Log.d(TAG, "update DownloadServerArgs = $s")
            }
        })
    }
}
