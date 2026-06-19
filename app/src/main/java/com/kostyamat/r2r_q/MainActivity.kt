package com.kostyamat.r2r_q

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var btnOpenSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adb)

        statusTextView = findViewById(R.id.statusTextView)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnOpenSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                statusTextView.text = "Не вдалося відкрити налаштування"
            }
        }

        setupAdbButtons()
    }

    private fun setupAdbButtons() {
        val buttons = listOf(
            R.id.cmdEnableService,
            R.id.cmdActivateAccessibility,
            R.id.cmdActiveBucket,
            R.id.cmdAllowRestricted
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
        Toast.makeText(this, "Команду скопійовано!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, KeyInterceptorService::class.java)
        if (isEnabled) {
            statusTextView.text = "Статус: АКТИВНО (служба працює)"
            statusTextView.setTextColor(Color.GREEN)
        } else {
            statusTextView.text = "Статус: НЕАКТИВНО"
            statusTextView.setTextColor(Color.RED)
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
}
