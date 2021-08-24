package ru.scoltech.openran.speedtest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Pair;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.LongSummaryStatistics;

public class SpeedManager {
    private int[] uploadArray;
    private int[] downloadArray;


    private static SpeedManager instance;

    private SpeedManager() {
    }

    public static SpeedManager getInstance() {
        if (instance == null) {
            instance = new SpeedManager();
        }
        return instance;
    }

    public void attachList(List<String> list) {

        downloadArray = new int[list.size() / 2];
        for (int i = 0; i < downloadArray.length; i++) {
            downloadArray[i] = Integer.parseInt(list.get(i));
        }

        uploadArray = new int[list.size() - list.size() / 2];
        for (int i = 0; i < uploadArray.length; i++) {
            uploadArray[i] = Integer.parseInt(list.get(i));
        }
    }


    private Pair<Integer, Integer> convertBitToMbps(int speed) {

        return new Pair<>(speed / 1000000, speed % 1000000);
    }


    public Pair<Integer, Integer> getSpeedWithPrecision(int speedBit, int precision) {
        Pair<Integer, Integer> speed = convertBitToMbps(speedBit);

        if (speed.second > 99) {
            String second = String.valueOf(speed.second).substring(0, precision);
            return new Pair<>(speed.first, Integer.valueOf(second));

        } else {
            return speed;
        }
    }

    public Pair<Integer, Integer> getAverageSpeed(LongSummaryStatistics statistics) {
        int speed = (int) (statistics.getAverage() / 1000);

        int int_speed = speed / 1000;
        int frac_speed = speed % 1000;

        if (frac_speed > 99)
            return new Pair<>(int_speed, frac_speed / 10);
        else
            return new Pair<>(int_speed, frac_speed);
    }

    private Pair<Integer, Integer> getAverageSpeed(int[] listBit) {

        int sum = 0;
        if (listBit.length > 0) {
            for (int sp : listBit) {
                sum += sp / 1000;
            }
            int speed = sum / listBit.length;

            int int_speed = speed / 1000;
            int frac_speed = speed % 1000;

            if (frac_speed > 99)
                return new Pair<>(int_speed, frac_speed / 10);
            else
                return new Pair<>(int_speed, frac_speed);
        }
        return null;
    }

    public Pair<Integer, Integer> getAverageUploadSpeed() {
        return getAverageSpeed(uploadArray);
    }

    public Pair<Integer, Integer> getAverageDownloadSpeed() {
        return getAverageSpeed(downloadArray);
    }

    public int[] getDownloadArray() {
        return downloadArray;
    }

    public int[] getUploadArray() {
        return uploadArray;
    }


    public Bitmap generateImage(Activity activity) {

        //TODO: rewrite with xml view only
        Bitmap bg = BitmapFactory.decodeResource(activity.getResources(), R.drawable.generated_result_background);

        Bitmap background = bg.copy(Bitmap.Config.ARGB_8888, true);
        Canvas backgroundCanvas = new Canvas(background);
        backgroundCanvas.drawBitmap(background, 0, 0, null);


        View v = activity.findViewById(R.id.result);

        Bitmap foreground = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas foregroundCanvas = new Canvas(foreground);
        v.draw(foregroundCanvas);


        Bitmap combo = Bitmap.createBitmap(background.getWidth(), background.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas comboCanvas = new Canvas(combo);
        comboCanvas.drawBitmap(background, 0f, 0f, null);
        comboCanvas.drawBitmap(foreground, 10f, 10f, null);


        Typeface futuraPtMedium = ResourcesCompat.getFont(activity, R.font.futura_pt_medium);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(activity.getColor(R.color.neutral_100));
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTypeface(futuraPtMedium);
        textPaint.setAntiAlias(true);


        String timestamp = new SimpleDateFormat("HH:mm\tdd.MM.yyyy", Locale.ROOT).format(new Date());
        comboCanvas.drawText(timestamp, background.getWidth() - 50f, background.getHeight() - 20f, textPaint);
        comboCanvas.drawText("Speedtest 5G", background.getWidth() - 50f, background.getHeight() - 50f, textPaint);

        return combo;

    }
}
