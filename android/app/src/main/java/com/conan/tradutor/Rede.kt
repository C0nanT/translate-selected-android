package com.conan.tradutor

import java.net.HttpURLConnection
import java.net.URL

// busca HTTP real injetada no nucleo; lanca excecao em qualquer falha
fun buscarHttp(url: String): String {
    val c = URL(url).openConnection() as HttpURLConnection
    try {
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode}")
        return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        c.disconnect()
    }
}
