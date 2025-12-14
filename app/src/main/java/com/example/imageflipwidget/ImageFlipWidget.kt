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
import android.view.View
import android.widget.RemoteViews
import kotlin.math.roundToInt
import org.json.JSONArray

/**
 * Implementation of App Widget functionality.
 * PROPOSED NAME BY CODEX: ImageFlipWidget OLD NAME: WidgetFlipWidget
 */
class ImageFlipWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_NEXT_IMAGE = "com.example.imageflipwidget.action.NEXT_IMAGE"

        private const val PREF_FIRST_IMAGE_URI_LEGACY = "widgetFirstImageUri"

        private const val PREF_IMAGE_URIS_PREFIX = "widgetImageUris_"
        private const val PREF_IMAGE_INDEX_PREFIX = "widgetImageIndex_"

        private fun imageUrisKey(appWidgetId: Int) = "$PREF_IMAGE_URIS_PREFIX$appWidgetId"
        private fun imageIndexKey(appWidgetId: Int) = "$PREF_IMAGE_INDEX_PREFIX$appWidgetId"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ImageFlipWidget::class.java))
            ids.forEach { id -> updateAppWidget(context, manager, id) }
        }

        fun updateSingleWidget(context: Context, appWidgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            updateAppWidget(context, manager, appWidgetId)
        }

        private fun imagePickerPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, ImagePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getActivity(
                context,
                100_000 + appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun nextImagePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, ImageFlipWidget::class.java).apply {
                action = ACTION_NEXT_IMAGE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                200_000 + appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun saveSelectedImages(
            prefs: android.content.SharedPreferences,
            appWidgetId: Int,
            imageUris: List<String>
        ) {
            val normalizedUris = imageUris.filter { it.isNotBlank() }.distinct()
            if (normalizedUris.isEmpty()) return

            val editor = prefs.edit()
            if (appWidgetId != -1) {
                editor.putString(imageUrisKey(appWidgetId), JSONArray(normalizedUris).toString())
                editor.putInt(imageIndexKey(appWidgetId), 0)
            } else {
                editor.putString(PREF_FIRST_IMAGE_URI_LEGACY, normalizedUris.first())
            }
            editor.apply()
        }

        private fun loadImageUris(
            prefs: android.content.SharedPreferences,
            appWidgetId: Int
        ): List<String> {
            if (appWidgetId != -1) {
                val stored = prefs.getString(imageUrisKey(appWidgetId), null)
                if (!stored.isNullOrBlank()) {
                    return runCatching {
                        val array = JSONArray(stored)
                        List(array.length()) { idx -> array.getString(idx) }.filter { it.isNotBlank() }
                    }.getOrDefault(emptyList())
                }
            }
            val legacy = prefs.getString(PREF_FIRST_IMAGE_URI_LEGACY, null)
            return if (legacy.isNullOrBlank()) emptyList() else listOf(legacy)
        }

        private fun loadImageIndex(prefs: android.content.SharedPreferences, appWidgetId: Int): Int {
            if (appWidgetId == -1) return 0
            return prefs.getInt(imageIndexKey(appWidgetId), 0)
        }

        private fun saveImageIndex(
            prefs: android.content.SharedPreferences,
            appWidgetId: Int,
            index: Int
        ) {
            if (appWidgetId == -1) return
            prefs.edit().putInt(imageIndexKey(appWidgetId), index).apply()
        }

        private fun advanceToNextImage(context: Context, appWidgetId: Int) {
            if (appWidgetId == -1) return
            val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            val uris = loadImageUris(prefs, appWidgetId)
            if (uris.size <= 1) return

            val current = loadImageIndex(prefs, appWidgetId).coerceAtLeast(0)
            val next = (current + 1) % uris.size
            saveImageIndex(prefs, appWidgetId, next)
            updateSingleWidget(context, appWidgetId)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

            val imageUris = loadImageUris(prefs, appWidgetId)
            val imageIndex = loadImageIndex(prefs, appWidgetId)
            val selectedImageUri = imageUris.getOrNull(imageIndex.coerceAtLeast(0))

            val views = RemoteViews(context.packageName, R.layout.image_flip_widget)
            views.setViewVisibility(
                R.id.appwidget_header_label,
                if (context.resources.getBoolean(R.bool.show_debug_label)) View.VISIBLE else View.GONE
            )

            if (!selectedImageUri.isNullOrBlank()) {
                val bitmap = runCatching {
                    loadScaledBitmapForWidget(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        uri = Uri.parse(selectedImageUri)
                    )
                }.getOrNull()

                if (bitmap != null) {
                    views.setViewVisibility(R.id.background_image, android.view.View.VISIBLE)
                    views.setImageViewBitmap(R.id.background_image, bitmap)
                    views.setOnClickPendingIntent(
                        R.id.background_image,
                        nextImagePendingIntent(context, appWidgetId)
                    )
                } else {
                    views.setViewVisibility(R.id.background_image, android.view.View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.background_image, android.view.View.GONE)
            }

            views.setOnClickPendingIntent(R.id.settings_button, imagePickerPendingIntent(context, appWidgetId))

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

        if (context != null && action == ACTION_NEXT_IMAGE) {
            val appWidgetId =
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                advanceToNextImage(context, appWidgetId)
            } else {
                updateAllWidgets(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }


}
