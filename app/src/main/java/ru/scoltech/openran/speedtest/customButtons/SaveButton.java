package ru.scoltech.openran.speedtest.customButtons;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.util.ExternalStorageSaver;

public class SaveButton extends androidx.appcompat.widget.AppCompatButton {
    public SaveButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("SaveButton context should be a subclass of Activity");
        }
        final Activity activity = (Activity) context;

        this.setBackground(AppCompatResources.getDrawable(context, R.drawable.ic_save));
        this.setOnClickListener(view -> {
            SpeedManager sm = SpeedManager.getInstance();
            saveTask(activity, sm.generateImage(activity));
        });
    }

    private void saveTask(Activity activity, Bitmap bitmap) {
        Log.d("SAVE_BUTTON", "saveTask: pressed save");

        if (new ExternalStorageSaver(activity).save(createFile(activity), bitmap)) {
            Toast.makeText(activity, "Successfully saved image", Toast.LENGTH_SHORT).show();
        }
    }

    private File createFile(Context context) {
        File dir = new File(String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)));

        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.ROOT).format(new Date());
        String imageName = "test_" + timeStamp + ".jpg";

        return new File(dir.getPath() + File.separator + imageName);

    }
}
