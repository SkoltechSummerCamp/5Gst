package ru.scoltech.openran.speedtest.customButtons;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
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

/*
* A class that allows you to save an image of the speed test result to the gallery.
* To create a picture it uses: result_layout.xml
*/
public class SaveButton extends androidx.appcompat.widget.AppCompatButton {

    private static final String TAG = SaveButton.class.getName();

    public SaveButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("SaveButton context should be a subclass of Activity");
        }
        final Activity activity = (Activity) context;

        this.setOnClickListener(view -> {
            SpeedManager sm = SpeedManager.getInstance();
            saveTask(activity, sm.generateImage(activity));
        });
    }

    private void saveTask(Activity activity, Bitmap bitmap) {
        Log.d(TAG, "saveTask: pressed save");

        String name = createName();
        String path = createPath(getContext(), name);
        String description = activity.getString(R.string.app_name);

        if (new ExternalStorageSaver(activity).save(new File(path), bitmap)) {
            Log.d(TAG, "saveTask: save image to external storage");
        }

        if (addToGallery(activity.getApplicationContext(), bitmap, name, description))
            Toast.makeText(activity, "Save image to gallery", Toast.LENGTH_SHORT).show();
    }

    private String createPath(Context context, String name) {
        File dir = new File(String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
        return dir.getPath() + File.separator + name;
    }

    private String createName() {
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.ROOT).format(new Date());
        return "Speedtest_" + timeStamp + ".jpg";
    }

    private boolean addToGallery(Context context, Bitmap bitmap, String title, String description) {
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, title, description);
        return true;
    }
}