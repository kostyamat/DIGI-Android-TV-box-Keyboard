package com.kostyamat.r2r_q

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat

class KeyInterceptorIME : InputMethodService() {

    private var keyReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        fallbackInstance = this
        Log.d("KeyInterceptorIME", "Proxy-IME Server Started and Ready")

        // Ініціалізуємо приймач бродкастів від AccessibilityService
        keyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_INJECT_KEY) {
                    val keyCode = intent.getIntExtra(EXTRA_KEY_CODE, -1)
                    val charToInsert = intent.getStringExtra(EXTRA_STRING_CHAR)

                    if (keyCode != -1 && keyCode != -2) {
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

    // Головний метод вставки тексту в активний EditText
    fun triggerKey(keyCode: Int, charToInsert: String?) {
        if (!charToInsert.isNullOrEmpty() && charToInsert != "\u0000") {
            val ic = currentInputConnection
            if (ic != null) {
                Log.d("KeyInterceptorIME", "Committing character via active IME: $charToInsert")
                // Нативно вкидаємо прорахований юнікод-символ
                ic.commitText(charToInsert, 1)
            } else {
                Log.w("KeyInterceptorIME", "InputConnection is null, falling back to key event")
                // Якщо InputConnection втрачено, кидаємо хоча б базовий код
                sendDownUpKeyEvents(keyCode)
            }
        }
    }

    // Блокуємо проходження фізичних літер через стандартний інтерфейс IME,
    // щоб уникнути хаотичного дублювання символів
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        if (keyReceiver != null) {
            unregisterReceiver(keyReceiver)
        }
        Log.d("KeyInterceptorIME", "Proxy-IME Server Destroyed")
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