package com.conan.translator

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ignores taps less than minMs after the last accepted one. No Android: the "now" (ms) comes in on each call.
class Taps(private val minMs: Long) {
    private var last: Long? = null

    fun accept(now: Long): Boolean {
        val l = last
        if (l != null && now - l < minMs) return false
        last = now
        return true
    }
}

// a single executor, shared by all popups: requests do not pile up threads
object Requests {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
}
