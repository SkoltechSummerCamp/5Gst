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

import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.activities.DemoActivity;
import ru.scoltech.openran.speedtest.activities.DevActivity;


public class HeaderView extends LinearLayout {

    private Button returnBtn;

    private TextView sectionNameTV;

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.header_layout, this);

        init();

        parseAttrs(context, attrs);

        returnBtn.setOnClickListener(v -> goToStart(v.getContext()));
    }

    private void init() {
        sectionNameTV = findViewById(R.id.section_name);

        returnBtn = findViewById(R.id.return_btn);
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
        Intent intent = new Intent(context, DemoActivity.class);
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
        // no operations
    }

    public void enableButtonGroup() {
        // no operations
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