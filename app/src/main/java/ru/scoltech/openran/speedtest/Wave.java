package ru.scoltech.openran.speedtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class Wave extends View {
    private static final int mAlphaChanelValue = 180;
    private static final int mRedChanelValue = 3;
    private static final int mGreenChanelValue = 218;
    private static final int mMinimumGreenChanelValue = 63;
    private static final int mBlueChanelValue = 197;
    private static final int mWaveAplitudeDevider = 20;
    private static final float mFirstWaveFrequency = 1.2f;
    private static final float mSecondWaveFrequency = 1.0f;
    private static final int mSecondWaveOffset = 15;
    private static final int mOffsetAddendum = 15;
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
            path.lineTo(x, (float) height / 8 - amplitude * 
                    (float) Math.sin(frequency * 4.0 * Math.PI * (x + xOffset) / width));
        }
        path.close();
        return path;
    }
    private void init() {
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setARGB(mAlphaChanelValue, mRedChanelValue, mGreenChanelValue, mBlueChanelValue);
        mCurrentSpeed = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int colorValue = Math.min(mGreenChanelValue,
                mMinimumGreenChanelValue + mCurrentSpeed);
        Path p = buildWavePath(width * 2, height * 2,
                mFirstWaveFrequency, mOffsetX, height / mWaveAplitudeDevider);
        Path p2 = buildWavePath(width * 2, height * 2,
                mSecondWaveFrequency, mOffsetX + mSecondWaveOffset, height / mWaveAplitudeDevider);
        mPaint.setARGB(mAlphaChanelValue, mRedChanelValue, colorValue, mBlueChanelValue);
        canvas.drawPath(p, mPaint);
        canvas.drawPath(p2, mPaint);
        mOffsetX += mOffsetAddendum;
    }

    public void attachSpeed(int speed) { // attach current instant speed to wave
        mCurrentSpeed = speed;
    }

    public void attachColor(int color) {
        mPaint.setColor(color);
    }

}
