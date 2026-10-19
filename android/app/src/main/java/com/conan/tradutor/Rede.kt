package com.conan.tradutor

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

// busca HTTP real injetada no nucleo; qualquer falha sai como NetworkError (Http em nao-200).
// HttpURLConnection ignora interrupt de thread: cancelar() desconecta a conexao em curso
// para a leitura falhar na hora e a thread unica do executor ficar livre.
class BuscaHttp {
    @Volatile private var conexao: HttpURLConnection? = null
    @Volatile private var cancelada = false

    fun buscar(url: String): String {
        if (cancelada) throw IOException("cancelado")
        val c = URL(url).openConnection() as HttpURLConnection
        conexao = c
        try {
            if (cancelada) throw IOException("cancelado")  // cancelar() pode ter corrido antes de conexao ser guardada
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

    // pode ser chamado da thread principal: o disconnect vai numa thread propria (rede fora da principal)
    fun cancelar() {
        cancelada = true
        val c = conexao ?: return
        Thread { c.disconnect() }.start()
    }
}
