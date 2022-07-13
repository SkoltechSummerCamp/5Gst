package ru.scoltech.openran.speedtest.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import ru.scoltech.openran.speedtest.ApplicationConstants;
import ru.scoltech.openran.speedtest.PingCheckServer;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.backend.IcmpPinger;
import ru.scoltech.openran.speedtest.backend.UdpPingCheckClient;
import ru.scoltech.openran.speedtest.customViews.HeaderView;

public class OptionsActivity extends AppCompatActivity {
    private static final String TAG = OptionsActivity.class.getSimpleName();

    private TextView ipInfo;
    private TextView pingValue;
    private EditText serverIP;
    private Button icmpPing;

    private IcmpPinger icmpPinger;

    private Thread addressUpdater;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_options);

        init();

        refreshAddresses();

        Runnable updater = () -> {
            while(true) {
                runOnUiThread(this::refreshAddresses);
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
            }
        };

        addressUpdater = new Thread(updater);
        addressUpdater.start();
    }

    private void init() {
        HeaderView header = findViewById(R.id.option_header);
        header.hideOptionsButton();

        ipInfo = findViewById(R.id.ipInfo);
        pingValue = findViewById(R.id.pingValue);

        serverIP = findViewById(R.id.serverIP);

        icmpPing = findViewById(R.id.icmpPingButton);
        icmpPing.setOnClickListener(this::startIcmpPing);

        icmpPinger = new IcmpPinger();

        // get IPERF arguments
        SharedPreferences iperfPref = getSharedPreferences(
                 getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE);
        String DOWNLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
                getString(R.string.download_device_args),
                getString(R.string.default_download_device_iperf_args)
        );
        String DOWNLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
                getString(R.string.download_server_args),
                getString(R.string.default_download_server_iperf_args)
        );
        String UPLOAD_DEVICE_IPERF_ARGS = iperfPref.getString(
                getString(R.string.upload_device_args),
                getString(R.string.default_upload_device_iperf_args)
        );
        String UPLOAD_SERVER_IPERF_ARGS = iperfPref.getString(
                getString(R.string.upload_server_args),
                getString(R.string.default_upload_server_iperf_args)
        );

        EditText iperfUploadDevText = findViewById(R.id.upload_device_args);
        iperfUploadDevText.setText(UPLOAD_DEVICE_IPERF_ARGS);
        iperfUploadDevText.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE).edit();
                editor.putString(getString(R.string.upload_device_args), s.toString());
                editor.apply();
                Log.d(TAG, "update UploadDeviceArgs = "+ s);
            }
        });

        EditText iperfUploadServText = findViewById(R.id.upload_server_args);
        iperfUploadServText.setText(UPLOAD_SERVER_IPERF_ARGS);
        iperfUploadServText.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE).edit();
                editor.putString(getString(R.string.upload_server_args), s.toString());
                editor.apply();
                Log.d(TAG, "update UploadServerArgs = "+ s);
            }
        });

        EditText iperfDownloadDevText = findViewById(R.id.download_device_args);
        iperfDownloadDevText.setText(DOWNLOAD_DEVICE_IPERF_ARGS);
        iperfDownloadDevText.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE).edit();
                editor.putString(getString(R.string.download_device_args), s.toString());
                editor.apply();
                Log.d(TAG, "update DownloadDeviceArgs = "+ s);
            }
        });

        EditText iperfDownloadServText = findViewById(R.id.download_server_args);
        iperfDownloadServText.setText(DOWNLOAD_SERVER_IPERF_ARGS);
        iperfDownloadServText.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.iperfSharedPreferences),Context.MODE_PRIVATE).edit();
                editor.putString(getString(R.string.download_server_args), s.toString());
                editor.apply();
                Log.d(TAG, "update DownloadServerArgs = "+ s);
            }
        });
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


    private void refreshAddresses() {
        String ip = "no connection";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
                        ip = inetAddress.toString().substring(1);
                }
            }
        } catch (Exception ignored) {}

        ipInfo.setText(ip);
//        Log.d("device ip", ip);
    }
}
