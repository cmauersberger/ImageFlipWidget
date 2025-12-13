package org.example.imageflipwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PinWidgetResultReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PIN_RESULT = "org.example.imageflipwidget.PIN_WIDGET_RESULT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("ImageFlipWidget", "Pin widget callback received: action=${intent.action}")
        Toast.makeText(context, R.string.pin_callback_received, Toast.LENGTH_LONG).show()
    }
}
