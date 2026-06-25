package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

object ImeSwitcherHelper {

    fun hasSecureSettingsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isRealPhysicalKeyboardConnected(context: Context): Boolean {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return false
        val deviceIds = inputManager.inputDeviceIds
        for (id in deviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            if (device.isVirtual) continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                // Check if the device has ABCD keys to identify a real alphabetic keyboard
                val keysToCheck = intArrayOf(
                    KeyEvent.KEYCODE_A,
                    KeyEvent.KEYCODE_B,
                    KeyEvent.KEYCODE_C,
                    KeyEvent.KEYCODE_D
                )
                val hasKeys = device.hasKeys(*keysToCheck)
                if (hasKeys.all { it }) {
                    Log.d("ImeSwitcherHelper", "Real physical keyboard detected: ${device.name} (ID: ${device.id})")
                    return true
                }
            }
        }
        return false
    }

    fun getDefaultFallbackIme(context: Context): String? {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return null
        val enabledImes = imm.enabledInputMethodList
        val nonOurs = enabledImes.filter { it.packageName != context.packageName }
        val preferred = nonOurs.firstOrNull { it.packageName.contains("gboard") || it.packageName.contains("latin") }
        return preferred?.id ?: nonOurs.firstOrNull()?.id
    }

    fun getImeLabel(context: Context, imeId: String): String {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return imeId
        val list = imm.enabledInputMethodList
        val info = list.firstOrNull { it.id == imeId } ?: return imeId
        val pm = context.packageManager
        return info.loadLabel(pm).toString()
    }

    fun switchIme(service: AccessibilityService, targetImeId: String): Boolean {
        if (!hasSecureSettingsPermission(service)) {
            Log.w("ImeSwitcherHelper", "No WRITE_SECURE_SETTINGS permission to switch IME")
            return false
        }
        return try {
            val contentResolver = service.contentResolver
            val imm = service.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledImeIds = imm?.enabledInputMethodList?.map { it.id } ?: emptyList()
            
            // Переконуємося, що цільова клавіатура увімкнена в системі
            if (!enabledImeIds.contains(targetImeId)) {
                val newEnabled = (enabledImeIds + targetImeId).joinToString(":")
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS, newEnabled)
                Log.d("ImeSwitcherHelper", "Automatically enabled target IME in settings: $targetImeId")
            }

            // На Android 11+ (API 30+) AccessibilityService має нативний метод для зміни активної клавіатури!
            // Це гарантує "гаряче" перемикання на рівні системи.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val switched = service.softKeyboardController.switchToInputMethod(targetImeId)
                Log.d("ImeSwitcherHelper", "API 30+ switchToInputMethod result: $switched for $targetImeId")
            }

            // Резервний захід: встановлюємо через Settings
            Settings.Secure.putString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD, targetImeId)
            Settings.Secure.putString(contentResolver, Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, "-1")
            
            Log.i("ImeSwitcherHelper", "Successfully requested input method switch to: $targetImeId")
            true
        } catch (e: Exception) {
            Log.e("ImeSwitcherHelper", "Failed to switch input method to $targetImeId", e)
            false
        }
    }
}
