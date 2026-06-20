package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
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

class KeyInterceptorService : AccessibilityService() {

    private var currentLanguageIndex = 0
    private val languages = arrayOf("English", "Spanish", "Ukrainian", "Russian")
    // Скорочені назви для красивого відображення в кутку екрана
    private val langShort = arrayOf("EN", "ES", "UA", "RU")

    // Матриця символів [EN, ES, UA, RU] -> Pair(Мала, Велика)
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

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { removeOverlay() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d("KeyInterceptor", "Service connected with Overlay Layout Support")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {

        // 1. ПЕРЕХОПЛЕННЯ CTRL + SPACE
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                currentLanguageIndex = (currentLanguageIndex + 1) % languages.size
                val textToShow = langShort[currentLanguageIndex]

                Log.d("KeyInterceptor", "Language changed to: ${languages[currentLanguageIndex]}")

                Handler(Looper.getMainLooper()).post {
                    showLanguageOverlay(textToShow)
                }
            }
            return true
        }

        // 2. СТАНДАРТНИЙ ПЕРЕКЛАД ЛІТЕР
        val translationArray = keyMap[event.keyCode]
        if (translationArray != null) {
            if (event.isCtrlPressed || event.isAltPressed) {
                return false
            }
            if (event.deviceId <= 0) {
                return false
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                val isShifted = event.isShiftPressed xor event.isCapsLockOn
                val languagePair = translationArray[currentLanguageIndex]
                val charToInsert = if (isShifted) languagePair.second else languagePair.first

                val injectIntent = Intent(KeyInterceptorIME.ACTION_INJECT_KEY).apply {
                    putExtra(KeyInterceptorIME.EXTRA_KEY_CODE, event.keyCode)
                    putExtra(KeyInterceptorIME.EXTRA_STRING_CHAR, charToInsert)
                    setPackage(packageName)
                }
                sendBroadcast(injectIntent)
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    // МЕТОД МАЛЮВАННЯ ВІКНА ПОВЕРХ ІНШИХ ПРОГРАМ (З КОЛЬОРАМИ)
    private fun showLanguageOverlay(text: String) {
        if (windowManager == null) return

        if (overlayTextView != null) {
            overlayTextView?.text = text
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 3000) // Залишив 3 секунди, 10 забагато висить
            return
        }

        // Локально створюємо кастомний фон: Синій контейнер із жовтим кантиком
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#0057B7")) // Контрастний синій фон
            cornerRadius = 12f // М'яке заокруглення кутів
            setStroke(2, Color.parseColor("#FFD700")) // Чітка жовта рамка
        }

        // Формуємо жовтий жирний текст на нашому синьому фоні
        overlayTextView = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#FFD700")) // Яскравий жовтий колір тексту
            textSize = 22f // Збільшений розмір для впевненого читання здалеку
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            background = backgroundDrawable // Заряджаємо наш GradientDrawable
            setPadding(35, 15, 35, 15)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END // Нижній правий кут
            x = 60 // Відступ від правого краю екрана
            y = 60 // Відступ від нижнього краю екрана
        }

        try {
            windowManager?.addView(overlayTextView, params)
            hideHandler.postDelayed(hideRunnable, 3000)
        } catch (e: Exception) {
            Log.e("KeyInterceptor", "Failed to add overlay window.", e)
        }
    }

    private fun removeOverlay() {
        if (windowManager != null && overlayTextView != null) {
            try {
                windowManager?.removeView(overlayTextView)
            } catch (e: Exception) {
                // Вікно вже прибрано
            }
            overlayTextView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}