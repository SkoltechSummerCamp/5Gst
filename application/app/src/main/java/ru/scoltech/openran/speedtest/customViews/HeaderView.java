package ru.scoltech.openran.speedtest.customViews;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
                    boolean isActiveReturnBtn = typedArray.getBoolean(attr, false);

                    changeVisibilityReturnBtn(isActiveReturnBtn);

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
        Log.d("HEADER", "goToHistory: pressed btn");
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

    private void changeVisibilityReturnBtn(boolean flag) {
        if (flag) {
            showReturnBtn();
        } else {
            hideReturnBtn();
        }
    }

    public void disableButtonGroup() {
        historyBtn.setEnabled(false);
        historyBtn.setAlpha(0.5f);

        modeBtn.setEnabled(false);
        modeBtn.setAlpha(0.5f);
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

    public void hideReturnBtn() {
        returnBtn.setVisibility(View.GONE);
    }

    public void showReturnBtn() {
        returnBtn.setVisibility(View.VISIBLE);
    }

}