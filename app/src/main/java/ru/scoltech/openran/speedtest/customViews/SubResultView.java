package ru.scoltech.openran.speedtest.customViews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.scoltech.openran.speedtest.R;

public class SubResultView extends LinearLayout {

    private TextView downloadSpeedTV;
    private TextView uploadSpeedTV;

    public SubResultView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.subresult_layout, this);

        init();
    }

    private void init() {
        downloadSpeedTV = findViewById(R.id.value_download);
        uploadSpeedTV = findViewById(R.id.value_upload);
    }


    public void setUploadSpeed(String speed) {
        uploadSpeedTV.setText(speed);
    }

    public void setDownloadSpeed(String speed) {
        downloadSpeedTV.setText(speed);
    }


    public String getDownloadSpeed() {
        return downloadSpeedTV.getText().toString();
    }

    public String getUploadSpeed() {
        return uploadSpeedTV.getText().toString();
    }

    public void setEmpty() {
        setDownloadSpeed(getContext().getString(R.string.empty));
        setUploadSpeed(getContext().getString(R.string.empty));
    }
}
