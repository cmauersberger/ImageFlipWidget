package org.example.imageflipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class FlipperWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_FLIP = "org.example.imageflipwidget.FLIP_IMAGE"
        private const val PREFS_NAME = "org.example.imageflipwidget.PREFERENCES"
        private const val STATE_PREFIX = "widget_state_"

        private fun loadState(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(STATE_PREFIX + appWidgetId, 0)
        }

        private fun saveState(context: Context, appWidgetId: Int, state: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(STATE_PREFIX + appWidgetId, state).apply()
        }
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
        if (intent.action == ACTION_FLIP) {
            val appWidgetId =
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val newState = 1 - loadState(context, appWidgetId)
                saveState(context, appWidgetId, newState)
                val manager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, manager, appWidgetId)
            } else {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, FlipperWidgetProvider::class.java))
                onUpdate(context, manager, ids)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_flipper)
        val state = loadState(context, appWidgetId)
        val imageRes = if (state == 0) R.drawable.image1 else R.drawable.image2
        views.setImageViewResource(R.id.image_view, imageRes)

        val intent = Intent(context, FlipperWidgetProvider::class.java).apply {
            action = ACTION_FLIP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.image_view, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
