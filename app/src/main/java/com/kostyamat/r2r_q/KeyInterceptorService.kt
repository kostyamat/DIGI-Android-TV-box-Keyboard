package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class KeyInterceptorService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val allLangShort = arrayOf("EN", "ES", "UA", "RU")
    private var activeLanguageIndices = mutableListOf(0, 1, 2, 3)
    private var currentIndexInActiveList = 0

    // Main layout matrix: [EN, ES, UA, RU] -> Pair(Lower, Upper)
    private val keyMap = mapOf(
        KeyEvent.KEYCODE_A to arrayOf(Pair("a", "A"), Pair("a", "A"), Pair("ф", "Ф"), Pair("ф", "Ф")),
        KeyEvent.KEYCODE_B to arrayOf(Pair("b", "B"), Pair("b", "B"), Pair("и", "И"), Pair("и", "И")),
        KeyEvent.KEYCODE_C to arrayOf(Pair("c", "C"), Pair("c", "C"), Pair("с", "С"), Pair("с", "С")),
        KeyEvent.KEYCODE_D to arrayOf(Pair("d", "D"), Pair("d", "D"), Pair("в", "В"), Pair("в", "В")),
        KeyEvent.KEYCODE_E to arrayOf(Pair("e", "E"), Pair("e", "E"), Pair("у", "У"), Pair("у", "У")),
        KeyEvent.KEYCODE_F to arrayOf(Pair("f", "F"), Pair("f", "F"), Pair("а", "А"), Pair("а", "А")),
        KeyEvent.KEYCODE_G to arrayOf(Pair("g", "G"), Pair("g", "G"), Pair("п", "П"), Pair("п", "П")),
        KeyEvent.KEYCODE_H to arrayOf(Pair("h", "H"), Pair("h", "H"), Pair("р", "Р"), Pair("р", "Р")),
        KeyEvent.KEYCODE_I to arrayOf(Pair("i", "I"), Pair("i", "I"), Pair("ш", "Ш"), Pair("ш", "Ш")),
        KeyEvent.KEYCODE_J to arrayOf(Pair("j", "J"), Pair("j", "J"), Pair("о", "О"), Pair("о", "О")),
        KeyEvent.KEYCODE_K to arrayOf(Pair("k", "K"), Pair("k", "K"), Pair("л", "Л"), Pair("л", "Л")),
        KeyEvent.KEYCODE_L to arrayOf(Pair("l", "L"), Pair("l", "L"), Pair("д", "Д"), Pair("д", "Д")),
        KeyEvent.KEYCODE_M to arrayOf(Pair("m", "M"), Pair("m", "M"), Pair("ь", "Ь"), Pair("ь", "Ь")),
        KeyEvent.KEYCODE_N to arrayOf(Pair("n", "N"), Pair("n", "N"), Pair("т", "Т"), Pair("т", "Т")),
        KeyEvent.KEYCODE_O to arrayOf(Pair("o", "O"), Pair("o", "O"), Pair("щ", "Щ"), Pair("щ", "Щ")),
        KeyEvent.KEYCODE_P to arrayOf(Pair("p", "P"), Pair("p", "P"), Pair("з", "З"), Pair("з", "З")),
        KeyEvent.KEYCODE_Q to arrayOf(Pair("q", "Q"), Pair("q", "Q"), Pair("й", "Й"), Pair("й", "Й")),
        KeyEvent.KEYCODE_R to arrayOf(Pair("r", "R"), Pair("r", "R"), Pair("к", "К"), Pair("к", "К")),
        KeyEvent.KEYCODE_S to arrayOf(Pair("s", "S"), Pair("s", "S"), Pair("і", "І"), Pair("ы", "Ы")),
        KeyEvent.KEYCODE_T to arrayOf(Pair("t", "T"), Pair("t", "T"), Pair("е", "Е"), Pair("е", "Е")),
        KeyEvent.KEYCODE_U to arrayOf(Pair("u", "U"), Pair("u", "U"), Pair("г", "Г"), Pair("г", "Г")),
        KeyEvent.KEYCODE_V to arrayOf(Pair("v", "V"), Pair("v", "V"), Pair("м", "М"), Pair("м", "М")),
        KeyEvent.KEYCODE_W to arrayOf(Pair("w", "W"), Pair("w", "W"), Pair("ц", "Ц"), Pair("ц", "Ц")),
        KeyEvent.KEYCODE_X to arrayOf(Pair("x", "X"), Pair("x", "X"), Pair("ч", "Ч"), Pair("ч", "Ч")),
        KeyEvent.KEYCODE_Y to arrayOf(Pair("y", "Y"), Pair("y", "Y"), Pair("н", "Н"), Pair("н", "Н")),
        KeyEvent.KEYCODE_Z to arrayOf(Pair("z", "Z"), Pair("z", "Z"), Pair("я", "Я"), Pair("я", "Я")),

        KeyEvent.KEYCODE_SEMICOLON to arrayOf(Pair(";", ":"), Pair("ñ", "Ñ"), Pair("ж", "Ж"), Pair("ж", "Ж")),
        KeyEvent.KEYCODE_APOSTROPHE to arrayOf(Pair("'", "\""), Pair("'", "?"), Pair("є", "Є"), Pair("э", "Э")),
        KeyEvent.KEYCODE_GRAVE to arrayOf(Pair("`", "~"), Pair("º", "ª"), Pair("'","ʼ"), Pair("ё", "Ё")),
        KeyEvent.KEYCODE_COMMA to arrayOf(Pair(",", "<"), Pair(",", "."), Pair("б", "Б"), Pair("б", "Б")),
        KeyEvent.KEYCODE_PERIOD to arrayOf(Pair(".", ">"), Pair(".", "-"), Pair("ю", "Ю"), Pair("ю", "Ю")),
        KeyEvent.KEYCODE_LEFT_BRACKET to arrayOf(Pair("[", "{"), Pair("`", "^"), Pair("х", "Х"), Pair("х", "Х")),
        KeyEvent.KEYCODE_RIGHT_BRACKET to arrayOf(Pair("]", "}"), Pair("+", "*"), Pair("ї", "Ї"), Pair("ъ", "Ъ"))
    )

    // AltGr (Right Alt) symbols
    private val altGrMap = mapOf(
        KeyEvent.KEYCODE_E to "€",
        KeyEvent.KEYCODE_L to "ł",
        KeyEvent.KEYCODE_U to "ґ",
        KeyEvent.KEYCODE_I to "і",
        KeyEvent.KEYCODE_S to "ś",
        KeyEvent.KEYCODE_N to "ñ"
    )

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { removeOverlay() }
    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)
        
        loadSettings()
        Log.d("KeyInterceptor", "Service connected. Direct IME mode enabled.")
    }

    private fun loadSettings() {
        val newActive = mutableListOf<Int>()
        if (prefs.getBoolean("lang_en", true)) newActive.add(0)
        if (prefs.getBoolean("lang_es", true)) newActive.add(1)
        if (prefs.getBoolean("lang_ua", true)) newActive.add(2)
        if (prefs.getBoolean("lang_ru", true)) newActive.add(3)
        
        if (newActive.isEmpty()) newActive.add(0)
        activeLanguageIndices = newActive

        if (currentIndexInActiveList >= activeLanguageIndices.size) {
            currentIndexInActiveList = 0
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadSettings()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // 1. Language Cycle: CTRL + SPACE
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                currentIndexInActiveList = (currentIndexInActiveList + 1) % activeLanguageIndices.size
                val realLangIndex = activeLanguageIndices[currentIndexInActiveList]
                showLanguageOverlay(allLangShort[realLangIndex])
            }
            return true
        }

        // 2. AltGr (Right Alt) Support
        val isAltGr = (event.metaState and KeyEvent.META_ALT_RIGHT_ON) != 0
        if (isAltGr && event.action == KeyEvent.ACTION_DOWN) {
            val altChar = altGrMap[event.keyCode]
            if (altChar != null) {
                injectChar(altChar)
                return true
            }
        }

        // 3. Custom Translation Matrix
        val translationArray = keyMap[event.keyCode]
        if (translationArray != null) {
            // Ignore if Ctrl is pressed or it's a virtual event
            if (event.isCtrlPressed || (event.isAltPressed && !isAltGr) || event.deviceId <= 0) return false

            if (event.action == KeyEvent.ACTION_DOWN) {
                val isShifted = event.isShiftPressed xor event.isCapsLockOn
                val realLangIndex = activeLanguageIndices[currentIndexInActiveList]
                val pair = translationArray[realLangIndex]
                val charToInsert = if (isShifted) pair.second else pair.first

                injectChar(charToInsert)
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    private fun injectChar(char: String) {
        val ime = KeyInterceptorIME.getInstance()
        if (ime != null) {
            val success = ime.commitTextDirectly(char)
            if (!success) {
                showLanguageOverlay("NO FOCUS", isError = true)
            }
        } else {
            showLanguageOverlay("IME DISABLED", isError = true)
            Log.e("KeyInterceptor", "IME Instance is null!")
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
                y = 80 // TV-safe top margin
            }

            try {
                windowManager?.addView(overlayTextView, params)
            } catch (e: Exception) { return }
        }

        val bgAlpha = 200
        val bgColor = if (isError) Color.argb(bgAlpha, 180, 0, 0) else Color.argb(bgAlpha, 40, 40, 40)
        
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 30f
            setStroke(3, if (isError) Color.RED else Color.LTGRAY)
        }

        overlayTextView?.apply {
            this.text = text
            this.background = backgroundDrawable
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
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        removeOverlay()
    }
}
