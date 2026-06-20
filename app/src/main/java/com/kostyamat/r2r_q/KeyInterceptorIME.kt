package com.kostyamat.r2r_q

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.inputmethod.InputConnection

/**
 * Proxy Input Method Service (No UI).
 * Receives direct calls from KeyInterceptorService for low-latency text insertion.
 */
class KeyInterceptorIME : InputMethodService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("KeyInterceptorIME", "Proxy-IME Started (Direct Access Mode)")
    }

    /**
     * Commits text directly to the active input field.
     * @return true if successful, false if InputConnection is null.
     */
    fun commitTextDirectly(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        
        val ic: InputConnection? = currentInputConnection
        return if (ic != null) {
            ic.commitText(text, 1)
            true
        } else {
            Log.e("KeyInterceptorIME", "InputConnection is null. Active field lost focus?")
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        Log.d("KeyInterceptorIME", "Proxy-IME Destroyed")
    }

    companion object {
        @Volatile
        private var instance: KeyInterceptorIME? = null

        fun getInstance(): KeyInterceptorIME? = instance
        
        // Broadcast constants kept for reference/compatibility if needed
        const val ACTION_INJECT_KEY = "com.kostyamat.r2r_q.ACTION_INJECT_KEY"
        const val EXTRA_KEY_CODE = "extra_key_code"
        const val EXTRA_STRING_CHAR = "extra_string_char"
    }
}
