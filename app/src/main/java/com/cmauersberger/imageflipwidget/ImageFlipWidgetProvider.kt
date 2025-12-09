package com.cmauersberger.imageflipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class ImageFlipWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_FLIP_IMAGE = "com.cmauersberger.imageflipwidget.FLIP_IMAGE"
        private const val PREFS_NAME = "ImageFlipWidgetPrefs"
        private const val PREF_IMAGE_STATE = "image_state_"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_FLIP_IMAGE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Toggle the image state
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentState = prefs.getBoolean(PREF_IMAGE_STATE + appWidgetId, false)
                prefs.edit().putBoolean(PREF_IMAGE_STATE + appWidgetId, !currentState).apply()
                
                // Update the widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Clean up preferences when widget is deleted
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(PREF_IMAGE_STATE + appWidgetId)
        }
        editor.apply()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imageState = prefs.getBoolean(PREF_IMAGE_STATE + appWidgetId, false)
        
        // Choose the image based on the current state
        val imageResource = if (imageState) R.drawable.image_two else R.drawable.image_one
        
        // Create RemoteViews and set the image
        val views = RemoteViews(context.packageName, R.layout.image_flip_widget)
        views.setImageViewResource(R.id.widget_image, imageResource)
        
        // Create an intent to flip the image when tapped
        val intent = Intent(context, ImageFlipWidgetProvider::class.java).apply {
            action = ACTION_FLIP_IMAGE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            flags
        )
        
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
