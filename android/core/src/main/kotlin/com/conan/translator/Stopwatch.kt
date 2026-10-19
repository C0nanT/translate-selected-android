package com.conan.translator

// Time left on the popup. No Android: the "now" (ms) comes in on each call.
// States: idle (not started, e.g. loading), running, paused (finger down).
class Stopwatch(private val totalMs: Long) {
    var remainingMs = totalMs
        private set
    var started = false            // has been started
        private set
    private var paused = false
    private var last = 0L

    val finished get() = started && remainingMs <= 0
    val fraction get() = (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

    // (re)starts with full time; if the finger is down, it only runs once released
    fun start(now: Long) {
        remainingMs = totalMs
        started = true
        last = now
    }

    // back to idle (loading): does not count down until the next start.
    // the finger down (paused) still applies
    fun stop() {
        remainingMs = totalMs
        started = false
    }

    fun pause(now: Long) {
        elapse(now)
        paused = true
    }

    fun resume(now: Long) {
        if (!paused) return
        paused = false
        last = now
    }

    // subtracts the time elapsed since the last call and returns what remains
    fun elapse(now: Long): Long {
        if (started && !paused) {
            remainingMs = maxOf(0L, remainingMs - (now - last))
            last = now
        }
        return remainingMs
    }
}
