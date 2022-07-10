package ru.scoltech.openran.speedtest.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class ExternalStorageSaver(private val activity: Activity) {
    fun save(file: File, bitmap: Bitmap): Boolean {
        return save(file) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    fun save(file: File, writeToOutputStream: (OutputStream) -> Unit): Boolean {
        return if (isStoragePermissionGranted()) {
            file.parentFile?.mkdirs()
            try {
                FileOutputStream(file).use(writeToOutputStream)
                true
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Could not save $file", e)
                false
            }
        } else {
            false
        }
    }


    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(LOG_TAG, "Permission is granted")
                true
            } else {
                Log.v(LOG_TAG, "Permission is revoked")
                // TODO does not block the execution
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
                false
            }
        } else {
            Log.v(LOG_TAG, "Permission is granted by default")
            true
        }
    }

    companion object {
        private const val LOG_TAG = "ExternalStorageSaver"
        private const val REQUEST_PERMISSION_CODE = 1
    }
}
