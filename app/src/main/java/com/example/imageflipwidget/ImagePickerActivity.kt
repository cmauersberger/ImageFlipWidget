package com.example.imageflipwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

class ImagePickerActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_PICK_IMAGES = 1001

        fun start(context: Context, appWidgetId: Int) {
            val intent = Intent(context, ImagePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPicker()
    }

    private fun openPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_PICK_IMAGES)
    }

    @Deprecated("Deprecated in Android; kept for minimal dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_IMAGES) return

        if (resultCode != RESULT_OK || data == null) {
            finish()
            return
        }

        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        val pickedUris = mutableListOf<Uri>()
        val clipData = data.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let(pickedUris::add)
            }
        } else {
            data.data?.let(pickedUris::add)
        }

        if (pickedUris.isEmpty()) {
            finish()
            return
        }

        val takeFlags =
            data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        pickedUris.forEach { uri ->
            runCatching {
                if (takeFlags != 0) {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } else {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }

        val prefs = getSharedPreferences(packageName, MODE_PRIVATE)
        ImageFlipWidget.saveSelectedImages(
            prefs = prefs,
            appWidgetId = appWidgetId,
            imageUris = pickedUris.map { it.toString() }
        )

        if (appWidgetId != -1) {
            ImageFlipWidget.updateSingleWidget(this, appWidgetId)
        } else {
            ImageFlipWidget.updateAllWidgets(this)
        }
        finish()
    }
}
