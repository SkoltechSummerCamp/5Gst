package ru.scoltech.openran.speedtest.customButtons

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.SpeedManager
import ru.scoltech.openran.speedtest.util.ExternalStorageSaver

/*
* A class that allows you to share an image of the speed test result.
* To create a picture it uses: result_layout.xml
*/
class ShareButton(context: Context, attrs: AttributeSet?) : AppCompatButton(context, attrs) {
    init {
        require(context is Activity) { "SaveButton context should be a subclass of Activity" }
        setOnClickListener { shareTask(context) }
    }

    private fun shareTask(activity: Activity) {
        activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { externalDir ->
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"

            val picFile = externalDir.resolve("__share_tmp.png")
            val bitmap = SpeedManager.getInstance().generateImage(activity)
            ExternalStorageSaver(activity).save(picFile, bitmap)
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.shareFileProvider",
                picFile
            )
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            activity.startActivity(Intent.createChooser(intent, "Share via"))
        } ?: run {
            val message = "Shared storage is not available"
            Log.e(LOG_TAG, message)
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val LOG_TAG = "ShareButton"
    }
}
