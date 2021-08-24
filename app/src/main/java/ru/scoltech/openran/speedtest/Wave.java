package ru.scoltech.openran.speedtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class Wave extends View {

    private Paint mPaint;
    private RectF mRect;

    private int currentSpeed;


    public Wave(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mRect = new RectF();

        mPaint = new Paint();
        mPaint.setStrokeWidth(10);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        currentSpeed = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {

        float width, height, radius;
        width = getWidth();
        height = getHeight();

        if (width > height)
            radius = height / 2.5f;
        else
            radius = width / 2.5f;


        float center_x, center_y;
        center_x = Math.round(width / 2);
        center_y = height / 2;

        mRect.set(center_x - radius - currentSpeed * 2,
                center_y - radius,
                center_x + radius + currentSpeed * 2,
                center_y + radius);

        canvas.drawRoundRect(mRect, 70, 50, mPaint);

    }

    public void attachSpeed(int speed) { // attach current instant speed to wave
        currentSpeed = speed;
    }

    public void attachColor(int color) {
        mPaint.setColor(color);
    }

}
