package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var setupContainer: LinearLayout
    private lateinit var btnEnableIme: Button
    private lateinit var btnSelectIme: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var languagesContainer: LinearLayout
    private lateinit var btnImportLayout: Button

    private val handler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 2000)
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
        btnEnableIme = findViewById(R.id.btnEnableIme)
        btnSelectIme = findViewById(R.id.btnSelectIme)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        languagesContainer = findViewById(R.id.languagesContainer)
        btnImportLayout = findViewById(R.id.btnImportLayout)

        setupWizardButtons()
        setupAdbButtons()
        refreshLanguagesList()

        btnImportLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            importLauncher.launch(intent)
        }
    }

    private fun refreshLanguagesList() {
        languagesContainer.removeAllViews()
        val prefs = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val layouts = mutableListOf<Pair<String, String>>()

        try {
            val assetManager = this.assets
            assetManager.list("layouts")?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    assetManager.open("layouts/$fileName").use { inputStream ->
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        val layout = LayoutModel.fromJson(jsonString)
                        layouts.add(layout.id to getLocalizedName(layout))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading assets", e)
        }

        val externalDir = File(storageContext.filesDir, "layouts")
        if (externalDir.exists()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    try {
                        val layout = LayoutModel.fromJson(file.readText())
                        layouts.add(layout.id to getLocalizedName(layout))
                    } catch (e: Exception) {}
                }
            }
        }

        layouts.distinctBy { it.first }.forEach { (id, displayName) ->
            val cb = CheckBox(this).apply {
                text = displayName
                setTextColor(Color.WHITE)
                isChecked = prefs.getBoolean("lang_$id", true)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit { putBoolean("lang_$id", isChecked) }
                }
            }
            languagesContainer.addView(cb)
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

            val destFile = File(layoutsDir, "${layout.id}.json")
            FileOutputStream(destFile).use { it.write(jsonString.toByteArray()) }

            Toast.makeText(this, getString(R.string.import_success, layout.name), Toast.LENGTH_SHORT).show()
            refreshLanguagesList()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupWizardButtons() {
        btnEnableIme.setOnClickListener {
            if (hasSecureSettingsPermission()) {
                autoEnableIME()
            }
            try {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Не вдалося відкрити налаштування клавіатури", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        btnEnableAccessibility.setOnClickListener {
            if (hasSecureSettingsPermission()) {
                autoEnableAccessibility()
            } else {
                Toast.makeText(this, "Надайте дозвіл WRITE_SECURE_SETTINGS через ADB для кращої активації", Toast.LENGTH_LONG).show()
            }
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (ex: Exception) {
                    Toast.makeText(this, "Не вдалося відкрити налаштування", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                Toast.makeText(this, "Службу доступності активовано автоматично!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to auto-enable accessibility", e)
        }
    }

    private fun autoEnableIME() {
        try {
            val imeId = ComponentName(this, KeyInterceptorIME::class.java).flattenToString()
            val contentResolver = contentResolver
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            // Отримуємо список увімкнених IME через безпечний API
            val enabledImeIds = imm.enabledInputMethodList.map { it.id }

            if (!enabledImeIds.contains(imeId)) {
                val newEnabled = (enabledImeIds + imeId).joinToString(":")
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS, newEnabled)
            }

            // Set as Default
            Settings.Secure.putString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
            Toast.makeText(this, "Клавіатуру активовано та встановлено за замовчуванням!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to auto-enable IME", e)
        }
    }

    private fun setupAdbButtons() {
        val clickListener = View.OnClickListener { view ->
            val command = (view as Button).text.toString()
            copyToClipboard(command)
        }
        findViewById<Button>(R.id.cmdGrantSecure)?.setOnClickListener(clickListener)
        findViewById<Button>(R.id.cmdRestrictedSettings)?.setOnClickListener(clickListener)
        findViewById<Button>(R.id.cmdEnableService)?.setOnClickListener(clickListener)
        findViewById<Button>(R.id.cmdActivateAcc)?.setOnClickListener(clickListener)
        findViewById<Button>(R.id.cmdAllowOverlay)?.setOnClickListener(clickListener)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ADB", text))
        Toast.makeText(this, getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        handler.post(checkStatusRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(checkStatusRunnable)
    }

    private fun updateStatus() {
        val isServiceEnabled = isAccessibilityServiceEnabled(this, KeyInterceptorService::class.java)
        val isImeEnabled = isImeEnabled()
        val isImeSelected = isImeSelected()

        statusTextView.text = if (isServiceEnabled) getString(R.string.status_active) else getString(R.string.status_inactive)
        statusTextView.setTextColor(if (isServiceEnabled) Color.GREEN else Color.RED)

        // Динамічне блокування кнопок кроків, якщо вони вже виконані
        btnEnableIme.isEnabled = !isImeEnabled
        btnSelectIme.isEnabled = !isImeSelected
        btnEnableAccessibility.isEnabled = !isServiceEnabled

        // Приховувати весь блок налаштування, якщо все виконано
        if (isServiceEnabled && isImeEnabled && isImeSelected) {
            setupContainer.visibility = View.GONE
        } else {
            setupContainer.visibility = View.VISIBLE
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServicesSetting.split(':').any {
            ComponentName.unflattenFromString(it) == expectedComponentName
        }
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isImeSelected(): Boolean {
        val currentImeId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return currentImeId?.contains(packageName) == true
    }
}
