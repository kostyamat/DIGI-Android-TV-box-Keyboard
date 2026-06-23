package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.io.File

class KeyInterceptorService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val availableLayouts = mutableListOf<LayoutModel>()
    private var activeLayoutIndices = mutableListOf<Int>()
    private var currentIndexInActiveList = 0

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { removeOverlay() }
    private lateinit var prefs: SharedPreferences

    private val storageContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createDeviceProtectedStorageContext()
        } else {
            this
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("KeyInterceptor", "Service onCreate - system is attempting to start the service")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        loadAllLayouts()
        loadActiveSettings()
        Log.d("KeyInterceptor", "Service connected. Dynamic layout engine active.")
    }

    private fun loadAllLayouts() {
        availableLayouts.clear()
        try {
            val assetManager = this.assets
            assetManager.list("layouts")?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        assetManager.open("layouts/$fileName").use { inputStream ->
                            val jsonString = inputStream.bufferedReader().use { it.readText() }
                            availableLayouts.add(LayoutModel.fromJson(jsonString))
                        }
                    } catch (e: Exception) {
                        Log.e("KeyInterceptor", "Error loading asset layout: $fileName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("KeyInterceptor", "Error listing layouts from assets", e)
        }

        val externalDir = File(storageContext.filesDir, "layouts")
        if (externalDir.exists()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    try {
                        val jsonString = file.readText()
                        availableLayouts.add(LayoutModel.fromJson(jsonString))
                    } catch (e: Exception) {
                        Log.e("KeyInterceptor", "Error loading layout from file: ${file.name}", e)
                    }
                }
            }
        }
        Log.d("KeyInterceptor", "Loaded ${availableLayouts.size} layouts.")
    }

    private fun loadActiveSettings() {
        val newActive = mutableListOf<Int>()
        availableLayouts.forEachIndexed { index, layout ->
            if (prefs.getBoolean("lang_${layout.id}", true)) {
                newActive.add(index)
            }
        }
        if (newActive.isEmpty() && availableLayouts.isNotEmpty()) {
            newActive.add(0)
        }
        activeLayoutIndices = newActive
        if (currentIndexInActiveList >= activeLayoutIndices.size) {
            currentIndexInActiveList = 0
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadActiveSettings()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (activeLayoutIndices.isNotEmpty()) {
                    currentIndexInActiveList = (currentIndexInActiveList + 1) % activeLayoutIndices.size
                    val layout = availableLayouts[activeLayoutIndices[currentIndexInActiveList]]
                    showLanguageOverlay(layout.shortName)
                }
            }
            return true
        }

        val isAltGr = (event.metaState and KeyEvent.META_ALT_RIGHT_ON) != 0
        if (isAltGr && event.action == KeyEvent.ACTION_DOWN) {
            val altChar = when(event.keyCode) {
                KeyEvent.KEYCODE_E -> "€"
                KeyEvent.KEYCODE_U -> "ґ"
                KeyEvent.KEYCODE_I -> "і"
                KeyEvent.KEYCODE_S -> "ș"
                KeyEvent.KEYCODE_T -> "ț"
                KeyEvent.KEYCODE_A -> "ă"
                else -> null
            }
            if (altChar != null) {
                injectChar(altChar)
                return true
            }
        }

        if (activeLayoutIndices.isNotEmpty()) {
            val currentLayout = availableLayouts[activeLayoutIndices[currentIndexInActiveList]]
            val translation = currentLayout.map[event.keyCode]

            if (translation != null) {
                if (event.isCtrlPressed || (event.isAltPressed && !isAltGr) || event.deviceId <= 0) return false

                if (event.action == KeyEvent.ACTION_DOWN) {
                    val isShifted = event.isShiftPressed xor event.isCapsLockOn
                    val charToInsert = if (isAltGr) {
                        if (isShifted) {
                            translation.altGrShift ?: translation.altGr ?: translation.shift
                        } else {
                            translation.altGr ?: translation.normal
                        }
                    } else {
                        if (isShifted) translation.shift else translation.normal
                    }
                    injectChar(charToInsert)
                }
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun injectChar(char: String) {
        val ime = KeyInterceptorIME.getInstance()
        if (ime != null) {
            val success = ime.commitTextDirectly(char)
            if (!success) showLanguageOverlay(getString(R.string.no_focus), isError = true)
        } else {
            showLanguageOverlay(getString(R.string.ime_off), isError = true)
        }
    }

    private fun showLanguageOverlay(text: String, isError: Boolean = false) {
        if (windowManager == null) return
        hideHandler.removeCallbacks(hideRunnable)

        if (overlayTextView == null) {
            overlayTextView = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 24f
                setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                setPadding(50, 25, 50, 25)
                gravity = Gravity.CENTER
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 80
            }
            try { windowManager?.addView(overlayTextView, params) } catch (e: Exception) { return }
        }

        val bgAlpha = 200
        val bgColor = if (isError) Color.argb(bgAlpha, 180, 0, 0) else Color.argb(bgAlpha, 40, 40, 40)
        overlayTextView?.apply {
            this.text = text
            this.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColor)
                cornerRadius = 30f
                setStroke(3, if (isError) Color.RED else Color.LTGRAY)
            }
        }
        hideHandler.postDelayed(hideRunnable, 1500)
    }

    private fun removeOverlay() {
        if (windowManager != null && overlayTextView != null) {
            try { windowManager?.removeView(overlayTextView) } catch (e: Exception) {}
            overlayTextView = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        Log.d("KeyInterceptor", "Service onDestroy")
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        removeOverlay()
    }
}
