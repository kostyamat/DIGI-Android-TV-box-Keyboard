package com.kostyamat.r2r_q

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed received: ${intent.action}")
            // Accessibility Service та IME зазвичай запускаються системою автоматично, 
            // якщо вони були увімкнені користувачем.
        }
    }
}
