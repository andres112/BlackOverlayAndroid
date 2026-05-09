package com.andres.blackoverlay

object OverlaySettings {
    const val PREFS_NAME = "black_overlay_state"

    const val KEY_OVERLAY_ACTIVE = "overlay_active"
    const val KEY_UNLOCK_TAP_COUNT = "unlock_tap_count"
    const val KEY_QUICK_SETTINGS_TILE_ADDED = "quick_settings_tile_added"

    const val MIN_UNLOCK_TAP_COUNT = 3
    const val MAX_UNLOCK_TAP_COUNT = 7
    const val DEFAULT_UNLOCK_TAP_COUNT = 3

    val unlockTapCountOptions: List<Int>
        get() = (MIN_UNLOCK_TAP_COUNT..MAX_UNLOCK_TAP_COUNT).toList()
}
