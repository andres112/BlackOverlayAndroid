package com.andres.blackoverlay

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class BlackOverlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(this)) {
            openMainActivity()
            return
        }

        if (isOverlayActive()) {
            updateTile()
            return
        }

        try {
            ContextCompat.startForegroundService(this, Intent(this, BlackOverlayService::class.java))
        } catch (_: RuntimeException) {
            openMainActivity()
        }
        updateTile()
    }

    private fun isOverlayActive(): Boolean =
        getSharedPreferences(BlackOverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(BlackOverlayService.KEY_OVERLAY_ACTIVE, false)

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.label = getString(R.string.quick_settings_tile_label)
            tile.state = if (isOverlayActive()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                20,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
