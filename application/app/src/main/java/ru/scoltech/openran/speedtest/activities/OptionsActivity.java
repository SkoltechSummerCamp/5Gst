package ru.scoltech.openran.speedtest.activities;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import kotlin.Unit;
import ru.scoltech.openran.speedtest.ApplicationConstants;
import ru.scoltech.openran.speedtest.PingCheckServer;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.backend.IcmpPinger;
import ru.scoltech.openran.speedtest.customButtons.SaveButton;
import ru.scoltech.openran.speedtest.manager.DownloadUploadSpeedTestManager;
import ru.scoltech.openran.speedtest.util.Promise;

public class OptionsActivity extends AppCompatActivity {
    private static final String TAG = OptionsActivity.class.getSimpleName();

    private TextView ipInfo;
    private EditText serverIP;
    private Button udpPing;
    private Button icmpPing;

    private PingCheckServer pcs;
    IcmpPinger icmpPinger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_options);

        init();

        refreshAddresses();
    }

    private void init() {
        ipInfo = (TextView) findViewById(R.id.ipInfo);

        serverIP = (EditText) findViewById(R.id.serverIP);

        udpPing = (Button) findViewById(R.id.udpPingButton);
        udpPing.setOnClickListener(this::startUdpPing);

        icmpPing = (Button) findViewById(R.id.icmpPingButton);
        icmpPing.setOnClickListener(this::startIcmpPing);

        pcs = new PingCheckServer(ApplicationConstants.PING_SERVER_UDP_PORT);
        pcs.start();

        icmpPinger = new IcmpPinger();
    }

    private void startPingServer(){
//        CoroutineScope(Dispatchers.Main).launch {
//            Log.d("ping server", "pcs thread is alive: ${pcs.isAlive}")
//            if (pcs.isAlive) {
//                pcs.interrupt()
//                Log.d("ping server","stop");
//            }
//            Log.d("ping server:", "pcs thread is alive: ${pcs.isAlive}")
//        }
    }

    private void startUdpPing(View view) {
        stopIcmpPing(view);
        udpPing.setText(getString(R.string.bigStop));


        udpPing.setOnClickListener(this::stopUdpPing);
    }
    private void stopUdpPing(View view) {
        udpPing.setText(getString(R.string.udpPing));

        udpPing.setOnClickListener(this::startUdpPing);
    }

    private void startIcmpPing(View view) {
        stopUdpPing(view);
        icmpPing.setText(getString(R.string.bigStop));

        icmpPinger.start(serverIP.getText().toString())
            .onSuccess(
                new Thread(){
                    runOnUiThread {
                        binding.pingValue.text = it.toString()
                    }
                }
            )
            .onError{
                Log.e(TAG, "Ping failed");
            }
            .start();

        icmpPing.setOnClickListener(this::stopIcmpPing);
    }
    private void stopIcmpPing(View view) {
        icmpPing.setText(getString(R.string.icmpPing));

        icmpPing.setOnClickListener(this::startIcmpPing);
    }


    private void refreshAddresses() {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        ipInfo.setText(ip);
        Log.d("device ip", ip);
    }
}
