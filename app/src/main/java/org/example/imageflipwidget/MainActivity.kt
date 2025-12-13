package org.example.imageflipwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val pinButton = findViewById<Button>(R.id.pin_widget_button)
        val diagnosticsText = findViewById<TextView>(R.id.diagnostics_text)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, FlipperWidgetProvider::class.java)

        val providers = appWidgetManager.installedProviders
        val providerLines = buildString {
            appendLine("Installed widget providers: ${providers.size}")
            for (info in providers) {
                val label = try {
                    info.loadLabel(packageManager)?.toString().orEmpty()
                } catch (_: Throwable) {
                    ""
                }
                appendLine("- ${info.provider} label='${label}'")
            }
        }.trimEnd()
        diagnosticsText.text = providerLines
        Log.i("ImageFlipWidget", providerLines)

        val isPinSupported =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                appWidgetManager.isRequestPinAppWidgetSupported

        Log.i("ImageFlipWidget", "isRequestPinAppWidgetSupported=$isPinSupported")

        statusText.text = if (isPinSupported) {
            getString(R.string.pin_supported)
        } else {
            getString(R.string.pin_not_supported)
        }

        pinButton.visibility = if (isPinSupported) View.VISIBLE else View.GONE
        pinButton.setOnClickListener {
            if (!isPinSupported) return@setOnClickListener

            try {
                val callbackIntent = Intent(this, PinWidgetResultReceiver::class.java).apply {
                    action = PinWidgetResultReceiver.ACTION_PIN_RESULT
                }
                val successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val accepted = appWidgetManager.requestPinAppWidget(provider, null, successCallback)
                Log.i("ImageFlipWidget", "requestPinAppWidget accepted=$accepted")
                Toast.makeText(
                    this,
                    if (accepted) R.string.pin_requested else R.string.pin_request_rejected,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Throwable) {
                Log.e("ImageFlipWidget", "requestPinAppWidget failed", e)
                Toast.makeText(this, e.javaClass.simpleName + ": " + (e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
}
