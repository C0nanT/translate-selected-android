package com.conan.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.provider.Settings
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

// floating bubble on the screen edge: copy in the reader + tap here = translate.
// needs "Sobrepor a outros apps" (draw over other apps); runs in the foreground (ongoing notification).
class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISABLE) { stopSelf(); return START_NOT_STICKY }
        // without "Sobrepor a outros apps" the window cannot be added: stop silently, with no error or restart request
        goForeground() // startForegroundService requires startForeground before stopping, otherwise ForegroundServiceDidNotStartInTimeException
        if (!Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
            else @Suppress("DEPRECATION") stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun goForeground() {
        val disable = PendingIntent.getService(this, 0,
            Intent(this, BubbleService::class.java).setAction(ACTION_DISABLE), PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Bolha", NotificationManager.IMPORTANCE_MIN))
            Notification.Builder(this, CHANNEL)
        } else @Suppress("DEPRECATION") Notification.Builder(this)
        val n = b.setSmallIcon(R.drawable.ic_translate)
            .setContentTitle("Bolha do Tradutor ligada")
            .setContentText("Toque para desligar")
            .setContentIntent(disable)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_translate), "Desligar", disable).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun showBubble() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val side = dp(48)
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(side, side, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            val screen = resources.displayMetrics
            val saved = getSharedPreferences(PREFS, MODE_PRIVATE)
            val p = onScreen(saved.getInt("x", screen.widthPixels - side), saved.getInt("y", screen.heightPixels / 3), side)
            x = p.x
            y = p.y
        }
        val v = TextView(this).apply {
            text = "文"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(accentColor()) }
            alpha = 0.85f
        }
        // dragging moves (beyond the system slop); a short tap translates; holding still disables with a vibration
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val disableOnHold = Runnable {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            stopSelf()
        }
        var x0 = 0; var y0 = 0; var rx = 0f; var ry = 0f; var dragged = false
        v.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    x0 = lp.x; y0 = lp.y; rx = ev.rawX; ry = ev.rawY; dragged = false
                    v.postDelayed(disableOnHold, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - rx; val dy = ev.rawY - ry
                    if (!dragged && (abs(dx) > slop || abs(dy) > slop)) {
                        dragged = true
                        v.removeCallbacks(disableOnHold)
                    }
                    if (dragged) {
                        val p = onScreen(x0 + dx.toInt(), y0 + dy.toInt(), side)
                        lp.x = p.x; lp.y = p.y
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(disableOnHold)
                    if (dragged) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    } else translateCopied()
                }
                MotionEvent.ACTION_CANCEL -> v.removeCallbacks(disableOnHold)
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
        stateChanged(true)
    }

    // the Quick Settings tile follows the state
    private fun stateChanged(on: Boolean) {
        enabled = on
        if (Build.VERSION.SDK_INT >= 24) TileService.requestListeningState(this, ComponentName(this, BubbleTile::class.java))
    }

    // bubble position (side x side) without leaving the screen
    private fun onScreen(x: Int, y: Int, side: Int): Position {
        val screen = resources.displayMetrics
        return clampPosition(Position(x, y), Size(screen.widthPixels, screen.heightPixels), Size(side, side), centeredX = false)
    }

    private val taps = Taps(REPEATED_TAP_MS)

    private fun translateCopied() {
        if (!taps.accept(SystemClock.uptimeMillis())) return  // repeated tap: a single popup
        startActivity(Intent(this, ClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        bubble?.let { wm.removeView(it) }
        bubble = null
        stateChanged(false)
        super.onDestroy()
    }

    companion object {
        // bubble on screen right now (the process dies with the service, so it starts false)
        var enabled = false
            private set

        // without "Sobrepor a outros apps" the service stops by itself: callers check first
        fun enable(ctx: Context) {
            val i = Intent(ctx, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun disable(ctx: Context) {
            ctx.stopService(Intent(ctx, BubbleService::class.java))
        }

        const val ACTION_DISABLE = "com.conan.translator.DISABLE"
        private const val CHANNEL = "bubble"
        private const val PREFS = "bubble"  // last position (x, y of the top-left corner)
    }
}
