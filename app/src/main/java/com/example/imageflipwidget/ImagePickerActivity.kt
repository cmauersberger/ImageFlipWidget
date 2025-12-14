package com.example.imageflipwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ImagePickerActivity : AppCompatActivity() {
    private companion object {
        const val REQUEST_PICK_IMAGES = 1001
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
        prefs.edit().putString(ImageFlipWidget.PREF_FIRST_IMAGE_URI, pickedUris.first().toString()).apply()

        ImageFlipWidget.updateAllWidgets(this)
        finish()
    }
}
