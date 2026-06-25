package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import java.io.File

class KeyInterceptorService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        var isCatcherModeActive = false
    }

    private val availableLayouts = mutableListOf<LayoutModel>()
    private var activeLayoutIndices = mutableListOf<Int>()
    private var currentIndexInActiveList = 0
    private var customRemaps = mutableMapOf<Int, String>()
    private var isDigiBox = false

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { removeOverlay() }
    private lateinit var prefs: SharedPreferences
    private var inputDeviceListener: InputManager.InputDeviceListener? = null
    private val checkKeyboardsRunnable = Runnable { checkKeyboardsAndSwitch() }

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
        
        isDigiBox = Build.BRAND.equals("DIGI", ignoreCase = true) || Build.MODEL.contains("DIGI", ignoreCase = true) || Build.MODEL.contains("R2A", ignoreCase = true)

        loadAllLayouts()
        loadActiveSettings()
        setupInputDeviceListener()
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
        
        customRemaps.clear()
        val jsonStr = prefs.getString("custom_remaps", "{}") ?: "{}"
        try {
            val obj = org.json.JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val keyCode = key.toIntOrNull()
                if (keyCode != null) {
                    customRemaps[keyCode] = obj.getString(key)
                }
            }
        } catch (e: Exception) {
            Log.e("KeyInterceptor", "Error loading custom remaps", e)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "layouts_last_updated") {
            loadAllLayouts()
        }
        loadActiveSettings()
        if (key == "smart_ime_switcher_enabled" || key == "fallback_ime_id") {
            hideHandler.removeCallbacks(checkKeyboardsRunnable)
            hideHandler.post(checkKeyboardsRunnable)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.deviceId > 0) {
            checkSwitchOnKeyPress(event)
        }

        if (isCatcherModeActive) {
            val safeKeys = listOf(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 
                KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS
            )
            if (event.keyCode !in safeKeys) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    isCatcherModeActive = false
                    val intent = Intent("com.kostyamat.r2r_q.ACTION_KEY_CAUGHT")
                    intent.setPackage(packageName)
                    intent.putExtra("keyCode", event.keyCode)
                    sendBroadcast(intent)
                }
                return true
            }
        }

        if (!ImeSwitcherHelper.isRealPhysicalKeyboardConnected(this)) {
            val customAction = customRemaps[event.keyCode]
            if (customAction != null) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    executeAction(customAction)
                }
                return true
            }
            
            if (isDigiBox && event.keyCode == KeyEvent.KEYCODE_Q) {
                val qAction = prefs.getString("q_key_action", "default")
                if (qAction == "default") {
                    return false
                } else {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        executeAction(qAction!!)
                    }
                    return true
                }
            }
        }

        val isWinPressed = event.keyCode == KeyEvent.KEYCODE_META_LEFT || event.keyCode == KeyEvent.KEYCODE_META_RIGHT
        val isCtrlSpace = event.keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed
        if (isCtrlSpace || isWinPressed) {
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

    private fun executeAction(action: String) {
        when (action) {
            "action_back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "action_home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "action_recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "action_play_pause" -> {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            }
            "action_settings" -> {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            else -> {
                if (action.startsWith("intent:")) {
                    try {
                        val uri = action.substringAfter("intent:")
                        val intent = Intent.parseUri(uri, 0)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("KeyInterceptor", "Failed to parse/launch custom intent", e)
                    }
                } else if (action.startsWith("app:")) {
                    try {
                        val pkg = action.substringAfter("app:")
                        val intent = packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("KeyInterceptor", "Failed to launch app $action", e)
                    }
                }
            }
        }
    }

    private fun injectChar(char: String) {
        val ime = KeyInterceptorIME.getInstance()
        Log.d("KeyInterceptor", "injectChar: char=$char, KeyInterceptorIME instance is null: ${ime == null}")
        if (ime != null) {
            val success = ime.commitTextDirectly(char)
            if (!success) {
                val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFocusedNodeInActiveWindow()
                if (focusedNode?.isEditable == true) {
                    showLanguageOverlay(getString(R.string.no_focus), isError = true)
                }
                focusedNode?.recycle()
            }
        } else {
            val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFocusedNodeInActiveWindow()
            if (focusedNode?.isEditable == true) {
                showLanguageOverlay(getString(R.string.ime_off), isError = true)
                
                // Якщо в налаштуваннях стоїть наш IME, але його сервіс не запущено,
                // робимо примусове оновлення фокусу для його біндингу системою.
                val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                val compName = ComponentName(this, KeyInterceptorIME::class.java)
                val myImeId = compName.flattenToShortString()
                if (currentIme == myImeId) {
                    Log.d("KeyInterceptor", "IME is default in settings but instance is null. Requesting focus refresh to force bind.")
                    refreshInputFocus(delayMs = 0)
                }
            }
            focusedNode?.recycle()
        }
    }

    private fun showLanguageOverlay(text: String, isError: Boolean = false, durationMs: Long = 1500L) {
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
        hideHandler.postDelayed(hideRunnable, durationMs)
    }

    private fun removeOverlay() {
        if (windowManager != null && overlayTextView != null) {
            try { windowManager?.removeView(overlayTextView) } catch (e: Exception) {}
            overlayTextView = null
        }
    }

    private var isEditableFocused = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED || type == AccessibilityEvent.TYPE_VIEW_CLICKED || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val node = event.source
            if (node != null) {
                var hasEditableFocus = false
                if (node.isEditable) {
                    hasEditableFocus = true
                } else {
                    val focusedNode = node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focusedNode?.isEditable == true) {
                        hasEditableFocus = true
                    }
                    focusedNode?.recycle()
                }
                
                isEditableFocused = hasEditableFocus
                
                if (hasEditableFocus) {
                    hideHandler.removeCallbacks(checkKeyboardsRunnable)
                    hideHandler.postDelayed(checkKeyboardsRunnable, 100)
                }
                node.recycle()
            }
        }
    }
    override fun onInterrupt() {}

    private fun setupInputDeviceListener() {
        val inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                hideHandler.removeCallbacks(checkKeyboardsRunnable)
                hideHandler.postDelayed(checkKeyboardsRunnable, 1000)
            }
            override fun onInputDeviceRemoved(deviceId: Int) {
                hideHandler.removeCallbacks(checkKeyboardsRunnable)
                hideHandler.postDelayed(checkKeyboardsRunnable, 500)
            }
            override fun onInputDeviceChanged(deviceId: Int) {
                hideHandler.removeCallbacks(checkKeyboardsRunnable)
                hideHandler.postDelayed(checkKeyboardsRunnable, 500)
            }
        }
        inputDeviceListener = listener
        inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        
        // Initial check
        checkKeyboardsAndSwitch()
    }

    private fun checkKeyboardsAndSwitch() {
        val hasPermission = ImeSwitcherHelper.hasSecureSettingsPermission(this)
        if (!hasPermission) {
            Log.w("KeyInterceptor", "Cannot switch IME: WRITE_SECURE_SETTINGS not granted")
            return
        }

        val smartEnabled = prefs.getBoolean("smart_ime_switcher_enabled", false)
        if (!smartEnabled) return

        val compName = ComponentName(this, KeyInterceptorIME::class.java)
        val myImeId = compName.flattenToShortString()
        
        var fallbackImeId = prefs.getString("fallback_ime_id", null)
        if (fallbackImeId == null || fallbackImeId == "none") {
            fallbackImeId = ImeSwitcherHelper.getDefaultFallbackIme(this)
        }

        if (fallbackImeId == null) {
            Log.w("KeyInterceptor", "No fallback IME found or selected")
            return
        }

        val isKeyboardConnected = ImeSwitcherHelper.isRealPhysicalKeyboardConnected(this)
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

        val targetIme = if (isKeyboardConnected) {
            myImeId
        } else {
            if (isEditableFocused) fallbackImeId else currentIme
        }

        Log.d("KeyInterceptor", "checkKeyboardsAndSwitch: connected=$isKeyboardConnected, current=$currentIme, target=$targetIme, focused=$isEditableFocused")

        if (currentIme != targetIme) {
            val success = ImeSwitcherHelper.switchIme(this, targetIme)
            Log.d("KeyInterceptor", "checkKeyboardsAndSwitch: switch success=$success")
            if (success) {
                val imeName = ImeSwitcherHelper.getImeLabel(this, targetIme)
                if (targetIme == myImeId) {
                    showLanguageOverlay(getString(R.string.toast_ime_activated_long, imeName), durationMs = 10000L)
                    refreshInputFocus()
                } else {
                    showLanguageOverlay(getString(R.string.toast_ime_activated_short, imeName), durationMs = 2000L)
                }
            }
        }
    }

    private var lastFocusRefreshTime = 0L

    private fun refreshInputFocus(delayMs: Long = 300) {
        val now = System.currentTimeMillis()
        if (now - lastFocusRefreshTime < 2000) {
            Log.d("KeyInterceptor", "refreshInputFocus: skipped to avoid spamming")
            return
        }

        hideHandler.postDelayed({
            val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFocusedNodeInActiveWindow()
            if (focusedNode != null) {
                if (focusedNode.isEditable) {
                    lastFocusRefreshTime = System.currentTimeMillis()
                    Log.d("KeyInterceptor", "Refreshing input focus for node: ${focusedNode.className}")
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
                    hideHandler.postDelayed({
                        focusedNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        focusedNode.recycle()
                    }, 150)
                } else {
                    focusedNode.recycle()
                }
            }
        }, delayMs)
    }

    private fun findFocusedNodeInActiveWindow(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val result = findFocusedNodeRecursive(root)
        if (result != root) {
            root.recycle()
        }
        return result
    }

    private fun findFocusedNodeRecursive(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedNodeRecursive(child)
            if (found != null) {
                if (found != child) {
                    child.recycle()
                }
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun checkSwitchOnKeyPress(event: KeyEvent) {
        val smartEnabled = prefs.getBoolean("smart_ime_switcher_enabled", false)
        if (!smartEnabled) return

        val compName = ComponentName(this, KeyInterceptorIME::class.java)
        val myImeId = compName.flattenToShortString()
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

        Log.d("KeyInterceptor", "checkSwitchOnKeyPress: key=${event.keyCode}, current=$currentIme, target=$myImeId")

        if (currentIme != myImeId) {
            val device = event.device
            if (device != null && !device.isVirtual) {
                val sources = device.sources
                if ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
                    val keysToCheck = intArrayOf(
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_B,
                        KeyEvent.KEYCODE_C,
                        KeyEvent.KEYCODE_D
                    )
                    val hasKeys = device.hasKeys(*keysToCheck)
                    if (hasKeys.all { it }) {
                        val success = ImeSwitcherHelper.switchIme(this, myImeId)
                        Log.d("KeyInterceptor", "checkSwitchOnKeyPress: switch success=$success")
                        if (success) {
                            val imeName = ImeSwitcherHelper.getImeLabel(this, myImeId)
                            showLanguageOverlay(getString(R.string.toast_ime_activated_long, imeName), durationMs = 10000L)
                            refreshInputFocus()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("KeyInterceptor", "Service onDestroy")
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        
        hideHandler.removeCallbacks(checkKeyboardsRunnable)
        
        inputDeviceListener?.let { listener ->
            val inputManager = getSystemService(Context.INPUT_SERVICE) as? InputManager
            inputManager?.unregisterInputDeviceListener(listener)
        }
        
        removeOverlay()
    }
}
