package com.conan.tradutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TradutorTest {
    private fun resp(vararg partes: String) =
        "[[" + partes.joinToString(",") { "[$it]" } + "],null,\"en\"]"

    @Test fun umaParte() {
        val r = traduzir("hello") { resp("\"olá\",\"hello\",null,null,1") }
        assertEquals(Resultado.Traducao("olá"), r)
    }

    @Test fun variasPartesConcatenadas() {
        val r = traduzir("a. b.") { resp("\"Olá. \",\"a. \"", "\"Mundo.\",\"b.\"") }
        assertEquals(Resultado.Traducao("Olá. Mundo."), r)
    }

    @Test fun partesVaziasIgnoradas() {
        val r = traduzir("x") { resp("\"um\",\"x\"", "\"\",\"\"", "null,null", "\"dois\",\"y\"") }
        assertEquals(Resultado.Traducao("umdois"), r)
    }

    @Test fun escapesJson() {
        val r = traduzir("x") { resp("\"a\\\"b\\n\\u00e9\\\\\",\"x\"") }
        assertEquals(Resultado.Traducao("a\"b\né\\"), r)
    }

    @Test fun acimaDoLimiteNaoChamaRede() {
        var chamou = false
        val r = traduzir("a".repeat(MAX_CHARS + 1)) { chamou = true; "" }
        assertEquals(Resultado.GrandeDemais(MAX_CHARS + 1), r)
        assertFalse(chamou)
    }

    @Test fun noLimiteTraduz() {
        val r = traduzir("a".repeat(MAX_CHARS)) { resp("\"ok\",\"a\"") }
        assertEquals(Resultado.Traducao("ok"), r)
    }

    @Test fun falhaDeRedeViraErro() {
        val r = traduzir("x") { throw IOException("sem rede") }
        assertTrue(r is Resultado.Erro)
        assertTrue((r as Resultado.Erro).motivo.contains("sem rede"))
    }

    private fun motivo(r: Resultado) = (r as Resultado.Erro).motivo

    @Test fun timeoutViraTempoEsgotado() {
        assertEquals("Tempo esgotado", motivo(traduzir("x") { throw SocketTimeoutException("Read timed out") }))
    }

    @Test fun semConexaoViraMensagemClara() {
        for (e in listOf(UnknownHostException("translate.googleapis.com"), ConnectException("refused"))) {
            assertEquals("Sem conexão com a internet", motivo(traduzir("x") { throw e }))
        }
    }

    @Test fun httpNao200ViraErroDoServidor() {
        assertEquals("Erro do servidor (HTTP 503)", motivo(traduzir("x") { throw ErroHttp(503) }))
    }

    @Test fun jsonInvalidoViraErro() {
        assertTrue(traduzir("x") { "<html>oops" } is Resultado.Erro)
        assertTrue(traduzir("x") { "" } is Resultado.Erro)
        assertTrue(traduzir("x") { "[[1,2]" } is Resultado.Erro)
    }

    @Test fun formatoInesperadoViraErro() {
        assertTrue(traduzir("x") { "{\"a\":1}" } is Resultado.Erro)
        assertTrue(traduzir("x") { "[]" } is Resultado.Erro)
        assertTrue(traduzir("x") { "[[1]]" } is Resultado.Erro)
        assertTrue(traduzir("x") { "[[[5]]]" } is Resultado.Erro)
        assertEquals("Resposta inesperada do Google Tradutor", motivo(traduzir("x") { "[]" }))
    }

    @Test fun urlComParametrosETextoCodificado() {
        var url = ""
        traduzir("olá mundo&x=1") { url = it; resp("\"a\",\"b\"") }
        assertTrue(url.startsWith("https://translate.googleapis.com/translate_a/single?"))
        for (p in listOf("client=gtx", "sl=auto", "tl=pt", "dt=t")) {
            assertTrue(p, url.contains(p))
        }
        assertTrue(url, url.endsWith("q=ol%C3%A1+mundo%26x%3D1"))
    }
}
