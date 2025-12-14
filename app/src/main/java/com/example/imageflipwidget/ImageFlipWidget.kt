package com.example.imageflipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import kotlin.math.roundToInt

/**
 * Implementation of App Widget functionality.
 * PROPOSED NAME BY CODEX: ImageFlipWidget OLD NAME: WidgetFlipWidget
 */
class ImageFlipWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_INCREASE = "com.example.imageflipwidget.action.INCREASE"

        private const val PREF_WIDGET_TEXT = "widgetText"
        const val PREF_FIRST_IMAGE_URI = "widgetFirstImageUri"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ImageFlipWidget::class.java))
            ids.forEach { id -> updateAppWidget(context, manager, id) }
        }

        private fun increasePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ImageFlipWidget::class.java).apply { action = ACTION_INCREASE }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun imagePickerPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ImagePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

            val widgetText = prefs.getString(PREF_WIDGET_TEXT, "0")
            val imageUriString = prefs.getString(PREF_FIRST_IMAGE_URI, null)

            val views = RemoteViews(context.packageName, R.layout.image_flip_widget)
            views.setTextViewText(R.id.appwidget_text, widgetText)

            if (!imageUriString.isNullOrBlank()) {
                val bitmap = runCatching {
                    loadScaledBitmapForWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        uri = Uri.parse(imageUriString)
                    )
                }.getOrNull()

                if (bitmap != null) {
                    views.setViewVisibility(R.id.background_image, android.view.View.VISIBLE)
                    views.setImageViewBitmap(R.id.background_image, bitmap)
                } else {
                    views.setViewVisibility(R.id.background_image, android.view.View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.background_image, android.view.View.GONE)
            }

            views.setOnClickPendingIntent(R.id.button, increasePendingIntent(context))
            views.setOnClickPendingIntent(R.id.settings_button, imagePickerPendingIntent(context))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun loadScaledBitmapForWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            uri: Uri
        ): Bitmap? {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val density = context.resources.displayMetrics.density

            val minWidthPx =
                (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat() * density)
                    .roundToInt()
                    .coerceAtLeast(1)
            val minHeightPx =
                (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat() * density)
                    .roundToInt()
                    .coerceAtLeast(1)

            val maxSidePx = maxOf(minWidthPx, minHeightPx)
            val capPx = maxSidePx.coerceIn(128, 512)

            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > capPx * 2 || bounds.outHeight / sampleSize > capPx * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val decoded =
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
                    ?: return null

            val decodedMaxSide = maxOf(decoded.width, decoded.height)
            if (decodedMaxSide <= capPx) return decoded

            val scale = capPx.toFloat() / decodedMaxSide.toFloat()
            val scaledWidth = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val scaledHeight = (decoded.height * scale).roundToInt().coerceAtLeast(1)

            val scaled = Bitmap.createScaledBitmap(decoded, scaledWidth, scaledHeight, true)
            if (scaled != decoded) decoded.recycle()
            return scaled
        }
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
                PREF_WIDGET_TEXT,
                ((prefs.getString(PREF_WIDGET_TEXT, "0") ?: "0").toInt() + 1).toString()
            ).apply()

            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }


}
