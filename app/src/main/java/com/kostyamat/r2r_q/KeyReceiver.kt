package com.kostyamat.r2r_q

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == KeyInterceptorIME.ACTION_INJECT_KEY) {
            val keyCode = intent.getIntExtra(KeyInterceptorIME.EXTRA_KEY_CODE, -1)
            val charToInsert = intent.getStringExtra(KeyInterceptorIME.EXTRA_STRING_CHAR)

            if (keyCode != -1) {
                Log.d("KeyReceiver", "Static Receiver triggered for keyCode: $keyCode")

                val ime = KeyInterceptorIME.getInstance()
                if (ime != null) {
                    Log.d("KeyReceiver", "IME is alive, injecting via active instance")
                    ime.triggerKey(keyCode, charToInsert)
                } else {
                    Log.w("KeyReceiver", "IME is cold, forcing system interaction via fallback")
                    KeyInterceptorIME.getFallbackInstance()?.triggerKey(keyCode, charToInsert)
                }
            }
        }
    }
}