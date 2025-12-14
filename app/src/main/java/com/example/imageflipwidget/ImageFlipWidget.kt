package com.example.imageflipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 * PROPOSED NAME BY CODEX: ImageFlipWidget OLD NAME: WidgetFlipWidget
 */
class ImageFlipWidget : AppWidgetProvider() {
    private companion object {
        const val ACTION_INCREASE = "com.example.imageflipwidget.action.INCREASE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        // this is where we receive an intent broadcast
        val action = intent?.action ?: return

        if (context != null && action == ACTION_INCREASE) {
            // update preferences values
            val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            prefs.edit().putString(
                "widgetText",
                ((prefs.getString("widgetText", "0") ?: "0").toInt() + 1).toString()
            ).apply()

            updateWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    // update all widgets
    private fun updateWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
        // update every widget
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    // this is where we create such an intent
    private fun pendingIntent(context: Context?, action: String): PendingIntent? {
        val intent = Intent(context, javaClass)
        intent.action = action

        // return the pending intent
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun settingsPendingIntent(context: Context): PendingIntent {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        val widgetText = prefs.getString("widgetText", "0")
        // OLD: val widgetText = context.getString(R.string.appwidget_text)
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.image_flip_widget)
        views.setTextViewText(R.id.appwidget_text, widgetText)

        // launch a pending intent to increase the value saved in shared preferences
        views.setOnClickPendingIntent(R.id.button, pendingIntent(context, ACTION_INCREASE))
        views.setOnClickPendingIntent(R.id.settings_button, settingsPendingIntent(context))

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
