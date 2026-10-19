package com.conan.translator

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.concurrent.Future
import kotlin.math.abs

// popup anchored at the bottom, over the source app. Tapping outside closes it (theme).
// hides by itself after HIDE_AFTER_S; a finger resting on it pauses the time. No buttons.
// hold and drag moves it; the next one opens in the same place (saved in prefs).
// error: red background with the reason (same as on Linux).
// each entry point (selection, share...) only says where the text comes from.
//
// Every entry point declared in AndroidManifest needs:
//  - android:theme="@style/Popup" (translucent window, tapping outside closes)
//  - android:configChanges with orientation|screenSize|smallestScreenSize|screenLayout|
//    keyboard|keyboardHidden|navigation and uiMode: rotating or switching light/dark does not
//    recreate the screen (otherwise it redoes the translation and restarts the time)
//  - android:excludeFromRecents="true"
abstract class PopupActivity : Activity() {

    protected abstract fun textFrom(intent: Intent): String?

    // entry point that can only read the text while the window has focus (clipboard)
    protected open val readWithFocus = false
    // entry point that shows a red warning when there is nothing to translate (bubble); the others close silently
    protected open val warnWhenEmpty = false
    private var started = false
    private var request: Future<*>? = null
    private val fetcher = HttpFetcher()

    private lateinit var label: TextView
    private lateinit var bar: TimeBar
    private val panel = GradientDrawable()
    private val handler = Handler(Looper.getMainLooper())
    private val stopwatch = Stopwatch(HIDE_MS)
    private var paused = false
    private var inError = false

    // drag: only arms after holding still (dragging right away scrolls the text)
    private var dragging = false
    private var fingerX = 0f
    private var fingerY = 0f
    private val armDrag = Runnable { arm() }

    private fun arm() {
        dragging = true
        window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        // scrolling stops following the finger; the rest of the gesture belongs to the drag
        val now = SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, fingerX, fingerY, 0)
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }

    // only runs while the time is running; pausing removes the tick from the queue
    private val tick = object : Runnable {
        override fun run() {
            stopwatch.elapse(SystemClock.uptimeMillis())
            if (stopwatch.finished) { finish(); return }
            bar.fraction = stopwatch.fraction
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = dp(16)
        label = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(pad, pad, pad, pad)
        }
        bar = TimeBar(this, accentColor())
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(CappedScroll(this@PopupActivity).apply { addView(label) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        }
        setContentView(root)

        // own background (rounded corners) so it can turn red on error
        panel.cornerRadius = dp(12).toFloat()
        window.setBackgroundDrawable(InsetDrawable(panel, dp(8)))
        // bottom/center gravity: x = offset from the center, y = distance from the bottom edge
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        moveTo(prefs.getInt("x", 0), prefs.getInt("y", 0))
        fitWidth()
        // the real size only exists after layout: re-clamp the saved position on every size change
        window.decorView.addOnLayoutChangeListener { _, l, t, r, b, el, et, er, eb ->
            if (r - l != er - el || b - t != eb - et) {
                val lp = window.attributes
                moveTo(lp.x, lp.y)
            }
        }

        showLoading()
        if (!readWithFocus) begin(textFrom(intent))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && readWithFocus && !started) begin(textFrom(intent))
    }

    private fun begin(raw: String?) {
        started = true
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) { noText(NOTHING_COPIED); return }
        // network off the main thread. the thread only keeps a weak reference:
        // a closed popup is not held in memory and the result is discarded.
        val ref = WeakReference(this)
        request = Requests.executor.submit {
            val r = translate(text, fetcher::fetch)
            Handler(Looper.getMainLooper()).post { ref.get()?.render(r) }
        }
    }

    private fun noText(warning: String) {
        if (warnWhenEmpty) show(warning, error = true) else finish()
    }

    private fun render(r: Result) {
        if (isFinishing || isDestroyed) return
        when (r) {
            is Result.Translation -> show(r.text)
            is Result.TooLarge -> show("Texto grande demais (${r.n} caracteres)", error = true)
            Result.Empty -> noText(NOTHING_COPIED)
            Result.NoLetters -> noText("Nada para traduzir")
            is Result.Failure -> show(r.reason, error = true)
        }
    }

    // width: ~POPUP_CHARS characters, never wider than the screen (redone on rotation)
    private fun fitWidth() {
        val screen = resources.displayMetrics.widthPixels
        val wanted = label.paint.measureText("x".repeat(POPUP_CHARS)).toInt() + 2 * dp(16) + 2 * dp(8)
        window.setLayout(minOf(wanted, screen), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // configChanges in the manifest: rotating/resizing/switching theme does not recreate nor redo the request
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        paint()
        fitWidth()
        val lp = window.attributes
        moveTo(lp.x, lp.y)  // screen changed -> limits changed
    }

    // without leaving the screen (clampPosition). before measuring, the size is 0: wide limit,
    // which is why the first layout re-clamps (see onCreate)
    private fun moveTo(x: Int, y: Int) {
        val screen = resources.displayMetrics
        val v = window.decorView
        val lp = window.attributes
        val p = clampPosition(Position(x, y), Size(screen.widthPixels, screen.heightPixels),
            Size(v.width, v.height), centeredX = true)
        if (lp.x == p.x && lp.y == p.y) return
        lp.x = p.x
        lp.y = p.y
        window.attributes = lp
    }

    // "Traduzindo…": the time stays stopped and the bar empty until the result or error
    private fun showLoading() {
        label.text = "Traduzindo…"
        inError = false
        paint()
        stopwatch.stop()
        handler.removeCallbacks(tick)
        bar.fraction = 0f
    }

    // sets the content and restarts the time. error = red background, white text (in both themes).
    fun show(text: String, error: Boolean = false) {
        label.text = text
        inError = error
        paint()
        restartTime()
    }

    // colors of the current state (normal or error) in the current theme; does not touch the text or the time
    private fun paint() {
        panel.setColor(if (inError) errorColor() else backgroundColor())
        label.setTextColor(if (inError) Color.WHITE else textColor())
        bar.color = if (inError) Color.WHITE else accentColor()
    }

    private fun restartTime() {
        stopwatch.start(SystemClock.uptimeMillis())
        bar.fraction = 1f
        handler.removeCallbacks(tick)
        if (!paused) handler.postDelayed(tick, TICK_MS)
    }

    // a finger on the popup stops the tick; releasing resumes where it stopped.
    // holding still arms the drag; from then on the finger moves the window.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!paused) {
                    paused = true
                    stopwatch.pause(SystemClock.uptimeMillis())
                    handler.removeCallbacks(tick)
                }
                fingerX = ev.rawX; fingerY = ev.rawY
                handler.postDelayed(armDrag, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                // rawX/rawY: the window moves with the finger, local coordinates are no use
                val lp = window.attributes
                moveTo(lp.x + (ev.rawX - fingerX).toInt(), lp.y - (ev.rawY - fingerY).toInt())
                fingerX = ev.rawX; fingerY = ev.rawY
                return true
            } else {
                val slop = ViewConfiguration.get(this).scaledTouchSlop
                if (abs(ev.rawX - fingerX) > slop || abs(ev.rawY - fingerY) > slop) handler.removeCallbacks(armDrag)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(armDrag)
                if (paused) {
                    paused = false
                    stopwatch.resume(SystemClock.uptimeMillis())
                    if (stopwatch.started) handler.postDelayed(tick, TICK_MS)
                }
                if (dragging) {
                    dragging = false
                    val lp = window.attributes
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    return true  // children already got CANCEL
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(armDrag)
        fetcher.cancel()  // HttpURLConnection ignores interrupt: drops the ongoing connection
        request?.cancel(true)  // closed early: discards the request (removes it from the queue or interrupts it)
        super.onDestroy()
    }

    private fun textColor() = if (isNight()) Color.rgb(0xE6, 0xE6, 0xE6) else Color.rgb(0x20, 0x20, 0x20)
    private fun backgroundColor() = if (isNight()) Color.rgb(0x2D, 0x2D, 0x2D) else Color.WHITE
    private fun errorColor() = if (isNight()) Color.rgb(0x8C, 0x1D, 0x18) else Color.rgb(0xB3, 0x26, 0x1E)

    // thin bar that shrinks from left to right (anchored on the right)
    private class TimeBar(ctx: Activity, initial: Int) : View(ctx) {
        private val ink = Paint().apply { color = initial }
        var color: Int
            get() = ink.color
            set(v) { if (v != ink.color) { ink.color = v; invalidate() } }
        var fraction = 1f
            set(v) { if (v != field) { field = v; invalidate() } }
        override fun onDraw(c: Canvas) {
            c.drawRect(width * (1 - fraction), 0f, width.toFloat(), height.toFloat(), ink)
        }
    }

    // scroll up to half the screen height; beyond that the text scrolls
    private class CappedScroll(ctx: Activity) : ScrollView(ctx) {
        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            val cap = resources.displayMetrics.heightPixels / 2
            val available = MeasureSpec.getSize(heightSpec)
            val max = if (MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED) cap else minOf(available, cap)
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST))
        }
    }

    private companion object {
        const val NOTHING_COPIED = "Nada copiado"
        const val TICK_MS = 50L
        const val PREFS = "popup"  // dragged position (x, y of the gravity)
        const val HIDE_MS = HIDE_AFTER_S * 1000L  // the only place that converts to ms
    }
}
