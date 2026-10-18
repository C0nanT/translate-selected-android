package com.conan.tradutor

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// busca HTTP real injetada no nucleo; lanca ErroHttp em nao-200, IOException em falha de rede.
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
            if (c.responseCode != 200) throw ErroHttp(c.responseCode)
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
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
