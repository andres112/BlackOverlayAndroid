package com.andres.blackoverlay

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var permissionStatus: TextView
    private lateinit var requestOverlayButton: Button
    private lateinit var startOverlayButton: Button
    private lateinit var unlockTapCountSpinner: Spinner

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        permissionStatus = findViewById(R.id.textPermissionStatus)
        requestOverlayButton = findViewById(R.id.buttonRequestOverlay)
        startOverlayButton = findViewById(R.id.buttonStartOverlay)
        unlockTapCountSpinner = findViewById(R.id.spinnerUnlockTapCount)

        requestOverlayButton.setOnClickListener { requestOverlayPermission() }
        findViewById<Button>(R.id.buttonAddQuickSettingsTile).setOnClickListener {
            requestAddQuickSettingsTile()
        }
        startOverlayButton.setOnClickListener { startOverlay() }
        findViewById<Button>(R.id.buttonStopOverlay).setOnClickListener { stopOverlay() }

        configureUnlockTapCountSpinner()
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
        requestOverlayButton.isEnabled = !overlayGranted
        startOverlayButton.isEnabled = overlayGranted
    }

    private fun configureUnlockTapCountSpinner() {
        val options = viewModel.unlockTapCountOptions
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_unlock_tap_count,
            options
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_unlock_tap_count_dropdown)
        unlockTapCountSpinner.adapter = adapter
        val selectedIndex = options.indexOf(viewModel.getUnlockTapCount()).coerceAtLeast(0)
        unlockTapCountSpinner.setSelection(selectedIndex)

        unlockTapCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setUnlockTapCount(options[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
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

    private fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            statusBarManager.requestAddTileService(
                ComponentName(this, BlackOverlayTileService::class.java),
                getString(R.string.quick_settings_tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_overlay),
                mainExecutor
            ) {
                // Android owns the result UI; no app state needs to change here.
            }
        } else {
            Toast.makeText(
                this,
                R.string.quick_settings_tile_manual_add,
                Toast.LENGTH_LONG
            ).show()
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
