package com.andres.blackoverlay

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = OverlaySettingsRepository(application)

    val unlockTapCountOptions: List<Int> = OverlaySettings.unlockTapCountOptions

    fun getUnlockTapCount(): Int =
        settingsRepository.getUnlockTapCount()

    fun setUnlockTapCount(tapCount: Int) {
        settingsRepository.setUnlockTapCount(tapCount)
    }
}
