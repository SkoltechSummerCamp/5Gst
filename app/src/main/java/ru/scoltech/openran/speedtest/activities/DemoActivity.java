package ru.scoltech.openran.speedtest.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import kotlin.collections.SetsKt;
import kotlin.text.StringsKt;
import ru.scoltech.openran.speedtest.ApplicationConstants;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.Wave;
import ru.scoltech.openran.speedtest.customButtons.ActionButton;
import ru.scoltech.openran.speedtest.customButtons.SaveButton;
import ru.scoltech.openran.speedtest.customButtons.ShareButton;
import ru.scoltech.openran.speedtest.customViews.CardView;
import ru.scoltech.openran.speedtest.customViews.HeaderView;
import ru.scoltech.openran.speedtest.customViews.ResultView;
import ru.scoltech.openran.speedtest.customViews.SubResultView;
import ru.scoltech.openran.speedtest.manager.DownloadUploadSpeedTestManager;


public class DemoActivity extends AppCompatActivity {

    private Wave cWave;
    private CardView mCard;
    private SubResultView mSubResults; // in progress result
    private HeaderView mHeader;
    private ResultView mResults; // after finishing

    //TODO global: reorganise view operating

    //action elem
    private ActionButton actionBtn;
    private TextView actionTV;
    private ShareButton shareBtn;
    private SaveButton saveBtn;
    private RelativeLayout settings;

    private SpeedManager sm;

    private Handler handler;
    private Runnable task;

    private DownloadUploadSpeedTestManager speedTestManager;

    private final static int MEASURING_DELAY = 200;
    private final static int TASK_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_demo);

        init();

        sm = SpeedManager.getInstance();
    }

    private void init() {
        mHeader = findViewById(R.id.header);

        actionBtn = findViewById(R.id.action_btn);
        actionTV = findViewById(R.id.action_text);

        mCard = findViewById(R.id.card);
        cWave = mCard.getWave();

        mSubResults = findViewById(R.id.subresult);
        mResults = findViewById(R.id.result);

        shareBtn = findViewById(R.id.share_btn);
        saveBtn = findViewById(R.id.save_btn);

        settings = findViewById(R.id.start_screen_settings);
        final EditText mainAddress = findViewById(R.id.main_address);
        mainAddress.setText(
                getPreferences(MODE_PRIVATE).getString(
                        ApplicationConstants.MAIN_ADDRESS_KEY,
                        getString(R.string.default_main_address)
                )
        );
        mainAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no operations
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no operations
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final CharSequence newMainAddress = StringsKt.isBlank(s)
                        ? getString(R.string.default_main_address) : s;
                SharedPreferences.Editor preferences = getPreferences(MODE_PRIVATE).edit();
                preferences.putString(
                        ApplicationConstants.MAIN_ADDRESS_KEY,
                        newMainAddress.toString()
                );
                preferences.apply();
            }
        });

        final RadioGroup modeRadioGroup = findViewById(R.id.mode_radio_group);
        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor preferences = getPreferences(MODE_PRIVATE).edit();
            preferences.putBoolean(
                    ApplicationConstants.USE_BALANCER_KEY,
                    checkedId == R.id.balancer_mode
            );
            preferences.apply();
        });

        final boolean useBalancer = getPreferences(MODE_PRIVATE)
                .getBoolean(ApplicationConstants.USE_BALANCER_KEY, true);
        if (useBalancer) {
            this.<RadioButton>findViewById(R.id.balancer_mode).setChecked(true);
        } else {
            this.<RadioButton>findViewById(R.id.direct_mode).setChecked(true);
        }

        // TODO split on methods
        speedTestManager = new DownloadUploadSpeedTestManager.Builder(this)
                .onPingUpdate((ping) -> runOnUiThread(() -> mCard.setPing((int) ping)))
                .onDownloadSpeedUpdate((statistics, speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(speedBitsPS.intValue(), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();
                }))
                .onDownloadFinish((statistics) -> {
                    runOnUiThread(() -> mSubResults.setDownloadSpeed(getSpeedString(sm.getAverageSpeed(statistics))));
                    return TASK_DELAY;
                })
                .onUploadStart(() -> runOnUiThread(() -> cWave.attachColor(getColor(R.color.gold))))
                .onUploadSpeedUpdate((statistics, speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(speedBitsPS.intValue(), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();
                }))
                .onUploadFinish((statistics) -> runOnUiThread(() -> mSubResults.setUploadSpeed(getSpeedString(sm.getAverageSpeed(statistics)))))
                .onFinish(() -> runOnUiThread(() -> {
                    actionBtn.setPlay();

                    String downloadSpeed = mSubResults.getDownloadSpeed();
                    String uploadSpeed = mSubResults.getUploadSpeed();
                    String ping = mCard.getPing();
                    onResultUI(downloadSpeed, uploadSpeed, ping);
                }))
                .onStop(() -> runOnUiThread(() -> {
                    onStopUI();
                    actionBtn.setPlay();
                    mSubResults.setEmpty();
                }))
                .onFatalError((s) -> runOnUiThread(() -> {
                    // TODO bad tag
                    Log.e("FATAL", s);

                    onStopUI();
                    actionBtn.setPlay();
                    mSubResults.setEmpty();
                }))
                .onLog(Log::v)
                .build();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.action_btn) {

            if (SetsKt.setOf("start", "play").contains(actionBtn.getContentDescription().toString())) {

                onPlayUI();
                speedTestManager.start(
                        getPreferences(MODE_PRIVATE).getBoolean(
                                ApplicationConstants.USE_BALANCER_KEY,
                                true
                        ),
                        getPreferences(MODE_PRIVATE).getString(
                                ApplicationConstants.MAIN_ADDRESS_KEY,
                                getString(R.string.default_main_address)
                        ),
                        TASK_DELAY
                );

            } else if (actionBtn.getContentDescription().toString().equals("stop")) {

                onStopUI();
                speedTestManager.stop();

            }
        }
    }


    private List<String> readSpeedFromAssetsCSV(String filename) {
        List<String> records = new ArrayList<String>();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(getAssets().open(filename)));) {
            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                records.add(values[8]);
            }

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return records;
    }

    private void measureDownloadSpeed() {

        handler = new Handler();
        task = new Runnable() {
            int i = 0;

            @Override
            public void run() {
                Log.d("MEASURE", "Doing task download" + i);

                if (i < sm.getDownloadArray().length) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.getDownloadArray()[i], 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    i++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                    // if finish counting
                } else {
                    handler.removeCallbacks(this);

                    mSubResults.setDownloadSpeed(getSpeedString(sm.getAverageDownloadSpeed()));

                    // delay between two tasks: download and upload
                    handler = new Handler();
                    handler.postDelayed(() -> {

                        cWave.attachColor(getColor(R.color.gold));
                        measureUploadSpeed();

                    }, TASK_DELAY);
                }
            }
        };
        handler.post(task);
    }

    private void measureUploadSpeed() {


        handler = new Handler();
        task = new Runnable() {
            int i = 0;

            @Override
            public void run() {
                Log.d("MEASURE", "Doing task upload" + i);

                if (i < sm.getUploadArray().length) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.getUploadArray()[i], 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    i++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                } else {
                    handler.removeCallbacks(this);

                    mSubResults.setUploadSpeed(getSpeedString(sm.getAverageUploadSpeed()));
                    actionBtn.setPlay();

                    String downloadSpeed = mSubResults.getDownloadSpeed();
                    String uploadSpeed = mSubResults.getUploadSpeed();
                    String ping = mCard.getPing();
                    onResultUI(downloadSpeed, uploadSpeed, ping);
                }
            }
        };
        handler.post(task);
    }

    private void stopMeasuring() {
        Log.d("MEASURE", "stopSpeed: mock stopping");

        actionBtn.setPlay();
        mSubResults.setEmpty();

        handler.removeCallbacks(task);
    }

    private String getSpeedString(Pair<Integer, Integer> speed) {
        return String.format("%d.%d", speed.first, speed.second);
    }

    private void onResultUI(String downloadSpeed, String uploadSpeed, String ping) {

        mSubResults.setVisibility(View.GONE);

        mResults.setVisibility(View.VISIBLE);

        mCard.setEmptyCaptions();
        mCard.setMessage("Done");

        mResults.setDownloadSpeed(downloadSpeed);
        mResults.setUploadSpeed(uploadSpeed);
        mResults.setPing(ping);

        actionBtn.setRestart();

        mHeader.showReturnBtn();

        shareBtn.setVisibility(View.VISIBLE);
        saveBtn.setVisibility(View.VISIBLE);

    }

    public void onPlayUI() {
        settings.setVisibility(View.GONE);

        mCard.setVisibility(View.VISIBLE);
        mCard.setDefaultCaptions();

        cWave.attachColor(getColor(R.color.mint));

        mSubResults.setVisibility(View.VISIBLE);
        mSubResults.setEmpty();
        mResults.setVisibility(View.GONE);

        mHeader.setSectionName("Demonstration");
        mHeader.disableButtonGroup();
        mHeader.hideReturnBtn();

        actionTV.setVisibility(View.GONE);
        actionBtn.setStop();

        shareBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
    }

    public void onStopUI() {
        mHeader.enableButtonGroup();
        mHeader.showReturnBtn();

        actionBtn.setPlay();

        mSubResults.setEmpty();
    }

}