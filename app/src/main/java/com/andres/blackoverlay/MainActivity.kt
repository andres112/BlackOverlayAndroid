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
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var startOverlayButton: Button
    private lateinit var overlayPermissionSwitch: SwitchCompat
    private lateinit var notificationPermissionSwitch: SwitchCompat
    private lateinit var quickSettingsTileRow: View
    private lateinit var quickSettingsTileCheck: CheckBox
    private lateinit var unlockTapCountSpinner: Spinner

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateSettingsStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        startOverlayButton = findViewById(R.id.buttonStartOverlay)
        overlayPermissionSwitch = findViewById(R.id.switchOverlayPermission)
        notificationPermissionSwitch = findViewById(R.id.switchNotificationPermission)
        quickSettingsTileRow = findViewById(R.id.rowQuickSettingsTile)
        quickSettingsTileCheck = findViewById(R.id.checkQuickSettingsTile)
        unlockTapCountSpinner = findViewById(R.id.spinnerUnlockTapCount)

        overlayPermissionSwitch.setOnClickListener { requestOverlayPermission() }
        notificationPermissionSwitch.setOnClickListener { maybeRequestNotificationPermission() }
        quickSettingsTileCheck.setOnClickListener { requestAddQuickSettingsTile() }
        quickSettingsTileRow.setOnClickListener { requestAddQuickSettingsTile() }
        startOverlayButton.setOnClickListener { startOverlay() }
        findViewById<Button>(R.id.buttonStopOverlay).setOnClickListener { stopOverlay() }

        configureUnlockTapCountSpinner()
    }

    override fun onResume() {
        super.onResume()
        // Overlay permission is granted in Settings, so refresh whenever the user returns.
        updateSettingsStatus()
    }

    private fun updateSettingsStatus() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val notificationGranted = isNotificationPermissionGranted()
        val tileAdded = viewModel.isQuickSettingsTileAdded()

        overlayPermissionSwitch.isChecked = overlayGranted
        overlayPermissionSwitch.isEnabled = !overlayGranted
        notificationPermissionSwitch.isChecked = notificationGranted
        notificationPermissionSwitch.isEnabled = !notificationGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        quickSettingsTileCheck.isChecked = tileAdded
        quickSettingsTileCheck.isEnabled = !tileAdded
        quickSettingsTileRow.isEnabled = !tileAdded
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
            updateSettingsStatus()
            return
        }
        overlayPermissionSwitch.isChecked = false

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
            val granted = isNotificationPermissionGranted()
            if (!granted) {
                notificationPermissionSwitch.isChecked = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun isNotificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            statusBarManager.requestAddTileService(
                ComponentName(this, BlackOverlayTileService::class.java),
                getString(R.string.quick_settings_tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_overlay),
                mainExecutor
            ) { result ->
                if (
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                ) {
                    viewModel.setQuickSettingsTileAdded(true)
                }
                updateSettingsStatus()
            }
        } else {
            quickSettingsTileCheck.isChecked = false
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
