package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyInterceptorService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("KeyInterceptor", "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_Q) {
            // Пропускаємо Ctrl+Q, Alt+Q штатно далі в систему, якщо вони натиснуті
            if (event.isCtrlPressed || event.isAltPressed) {
                return false
            }

            // Пропускаємо віртуальні івенти від нашого IME, щоб не зациклитися
            if (event.deviceId <= 0) {
                return false
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                // ДИНАМІЧНО отримуємо символ, враховуючи поточну розкладку системи та Shift!
                val unicodeChar = event.getUnicodeChar(event.metaState)
                val charToInsert = unicodeChar.toChar().toString()

                Log.d("KeyInterceptor", "Physical Q captured. Map resolved to symbol: '$charToInsert'")

                val injectIntent = Intent(KeyInterceptorIME.ACTION_INJECT_KEY).apply {
                    putExtra(KeyInterceptorIME.EXTRA_KEY_CODE, KeyEvent.KEYCODE_Q)
                    putExtra(KeyInterceptorIME.EXTRA_STRING_CHAR, charToInsert) // Передаємо готовий символ рядочком
                    setPackage(packageName)
                }

                sendBroadcast(injectIntent)
            }

            return true
        }

        return super.onKeyEvent(event)
    }
}