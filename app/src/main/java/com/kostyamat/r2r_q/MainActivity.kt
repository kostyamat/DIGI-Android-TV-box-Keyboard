package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var wizardLayout: LinearLayout
    private lateinit var btnEnableIme: Button
    private lateinit var btnSelectIme: Button
    
    private lateinit var cbEn: CheckBox
    private lateinit var cbEs: CheckBox
    private lateinit var cbUa: CheckBox
    private lateinit var cbRu: CheckBox

    private val handler = Handler(Looper.getMainLooper())
    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adb)

        statusTextView = findViewById(R.id.statusTextView)
        wizardLayout = findViewById(R.id.wizardLayout)
        btnEnableIme = findViewById(R.id.btnEnableIme)
        btnSelectIme = findViewById(R.id.btnSelectIme)
        
        cbEn = findViewById(R.id.cbEn)
        cbEs = findViewById(R.id.cbEs)
        cbUa = findViewById(R.id.cbUa)
        cbRu = findViewById(R.id.cbRu)

        setupCheckboxes()
        setupAdbButtons()
        setupWizardButtons()
    }

    private fun setupCheckboxes() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        // Spanish enabled by default as requested
        cbEn.isChecked = prefs.getBoolean("lang_en", true)
        cbEs.isChecked = prefs.getBoolean("lang_es", true)
        cbUa.isChecked = prefs.getBoolean("lang_ua", true)
        cbRu.isChecked = prefs.getBoolean("lang_ru", true)

        val listener = { _: View ->
            prefs.edit().apply {
                putBoolean("lang_en", cbEn.isChecked)
                putBoolean("lang_es", cbEs.isChecked)
                putBoolean("lang_ua", cbUa.isChecked)
                putBoolean("lang_ru", cbRu.isChecked)
                apply()
            }
            // Notify service if it's running (could use broadcast or shared prefs)
            sendBroadcast(Intent("com.kostyamat.r2r_q.SETTINGS_CHANGED"))
        }

        cbEn.setOnClickListener(listener)
        cbEs.setOnClickListener(listener)
        cbUa.setOnClickListener(listener)
        cbRu.setOnClickListener(listener)
    }

    private fun setupWizardButtons() {
        btnEnableIme.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    private fun setupAdbButtons() {
        val buttons = listOf(
            R.id.cmdEnableService,
            R.id.cmdActivateAccessibility,
            R.id.cmdAllowOverlay
        )

        buttons.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { view ->
                val command = (view as Button).text.toString()
                copyToClipboard(command)
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Command", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
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

        if (isServiceEnabled) {
            statusTextView.text = getString(R.string.status_active)
            statusTextView.setTextColor(Color.GREEN)
            
            // If service is active, show IME wizard if not fully configured
            if (!isImeEnabled || !isImeSelected) {
                wizardLayout.visibility = View.VISIBLE
                btnEnableIme.isEnabled = !isImeEnabled
                btnSelectIme.isEnabled = isImeEnabled && !isImeSelected
            } else {
                wizardLayout.visibility = View.GONE
            }
        } else {
            statusTextView.text = getString(R.string.status_inactive)
            statusTextView.setTextColor(Color.RED)
            wizardLayout.visibility = View.GONE
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImeList = imm.enabledInputMethodList
        return enabledImeList.any { it.packageName == packageName }
    }

    private fun isImeSelected(): Boolean {
        val currentImeId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return currentImeId?.contains(packageName) == true
    }
}
