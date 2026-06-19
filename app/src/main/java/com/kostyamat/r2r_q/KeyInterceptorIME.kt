package com.kostyamat.r2r_q

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat

class KeyInterceptorIME : InputMethodService() {

    private var keyReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        fallbackInstance = this
        Log.d("KeyInterceptorIME", "IME Service running as default keyboard")

        keyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_INJECT_KEY) {
                    val keyCode = intent.getIntExtra(EXTRA_KEY_CODE, -1)
                    val charToInsert = intent.getStringExtra(EXTRA_STRING_CHAR)

                    if (keyCode != -1) {
                        triggerKey(keyCode, charToInsert)
                    }
                }
            }
        }

        val filter = IntentFilter(ACTION_INJECT_KEY)
        ContextCompat.registerReceiver(
            this,
            keyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Метод, який викликає наш Receiver
    fun triggerKey(keyCode: Int, charToInsert: String?) {
        if (keyCode == KeyEvent.KEYCODE_Q && !charToInsert.isNullOrEmpty() && charToInsert != "\u0000") {
            Log.d("KeyInterceptorIME", "Committing character via IME: $charToInsert")
            val ic = currentInputConnection
            if (ic != null) {
                // Вставляємо нативний готовий символ (враховуючи мову та Shift)
                ic.commitText(charToInsert, 1)
            } else {
                Log.w("KeyInterceptorIME", "InputConnection is null, falling back to key event")
                sendDownUpKeyEvents(KeyEvent.KEYCODE_Q)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_Q) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_Q) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        if (keyReceiver != null) {
            unregisterReceiver(keyReceiver)
        }
    }

    companion object {
        const val ACTION_INJECT_KEY = "com.kostyamat.r2r_q.ACTION_INJECT_KEY"
        const val EXTRA_KEY_CODE = "extra_key_code"
        const val EXTRA_STRING_CHAR = "extra_string_char"

        private var instance: KeyInterceptorIME? = null
        private var fallbackInstance: KeyInterceptorIME? = null

        fun getInstance(): KeyInterceptorIME? = instance
        fun getFallbackInstance(): KeyInterceptorIME? = fallbackInstance
    }
}