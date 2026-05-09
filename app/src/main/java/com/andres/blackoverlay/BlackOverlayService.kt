package com.andres.blackoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class BlackOverlayService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var overlayView: View? = null
    private var unlockTapCount = 0
    private var lastUnlockTapAt = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The permission can be revoked while the app is installed, so re-check inside the service.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Foreground service notification must be active before doing long-running overlay work.
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlayIfNeeded()
        setOverlayActive(true)
        return START_STICKY
    }

    private fun showOverlayIfNeeded() {
        // Repeated starts should keep the existing overlay instead of adding duplicate windows.
        if (overlayView != null) return

        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            setOnTouchListener { _, event ->
                handleOverlayTouch(event)
                // Consume all touches so normal apps underneath do not receive them.
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // TYPE_APPLICATION_OVERLAY is the public app-level overlay window type for Android O+.
        windowManager.addView(overlayView, params)
    }

    private fun handleOverlayTouch(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_UP) return

        val now = SystemClock.uptimeMillis()
        if (now - lastUnlockTapAt > UNLOCK_TAP_TIMEOUT_MS) {
            unlockTapCount = 0
        }
        lastUnlockTapAt = now

        unlockTapCount += 1
        if (unlockTapCount >= getRequiredUnlockTapCount()) {
            unlockTapCount = 0
            lastUnlockTapAt = 0L
            launchUnlock()
        }
    }

    private fun getRequiredUnlockTapCount(): Int =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_UNLOCK_TAP_COUNT, DEFAULT_UNLOCK_TAP_COUNT)
            .coerceIn(MIN_UNLOCK_TAP_COUNT, MAX_UNLOCK_TAP_COUNT)

    private fun launchUnlock() {
        val intent = Intent(this, UnlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Low importance keeps the required foreground notification visible without sound/vibration.
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_stat_overlay)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        removeOverlaySafely()
        setOverlayActive(false)
        super.onDestroy()
    }

    private fun removeOverlaySafely() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The system may already have detached the view during service teardown.
        }
        overlayView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setOverlayActive(active: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OVERLAY_ACTIVE, active)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "black_overlay_state"
        const val KEY_OVERLAY_ACTIVE = "overlay_active"
        const val KEY_UNLOCK_TAP_COUNT = "unlock_tap_count"
        const val MIN_UNLOCK_TAP_COUNT = 3
        const val MAX_UNLOCK_TAP_COUNT = 7
        const val DEFAULT_UNLOCK_TAP_COUNT = 3

        private const val CHANNEL_ID = "black_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private val UNLOCK_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout().toLong() * 2
    }
}
