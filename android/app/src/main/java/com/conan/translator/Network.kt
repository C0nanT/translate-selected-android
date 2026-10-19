package com.conan.translator

import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

// Converts HttpURLConnection/java.net exceptions to the common NetworkError type of the core.
fun convertException(e: Exception): NetworkError = when (e) {
    is NetworkError -> e
    is SocketTimeoutException -> NetworkError.Timeout
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> NetworkError.NoConnection
    else -> NetworkError.Other(e.message ?: e.javaClass.simpleName)
}

// real HTTP fetch injected into the core; any failure comes out as NetworkError (Http on non-200).
// HttpURLConnection ignores thread interrupt: cancel() disconnects the ongoing connection
// so the read fails right away and the executor's single thread is freed.
class HttpFetcher {
    @Volatile private var connection: HttpURLConnection? = null
    @Volatile private var cancelled = false

    fun fetch(url: String): String {
        if (cancelled) throw IOException("cancelled")
        val c = URL(url).openConnection() as HttpURLConnection
        connection = c
        try {
            if (cancelled) throw IOException("cancelled")  // cancel() may have run before the connection was stored
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            if (c.responseCode != 200) throw NetworkError.Http(c.responseCode)
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            throw convertException(e)
        } finally {
            c.disconnect()
        }
    }

    // may be called from the main thread: the disconnect runs on its own thread (network off the main thread)
    fun cancel() {
        cancelled = true
        val c = connection ?: return
        Thread { c.disconnect() }.start()
    }
}
