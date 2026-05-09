package com.andres.blackoverlay

import android.content.Context
import android.content.SharedPreferences

class OverlaySettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        OverlaySettings.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isOverlayActive(): Boolean =
        prefs.getBoolean(OverlaySettings.KEY_OVERLAY_ACTIVE, false)

    fun setOverlayActive(active: Boolean) {
        prefs.edit()
            .putBoolean(OverlaySettings.KEY_OVERLAY_ACTIVE, active)
            .apply()
    }

    fun getUnlockTapCount(): Int =
        prefs.getInt(
            OverlaySettings.KEY_UNLOCK_TAP_COUNT,
            OverlaySettings.DEFAULT_UNLOCK_TAP_COUNT
        ).coerceIn(
            OverlaySettings.MIN_UNLOCK_TAP_COUNT,
            OverlaySettings.MAX_UNLOCK_TAP_COUNT
        )

    fun setUnlockTapCount(tapCount: Int) {
        prefs.edit()
            .putInt(
                OverlaySettings.KEY_UNLOCK_TAP_COUNT,
                tapCount.coerceIn(
                    OverlaySettings.MIN_UNLOCK_TAP_COUNT,
                    OverlaySettings.MAX_UNLOCK_TAP_COUNT
                )
            )
            .apply()
    }

    fun isQuickSettingsTileAdded(): Boolean =
        prefs.getBoolean(OverlaySettings.KEY_QUICK_SETTINGS_TILE_ADDED, false)

    fun setQuickSettingsTileAdded(added: Boolean) {
        prefs.edit()
            .putBoolean(OverlaySettings.KEY_QUICK_SETTINGS_TILE_ADDED, added)
            .apply()
    }
}
