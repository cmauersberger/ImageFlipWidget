package com.example.imageflipwidget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_settings)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        findViewById<Button>(R.id.select_images_button).setOnClickListener {
            ImagePickerActivity.start(this, appWidgetId)
        }
    }

    override fun onResume() {
        super.onResume()
        val count = ImageFlipWidget.getSelectedImageCount(this, appWidgetId)
        val label = resources.getQuantityString(R.plurals.selected_images, count, count)
        findViewById<TextView>(R.id.selected_images_label).text = label
    }
}

