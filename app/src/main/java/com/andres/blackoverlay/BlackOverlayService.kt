package com.andres.blackoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class BlackOverlayService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // The permission can be revoked while the app is installed, so re-check inside the service.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Foreground service notification must be active before doing long-running overlay work.
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlayIfNeeded()
        return START_STICKY
    }

    private fun showOverlayIfNeeded() {
        // Repeated starts should keep the existing overlay instead of adding duplicate windows.
        if (overlayView != null) return

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                launchUnlock()
            }
        })

        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            isLongClickable = true
            setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                // Consume all touches so normal apps underneath do not receive them.
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
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
        val stopIntent = Intent(this, BlackOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            10,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_stat_overlay)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.stop_overlay),
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        removeOverlaySafely()
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

    companion object {
        private const val CHANNEL_ID = "black_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.andres.blackoverlay.action.STOP"
    }
}
