package ru.scoltech.openran.speedtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class Wave extends View {

    private Paint mPaint;
    private int mCurrentSpeed;
    private float mOffsetX;

    public Wave(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private Path buildWavePath(int width, int height, float frequency, float xOffset, int amplitude) {
        Path path = new Path();
        path.moveTo(width,height);
        path.lineTo(0, height);
        for (int x = 0; x <= width; x++) {
            path.lineTo(x, height/8 -  amplitude* (float) Math.sin(frequency*4.0 * Math.PI * (x + xOffset) / width));
        }
        path.close();
        return path;
    }
    private void init() {
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setARGB(30,101,101,101);
        mCurrentSpeed = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width, height;
        width = getWidth();
        height = getHeight();
        int colorValue = 63+ (mCurrentSpeed > 155 ? 155 : mCurrentSpeed);
        Path p = buildWavePath(width*2, height+height,
                1.2f, mOffsetX,height/20);
        Path p2 = buildWavePath(width*2, height+height,
                1.0f, mOffsetX+15,height/20);
        mPaint.setARGB(180,3,colorValue,197);
        canvas.drawPath(p, mPaint);
        canvas.drawPath(p2, mPaint);
        mOffsetX += 35;
    }

    public void attachSpeed(int speed) { // attach current instant speed to wave
        mCurrentSpeed = speed;
    }

    public void attachColor(int color) {
        mPaint.setColor(color);
    }

}
