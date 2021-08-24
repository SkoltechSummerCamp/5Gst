package ru.scoltech.openran.speedtest.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.SpeedTestManager;
import ru.scoltech.openran.speedtest.Wave;
import ru.scoltech.openran.speedtest.customButtons.ActionButton;
import ru.scoltech.openran.speedtest.customButtons.SaveButton;
import ru.scoltech.openran.speedtest.customButtons.ShareButton;
import ru.scoltech.openran.speedtest.customViews.CardView;
import ru.scoltech.openran.speedtest.customViews.HeaderView;
import ru.scoltech.openran.speedtest.customViews.ResultView;
import ru.scoltech.openran.speedtest.customViews.SubResultView;


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

    private SpeedManager sm;

    private Handler handler;
    private Runnable task;

    private SpeedTestManager speedTestManager;

    private final static int MEASURING_DELAY = 200;
    private final static int TASK_DELAY = 1000;

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

        // TODO split on methods
        speedTestManager = new SpeedTestManager.Builder(this)
                .onPingUpdate((ping) -> runOnUiThread(() -> mCard.setPing((int) ping)))
                .onDownloadSpeedUpdate((speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision((int) speedBitsPS, 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();
                }))
                .onDownloadFinish((statistics) -> {
                    runOnUiThread(() -> mSubResults.setDownloadSpeed(getSpeedString(sm.getAverageSpeed(statistics))));
                    return (long) TASK_DELAY;
                })
                .onUploadStart(() -> runOnUiThread(() -> cWave.attachColor(getColor(R.color.gold))))
                .onUploadSpeedUpdate((speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision((int) speedBitsPS, 2);
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
                .onStopped(() -> runOnUiThread(() -> {
                    actionBtn.setPlay();
                    mSubResults.setEmpty();
                }))
                .build();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.action_btn) {

            if (actionBtn.getContentDescription().toString().equals("start")) {

                onPlayUI();
                speedTestManager.start();

            } else if (actionBtn.getContentDescription().toString().equals("stop")) {

                onStopUI();
                speedTestManager.stop();

            } else if (actionBtn.getContentDescription().toString().equals("play")) {

                onPlayUI();
                speedTestManager.start();
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