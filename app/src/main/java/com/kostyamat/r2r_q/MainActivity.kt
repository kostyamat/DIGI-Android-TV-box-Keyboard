package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ClipboardManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import org.json.JSONObject
import org.json.JSONException
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var setupContainer: LinearLayout
    private lateinit var languagesContainer: LinearLayout
    private lateinit var btnImportLayout: Button
    private lateinit var spinnerFallbackIme: Spinner
    private lateinit var spinnerQAction: Spinner

    private val handler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 2000)
        }
    }

    private var catcherDialog: AlertDialog? = null
    private val keyCaughtReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.kostyamat.r2r_q.ACTION_KEY_CAUGHT") {
                val keyCode = intent.getIntExtra("keyCode", -1)
                if (keyCode != -1) {
                    catcherDialog?.dismiss()
                    Toast.makeText(context, R.string.toast_key_caught, Toast.LENGTH_SHORT).show()
                    showActionPicker(keyCode)
                }
            }
        }
    }

    // Use a device-protected context ONLY for storage (Prefs/Files).
    private val storageContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createDeviceProtectedStorageContext()
        } else {
            this
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importLayoutFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adb)

        statusTextView = findViewById(R.id.statusTextView)
        setupContainer = findViewById(R.id.setupContainer)
        languagesContainer = findViewById(R.id.languagesContainer)
        btnImportLayout = findViewById(R.id.btnImportLayout)
        spinnerFallbackIme = findViewById(R.id.spinnerFallbackIme)
        spinnerQAction = findViewById(R.id.spinnerQAction)

        val isDigiBox = Build.BRAND.equals("DIGI", ignoreCase = true) || Build.MODEL.contains("DIGI", ignoreCase = true) || Build.MODEL.contains("R2A", ignoreCase = true)
        val digiWorkaroundContainer = findViewById<LinearLayout>(R.id.digiWorkaroundContainer)
        if (!isDigiBox) {
            digiWorkaroundContainer.visibility = View.GONE
        }

        setupWizardButtons()
        setupAutoAdbSetup()
        updateStatus()
        refreshLanguagesList()
        setupSmartSwitcher()
        setupQActionSpinner()
        setupCatcherMode()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keyCaughtReceiver, IntentFilter("com.kostyamat.r2r_q.ACTION_KEY_CAUGHT"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(keyCaughtReceiver, IntentFilter("com.kostyamat.r2r_q.ACTION_KEY_CAUGHT"))
        }

        if (hasSecureSettingsPermission()) {
            val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("secure_settings_granted", true).apply()
        }

        if (hasSecureSettingsPermission()) {
            val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("secure_settings_granted", true).apply()
        }

        btnImportLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            importLauncher.launch(intent)
        }
    }

    private fun setupSmartSwitcher() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImes = imm.enabledInputMethodList.filter { it.packageName != packageName }

        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.smart_switcher_none) to "none")
        for (ime in enabledImes) {
            val label = ime.loadLabel(packageManager).toString()
            items.add(label to ime.id)
        }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items.map { it.first }) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.setTextColor(Color.WHITE)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.spinner_dropdown_item_bg)
                    val density = resources.displayMetrics.density
                    val padHoriz = (16 * density).toInt()
                    val padVert = (12 * density).toInt()
                    setPadding(padHoriz, padVert, padHoriz, padVert)
                }
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFallbackIme.adapter = adapter

        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val smartEnabled = prefs.getBoolean("smart_ime_switcher_enabled", false)
        val savedFallbackIme = prefs.getString("fallback_ime_id", null)

        if (smartEnabled && savedFallbackIme != null) {
            val index = items.indexOfFirst { it.second == savedFallbackIme }
            if (index >= 0) {
                spinnerFallbackIme.setSelection(index)
            } else {
                spinnerFallbackIme.setSelection(0)
            }
        } else {
            val currentDefault = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            val index = if (currentDefault != null && !currentDefault.contains(packageName)) {
                items.indexOfFirst { it.second == currentDefault }
            } else {
                val fallback = ImeSwitcherHelper.getDefaultFallbackIme(this)
                items.indexOfFirst { it.second == fallback }
            }
            if (index >= 0) {
                spinnerFallbackIme.setSelection(index)
            } else {
                spinnerFallbackIme.setSelection(0)
            }
        }

        spinnerFallbackIme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isFirstCall = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isFirstCall) {
                    isFirstCall = false
                    return
                }
                val selectedImeId = items[position].second
                if (selectedImeId != "none") {
                    if (!hasSecureSettingsPermission()) {
                        spinnerFallbackIme.setSelection(0)
                        return
                    }
                    prefs.edit {
                        putString("fallback_ime_id", selectedImeId)
                        putBoolean("smart_ime_switcher_enabled", true)
                    }
                } else {
                    prefs.edit {
                        putBoolean("smart_ime_switcher_enabled", false)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    data class LayoutItem(val id: String, val displayName: String, val isExternal: Boolean, val file: File?)

    private fun refreshLanguagesList() {
        languagesContainer.removeAllViews()
        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val layouts = mutableListOf<LayoutItem>()

        try {
            val assetManager = this.assets
            assetManager.list("layouts")?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        assetManager.open("layouts/$fileName").use { inputStream ->
                            val jsonString = inputStream.bufferedReader().use { it.readText() }
                            val layout = LayoutModel.fromJson(jsonString)
                            layouts.add(LayoutItem(layout.id, getLocalizedName(layout), false, null))
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error loading asset layout: $fileName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error listing assets", e)
        }

        val externalDir = File(storageContext.filesDir, "layouts")
        if (externalDir.exists()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    try {
                        val layout = LayoutModel.fromJson(file.readText())
                        layouts.add(LayoutItem(layout.id, getLocalizedName(layout), true, file))
                    } catch (e: Exception) {}
                }
            }
        }

        layouts.distinctBy { it.id }.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER_VERTICAL
            }
            val cb = CheckBox(this).apply {
                text = item.displayName
                setTextColor(Color.WHITE)
                isChecked = prefs.getBoolean("lang_${item.id}", true)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit { putBoolean("lang_${item.id}", isChecked) }
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(cb)

            if (item.isExternal && item.file != null) {
                val deleteBtn = Button(this).apply {
                    text = "X"
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.btn_delete_selector)
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(getString(R.string.dialog_delete_layout_title))
                            .setMessage(getString(R.string.dialog_delete_layout_msg, item.displayName))
                            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                                item.file.delete()
                                prefs.edit { 
                                    remove("lang_${item.id}") 
                                    putLong("layouts_last_updated", System.currentTimeMillis())
                                }
                                refreshLanguagesList()
                            }
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show()
                    }
                }
                row.addView(deleteBtn)
            }
            languagesContainer.addView(row)
        }
    }

    private fun getLocalizedName(layout: LayoutModel): String {
        val resId = resources.getIdentifier("lang_${layout.id}", "string", packageName)
        return if (resId != 0) getString(resId) else layout.name
    }

    private fun importLayoutFile(uri: Uri) {
        try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val layout = LayoutModel.fromJson(jsonString)
            val layoutsDir = File(storageContext.filesDir, "layouts")
            if (!layoutsDir.exists()) layoutsDir.mkdirs()

            val isAsset = try { assets.open("layouts/${layout.id}.json").use { true } } catch (e: Exception) { false }
            val isExternal = File(layoutsDir, "${layout.id}.json").exists()

            if (isAsset || isExternal) {
                val dialogView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                }
                val idInput = EditText(this).apply {
                    hint = getString(R.string.hint_new_layout_id)
                    setText("${layout.id}_2")
                }
                val nameInput = EditText(this).apply {
                    hint = getString(R.string.hint_new_layout_name)
                    setText("${layout.name} (2)")
                }
                dialogView.addView(idInput)
                dialogView.addView(nameInput)

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_layout_exists_title))
                    .setMessage(getString(R.string.dialog_layout_exists_msg))
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.btn_overwrite)) { _, _ ->
                        saveLayoutFile(layout.id, jsonString, layoutsDir, layout.name)
                    }
                    .setNeutralButton(getString(R.string.btn_save_as_new)) { _, _ ->
                        val newId = idInput.text.toString().trim()
                        val newName = nameInput.text.toString().trim()
                        if (newId.isNotEmpty() && newName.isNotEmpty()) {
                            try {
                                val jsonObj = JSONObject(jsonString)
                                jsonObj.put("id", newId)
                                jsonObj.put("name", newName)
                                saveLayoutFile(newId, jsonObj.toString(), layoutsDir, newName)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, getString(R.string.import_failed, e.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
            } else {
                saveLayoutFile(layout.id, jsonString, layoutsDir, layout.name)
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun saveLayoutFile(id: String, jsonString: String, dir: File, name: String) {
        try {
            val destFile = File(dir, "$id.json")
            FileOutputStream(destFile).use { it.write(jsonString.toByteArray()) }

            val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit { putLong("layouts_last_updated", System.currentTimeMillis()) }

            Toast.makeText(this, getString(R.string.import_success, name), Toast.LENGTH_SHORT).show()
            refreshLanguagesList()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }


    private fun setupWizardButtons() {
        // Step 1: Open Developer Options
        // Moved to setupAutoAdbSetup()

        // Step 2: Auto-Grant Permissions via ADB
        setupAutoAdbSetup()
    }

    private fun hasSecureSettingsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    private fun autoEnableAccessibility() {
        try {
            val serviceId = ComponentName(this, KeyInterceptorService::class.java).flattenToString()
            val contentResolver = contentResolver
            val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            
            if (!enabledServices.contains(serviceId)) {
                val newEnabledServices = if (enabledServices.isEmpty()) serviceId else "$enabledServices:$serviceId"
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newEnabledServices)
                Settings.Secure.putString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to auto-enable accessibility", e)
        }
    }

    private fun autoEnableIME() {
        try {
            val comp = ComponentName(this, KeyInterceptorIME::class.java)
            val imeId = comp.flattenToShortString()
            val contentResolver = contentResolver
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            // Отримуємо список увімкнених IME через безпечний API
            val enabledImeIds = imm.enabledInputMethodList.map { it.id }

            if (!enabledImeIds.contains(imeId)) {
                val newEnabled = (enabledImeIds + imeId).joinToString(":")
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS, newEnabled)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to auto-enable IME", e)
        }
    }

    private fun setupAutoAdbSetup() {
        findViewById<Button>(R.id.btnDevOptions)?.setOnClickListener {
            try {
                val intent = Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent("com.android.tv.settings.development.DevelopmentActivity")
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Could not open Developer Options", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnAutoGrant)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_adb_rsa_title)
                .setMessage(R.string.dialog_adb_rsa_msg)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = AdbHelper.autoGrantPermissions(this@MainActivity)
                        if (success) {
                            Toast.makeText(this@MainActivity, R.string.toast_secure_settings_granted, Toast.LENGTH_LONG).show()
                            updateStatus()
                        } else {
                            Toast.makeText(this@MainActivity, R.string.toast_adb_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ADB", text))
        Toast.makeText(this, getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        setupSmartSwitcher()
        updateStatus()
        handler.post(checkStatusRunnable)
        
        window.decorView.post {
            if (setupContainer.visibility == View.VISIBLE) {
                if (!hasSecureSettingsPermission()) {
                    findViewById<Button>(R.id.btnDevOptions)?.requestFocus()
                }
            } else if (spinnerFallbackIme.isEnabled) {
                spinnerFallbackIme.requestFocus()
            } else if (spinnerQAction.isShown && spinnerQAction.isEnabled) {
                spinnerQAction.requestFocus()
            } else {
                findViewById<Button>(R.id.btnCatchKey)?.requestFocus() ?: btnImportLayout.requestFocus()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(checkStatusRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(keyCaughtReceiver)
    }

    private fun updateStatus() {
        val isServiceEnabled = isAccessibilityServiceEnabled(this, KeyInterceptorService::class.java)
        val isImeEnabled = isImeEnabled()
        val isImeDefault = isImeDefault()
        val hasPermission = hasSecureSettingsPermission()

        statusTextView.text = if (isServiceEnabled) getString(R.string.status_active) else getString(R.string.status_inactive)
        statusTextView.setTextColor(if (isServiceEnabled) Color.GREEN else Color.RED)

        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val smartEnabled = prefs.getBoolean("smart_ime_switcher_enabled", false)

        if (hasPermission) {
            if (!prefs.getBoolean("secure_settings_granted", false)) {
                prefs.edit().putBoolean("secure_settings_granted", true).apply()
                Toast.makeText(this, getString(R.string.toast_secure_settings_granted), Toast.LENGTH_LONG).show()

                // Якщо Розумний Перемикач ще не налаштовано, увімкнути його з поточною наекранною клавіатурою
                if (!prefs.getBoolean("smart_ime_switcher_enabled", false)) {
                    val currentDefault = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                    val targetFallback = if (currentDefault != null && !currentDefault.contains(packageName)) {
                        currentDefault
                    } else {
                        ImeSwitcherHelper.getDefaultFallbackIme(this)
                    }
                    if (targetFallback != null) {
                        prefs.edit {
                            putString("fallback_ime_id", targetFallback)
                            putBoolean("smart_ime_switcher_enabled", true)
                        }
                        // Оновити спінер, щоб він відобразив збережене значення
                        setupSmartSwitcher()
                    }
                }
            }
            if (!isImeEnabled) {
                autoEnableIME()
            }
            if (!isServiceEnabled) {
                autoEnableAccessibility()
            }
        } else {
            if (prefs.getBoolean("secure_settings_granted", false)) {
                prefs.edit().putBoolean("secure_settings_granted", false).apply()
            }
        }

        spinnerFallbackIme.isEnabled = hasPermission
        if (!hasPermission) {
            if (prefs.getBoolean("smart_ime_switcher_enabled", false)) {
                prefs.edit().putBoolean("smart_ime_switcher_enabled", false).apply()
            }
            if (spinnerFallbackIme.selectedItemPosition != 0) {
                spinnerFallbackIme.setSelection(0)
            }
        }

        // Приховувати весь блок налаштування, якщо сервіс та IME увімкнені, і є необхідний дозвіл.
        val isSetupComplete = isServiceEnabled && isImeEnabled && hasPermission

        if (isSetupComplete) {
            setupContainer.visibility = View.GONE
        } else {
            setupContainer.visibility = View.VISIBLE
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServicesSetting.contains(expectedComponentName.flattenToString())
    }

    private fun setupQActionSpinner() {
        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedAction = prefs.getString("q_key_action", "default") ?: "default"

        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.q_action_default) to "default")
        items.add(getString(R.string.q_action_back) to "action_back")
        items.add(getString(R.string.q_action_home) to "action_home")
        items.add(getString(R.string.q_action_play_pause) to "action_play_pause")
        items.add(getString(R.string.q_action_recents) to "action_recents")
        items.add(getString(R.string.q_action_settings) to "action_settings")
        
        val customIntentText = if (savedAction.startsWith("intent:")) {
            "Intent: " + savedAction.substringAfter("intent:")
        } else {
            getString(R.string.q_action_custom_intent)
        }
        items.add(customIntentText to "intent_custom")

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<Pair<String, String>>()
        for (app in apps) {
            val label = app.loadLabel(pm).toString()
            val pkg = app.activityInfo.packageName
            appList.add(label to "app:$pkg")
        }
        appList.sortBy { it.first.lowercase() }
        items.addAll(appList)

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items.map { it.first }) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.setTextColor(Color.WHITE)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.spinner_dropdown_item_bg)
                    val density = resources.displayMetrics.density
                    val padHoriz = (16 * density).toInt()
                    val padVert = (12 * density).toInt()
                    setPadding(padHoriz, padVert, padHoriz, padVert)
                }
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQAction.adapter = adapter

        val index = items.indexOfFirst { it.second == savedAction }
        if (index >= 0) {
            spinnerQAction.setSelection(index)
        } else if (savedAction.startsWith("intent:")) {
            spinnerQAction.setSelection(6)
        } else {
            spinnerQAction.setSelection(0)
        }

        spinnerQAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isFirstCall = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isFirstCall) {
                    isFirstCall = false
                    return
                }
                val selectedAction = items[position].second
                if (selectedAction == "intent_custom") {
                    val editText = EditText(this@MainActivity).apply {
                        hint = getString(R.string.q_action_custom_intent_hint)
                    }
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.q_action_intent_dialog_title))
                        .setMessage(getString(R.string.q_action_intent_dialog_msg))
                        .setView(editText)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val intentStr = editText.text.toString()
                            if (intentStr.isNotBlank()) {
                                prefs.edit { putString("q_key_action", "intent:$intentStr") }
                                setupQActionSpinner()
                            } else {
                                setupQActionSpinner()
                            }
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ -> setupQActionSpinner() }
                        .setOnCancelListener { setupQActionSpinner() }
                        .show()
                } else {
                    prefs.edit { putString("q_key_action", selectedAction) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isImeDefault(): Boolean {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return defaultIme?.contains(packageName) == true
    }

    private fun setupCatcherMode() {
        val btnCatchKey = findViewById<Button>(R.id.btnCatchKey)
        btnCatchKey.setOnClickListener {
            KeyInterceptorService.isCatcherModeActive = true
            
            catcherDialog = AlertDialog.Builder(this)
                .setTitle(R.string.dialog_catch_key_title)
                .setMessage(R.string.dialog_catch_key_msg)
                .setNegativeButton(R.string.btn_cancel) { _, _ ->
                    KeyInterceptorService.isCatcherModeActive = false
                    Toast.makeText(this, R.string.toast_catcher_cancelled, Toast.LENGTH_SHORT).show()
                }
                .setOnCancelListener {
                    KeyInterceptorService.isCatcherModeActive = false
                }
                .show()
        }
        refreshRemapsList()
    }

    private fun refreshRemapsList() {
        val remapsContainer = findViewById<LinearLayout>(R.id.remapsContainer)
        if (remapsContainer == null) return
        remapsContainer.removeAllViews()
        
        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("custom_remaps", "{}") ?: "{}"
        try {
            val obj = org.json.JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val keyCodeStr = keys.next()
                val action = obj.getString(keyCodeStr)
                val keyCode = keyCodeStr.toIntOrNull() ?: continue
                
                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 8, 0, 8)
                    gravity = Gravity.CENTER_VERTICAL
                }
                
                val textView = TextView(this).apply {
                    text = "${KeyEvent.keyCodeToString(keyCode)} -> $action"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                
                val deleteBtn = Button(this).apply {
                    text = "X"
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.btn_delete_selector)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.dialog_delete_remap_title)
                            .setMessage(R.string.dialog_delete_remap_msg)
                            .setPositiveButton(R.string.btn_delete) { _, _ ->
                                obj.remove(keyCodeStr)
                                prefs.edit().putString("custom_remaps", obj.toString()).apply()
                                refreshRemapsList()
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    }
                }
                itemLayout.addView(textView)
                itemLayout.addView(deleteBtn)
                remapsContainer.addView(itemLayout)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading custom remaps", e)
        }
    }

    private fun getAvailableActions(): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        items.add(getString(R.string.q_action_back) to "action_back")
        items.add(getString(R.string.q_action_home) to "action_home")
        items.add(getString(R.string.q_action_play_pause) to "action_play_pause")
        items.add(getString(R.string.q_action_recents) to "action_recents")
        items.add(getString(R.string.q_action_settings) to "action_settings")
        items.add(getString(R.string.q_action_custom_intent) to "intent_custom")
        
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<Pair<String, String>>()
        for (app in apps) {
            val label = app.loadLabel(pm).toString()
            val pkg = app.activityInfo.packageName
            appList.add(label to "app:$pkg")
        }
        appList.sortBy { it.first.lowercase() }
        items.addAll(appList)
        return items
    }

    private fun showActionPicker(keyCode: Int) {
        val actions = getAvailableActions()
        val names = actions.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_for_key, KeyEvent.keyCodeToString(keyCode)))
            .setItems(names) { _, which ->
                val selectedAction = actions[which].second
                if (selectedAction == "intent_custom") {
                    val editText = EditText(this).apply {
                        hint = getString(R.string.q_action_custom_intent_hint)
                        setTextColor(Color.WHITE)
                    }
                    AlertDialog.Builder(this)
                        .setTitle(R.string.q_action_intent_dialog_title)
                        .setMessage(R.string.q_action_intent_dialog_msg)
                        .setView(editText)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val intentUri = editText.text.toString().trim()
                            if (intentUri.isNotEmpty()) {
                                saveCustomRemap(keyCode, "intent:$intentUri")
                            }
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
                } else {
                    saveCustomRemap(keyCode, selectedAction)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun saveCustomRemap(keyCode: Int, action: String) {
        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("custom_remaps", "{}") ?: "{}"
        try {
            val obj = org.json.JSONObject(jsonStr)
            obj.put(keyCode.toString(), action)
            prefs.edit().putString("custom_remaps", obj.toString()).apply()
            refreshRemapsList()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving custom remap", e)
        }
    }
}
