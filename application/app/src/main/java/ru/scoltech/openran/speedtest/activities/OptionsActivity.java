package ru.scoltech.openran.speedtest.activities;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import ru.scoltech.openran.speedtest.ApplicationConstants;
import ru.scoltech.openran.speedtest.PingCheckServer;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.backend.IcmpPinger;
import ru.scoltech.openran.speedtest.backend.UdpPingCheckClient;

public class OptionsActivity extends AppCompatActivity {
    private static final String TAG = OptionsActivity.class.getSimpleName();

    private TextView ipInfo;
    private TextView pingValue;
    private EditText serverIP;
    private Button udpPing;
    private Button icmpPing;

    IcmpPinger icmpPinger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_options);

        init();

        refreshAddresses();
    }

    private void init() {
        ipInfo = findViewById(R.id.ipInfo);
        pingValue = findViewById(R.id.pingValue);

        serverIP = findViewById(R.id.serverIP);

        icmpPing = findViewById(R.id.icmpPingButton);
        icmpPing.setOnClickListener(this::startIcmpPing);

        icmpPinger = new IcmpPinger();
    }

    private void onPingError(Exception e){
        icmpPinger.stop();
        Log.d(TAG, "Ping failed" + e.toString());
        runOnUiThread(() -> {
            stopIcmpPing(icmpPing);
        });
    }

    private void startIcmpPing(View view) {
        icmpPing.setText(getString(R.string.bigStop));

        icmpPinger.start(serverIP.getText().toString()) // TODO если указан ip на который нельзя подключиться, то приложение зависнет
                .onSuccess( aLong -> {
                    runOnUiThread(() -> pingValue.setText(String.valueOf(aLong)));
                    return null;
                })
                .onError(e -> {
                    onPingError(e);
                    return null;
                }).start();

        pingValue.setText("Err");
        icmpPing.setOnClickListener(this::stopIcmpPing);
    }
    private void stopIcmpPing(View view) {
        icmpPing.setText(getString(R.string.icmpPing));
        icmpPinger.stop();
        icmpPing.setOnClickListener(this::startIcmpPing);
    }


    private void refreshAddresses() { // TODO сделать обновление в паралельном потоке
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        ipInfo.setText(ip);
        Log.d("device ip", ip);
    }
}
