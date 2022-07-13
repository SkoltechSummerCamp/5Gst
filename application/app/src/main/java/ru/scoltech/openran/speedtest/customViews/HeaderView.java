package ru.scoltech.openran.speedtest.customViews;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.activities.OptionsActivity;


public class HeaderView extends LinearLayout {

    private Button returnBtn;
    private Button historyBtn;
    private Button modeBtn;

    private TextView sectionNameTV;

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.header_layout, this);

        init();

        parseAttrs(context, attrs);

        returnBtn.setOnClickListener(v -> goToStart(v.getContext()));

        historyBtn.setOnClickListener(v -> goToHistory(v.getContext()));

        modeBtn.setOnClickListener(v -> goToDev(v.getContext()));
    }

    private void init() {
        sectionNameTV = findViewById(R.id.section_name);

        returnBtn = findViewById(R.id.return_btn);
        historyBtn = findViewById(R.id.history_go_btn);
        modeBtn = findViewById(R.id.mode_switch_btn);
    }

    private void parseAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeaderView);
        int count = typedArray.getIndexCount();

        try {
            for (int i = 0; i < count; ++i) {

                int attr = typedArray.getIndex(i);

                if (attr == R.styleable.HeaderView_is_active_back) {
                } else if (attr == R.styleable.HeaderView_is_active_button_group) {
                    boolean isActiveButtonGroup = typedArray.getBoolean(attr, false);

                    changeStateButtonGroup(isActiveButtonGroup);

                } else if (attr == R.styleable.HeaderView_section_name) {
                    String sectionNameStr = typedArray.getString(attr);

                    setSectionName(sectionNameStr);
                }
            }
        } finally {
            typedArray.recycle();
        }

    }

    //TODO global: check if it efficient way to go to main menu, especially from the same activity
    private void goToStart(Context context) {
        AppCompatActivity activity = (AppCompatActivity) context;
        activity.finish();
    }

    private void goToHistory(Context context) {
        final int LOGSIZE = 20;
//        Log.d("HEADER", "goToHistory: pressed btn");
        try {
            String command = String.format("logcat -d -v threadtime *:*");
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            ArrayList<String> result = new ArrayList<>();
            String currentLine = null;

            int pid = android.os.Process.myPid();
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(String.valueOf(pid))) {
                    String line = currentLine;
                    String[] parts = line.split(" ");
                    StringBuilder data = new StringBuilder();
                    data.append(parts[1]);
                    data.append(" ");
                    for(int i = 3; i<parts.length; i++) {
                        data.append(parts[i]);
                        data.append(" ");
                    }
                    result.add(data.toString());
                }
            }

            StringBuilder logData = new StringBuilder();
            for(int i = result.size() - LOGSIZE; i < result.size(); i++){
                logData.append(result.get(i));
                logData.append("\n\n");
            }

            AlertDialog logs = new AlertDialog.Builder(context)
                    .setTitle("app logs")
                    .setMessage(logData.toString())
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            logs.show();
        } catch (IOException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToDev(Context context) {
        Log.d("HEADER", "goToDev: pressed btn");
        Intent intent = new Intent(context, OptionsActivity.class);
        context.startActivity(intent);
    }

    private void changeStateButtonGroup(boolean flag) {
        if (flag) {
            enableButtonGroup();
        } else {
            disableButtonGroup();
        }
    }

    public void hideOptionsButton() {
        modeBtn.setEnabled(false);
        modeBtn.setAlpha(0.5f);
    }

    public void disableButtonGroup() {
        historyBtn.setEnabled(false);
        historyBtn.setAlpha(0.5f);

        hideOptionsButton();
    }

    public void enableButtonGroup() {
        historyBtn.setEnabled(true);
        historyBtn.setAlpha(1f);

        modeBtn.setEnabled(true);
        modeBtn.setAlpha(1f);
    }

    public void setSectionName(String sectionName) {
        sectionNameTV.setText(sectionName);
    }

    public void showReturnBtn() {
        returnBtn.setVisibility(View.VISIBLE);
    }

}