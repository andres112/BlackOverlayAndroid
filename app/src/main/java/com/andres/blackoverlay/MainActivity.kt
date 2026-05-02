package com.andres.blackoverlay

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var permissionStatus: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionStatus = findViewById(R.id.textPermissionStatus)
        findViewById<Button>(R.id.buttonRequestOverlay).setOnClickListener { requestOverlayPermission() }
        findViewById<Button>(R.id.buttonStartOverlay).setOnClickListener { startOverlay() }
        findViewById<Button>(R.id.buttonStopOverlay).setOnClickListener { stopOverlay() }
    }

    override fun onResume() {
        super.onResume()
        // Overlay permission is granted in Settings, so refresh whenever the user returns.
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        permissionStatus.text = getString(
            R.string.permission_status_template,
            if (overlayGranted) getString(R.string.granted) else getString(R.string.missing),
            if (notificationGranted) getString(R.string.granted) else getString(R.string.missing)
        )
    }

    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            updatePermissionStatus()
            return
        }

        // SYSTEM_ALERT_WINDOW is not a runtime permission; Android exposes it through settings.
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun maybeRequestNotificationPermission() {
        // Android 13+ requires a runtime notification permission before notifications are visible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // The service still starts if the user declines notifications, but Android may hide alerts.
        maybeRequestNotificationPermission()
        ContextCompat.startForegroundService(this, Intent(this, BlackOverlayService::class.java))
    }

    private fun stopOverlay() {
        stopService(Intent(this, BlackOverlayService::class.java))
    }
}
