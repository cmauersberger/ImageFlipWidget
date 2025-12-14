package com.example.imageflipwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var initializedCropMode = false
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_settings)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        findViewById<Button>(R.id.select_images_button).setOnClickListener {
            pickImagesLauncher.launch(
                Intent(this, ImagePickerActivity::class.java).putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                )
            )
        }

        findViewById<RadioGroup>(R.id.crop_mode_group).setOnCheckedChangeListener { _, checkedId ->
            if (!initializedCropMode) return@setOnCheckedChangeListener

            val cropMode = when (checkedId) {
                R.id.crop_mode_max_fit -> ImageFlipWidget.CropMode.MAX_FIT
                else -> ImageFlipWidget.CropMode.STRETCH_CROPPED
            }
            ImageFlipWidget.saveCropMode(this, appWidgetId, cropMode)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                ImageFlipWidget.updateSingleWidget(this, appWidgetId)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val count = ImageFlipWidget.getSelectedImageCount(this, appWidgetId)
        val label = resources.getQuantityString(R.plurals.selected_images, count, count)
        findViewById<TextView>(R.id.selected_images_label).text = label

        initializedCropMode = false
        val cropMode = ImageFlipWidget.getCropMode(this, appWidgetId)
        val checkedId = when (cropMode) {
            ImageFlipWidget.CropMode.MAX_FIT -> R.id.crop_mode_max_fit
            ImageFlipWidget.CropMode.STRETCH_CROPPED -> R.id.crop_mode_stretch_cropped
        }
        findViewById<RadioGroup>(R.id.crop_mode_group).check(checkedId)
        initializedCropMode = true
    }
}
