package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TradutorTest {
    private fun resp(vararg partes: String) =
        "[[" + partes.joinToString(",") { "[$it]" } + "],null,\"en\"]"

    @Test fun textoVazioNaoChamaRede() {
        var chamou = false
        assertEquals(Resultado.Vazio, traduzir("") { chamou = true; "" })
        assertEquals(Resultado.Vazio, traduzir("  \n \r\n\t ") { chamou = true; "" })
        assertFalse(chamou)
    }

    @Test fun semLetrasNaoChamaRede() {
        var chamou = false
        for (t in listOf("42", "—", "3,14 % + 7", "content://media/external/1"
            .filter { !it.isLetter() })) {
            assertEquals(Resultado.SemLetras, traduzir(t) { chamou = true; "" })
        }
        assertFalse(chamou)
    }

    @Test fun umaLetraBasta() {
        val r = traduzir("42 a") { resp("\"ok\",\"a\"") }
        assertEquals(Resultado.Traducao("ok"), r)
    }

    @Test fun acimaDoLimiteNaoChamaRede() {
        var chamou = false
        val r = traduzir("a".repeat(MAX_CHARS + 1)) { chamou = true; "" }
        assertEquals(Resultado.GrandeDemais(MAX_CHARS + 1), r)
        assertFalse(chamou)
    }

    @Test fun emojiContaUmCaracterePorCodePoint() {
        var chamou = false
        val r = traduzir("a" + "😀".repeat(MAX_CHARS)) { chamou = true; "" }
        assertEquals(Resultado.GrandeDemais(MAX_CHARS + 1), r)
        assertFalse(chamou)
    }

    @Test fun urlCodificadaGrandeDemaisNaoChamaRede() {
        var chamou = false
        // 3000 caracteres (dentro de MAX_CHARS), mas cada "é" vira 6 na URL
        val r = traduzir("é".repeat(MAX_CHARS)) { chamou = true; "" }
        assertEquals(Resultado.GrandeDemais(MAX_CHARS), r)
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

    @Test fun urlComParametrosETextoCodificado() {
        var url = ""
        traduzir("olá mundo&x=1") { url = it; resp("\"a\",\"b\"") }
        assertTrue(url.startsWith("https://translate.googleapis.com/translate_a/single?"))
        for (p in listOf("client=gtx", "sl=auto", "tl=pt", "dt=t")) {
            assertTrue(p, url.contains(p))
        }
        assertTrue(url, url.endsWith("q=ol%C3%A1+mundo%26x%3D1"))
    }

    private fun casosFile(nome: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "casos/$nome") }
        .first { it.exists() }

    // os casos moram em casos/juntar_linhas.json (raiz) e valem tambem para o PC
    @Test fun casosCompartilhadosDeJuntarLinhas() {
        val casos = JsonParser.parseString(casosFile("juntar_linhas.json").readText()).asJsonArray
        assertTrue(casos.size() > 0)
        for (c in casos) {
            val o = c.asJsonObject
            assertEquals(
                "caso quebrado: " + o["nome"].asString,
                o["esperado"].asString,
                juntarLinhas(o["entrada"].asString),
            )
        }
    }

    // os casos moram em casos/validacao.json (raiz) e valem tambem para o PC
    @Test fun casosCompartilhadosDeValidacao() {
        val base = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=pt&dt=t&q="
        val casos = JsonParser.parseString(casosFile("validacao.json").readText()).asJsonArray
        assertTrue(casos.size() > 0)
        for (c in casos) {
            val o = c.asJsonObject
            val nome = "caso quebrado: " + o["nome"].asString
            val vezes = o["vezes"]?.asInt ?: 1
            val esp = o["esperado"].asJsonObject
            val urls = mutableListOf<String>()
            val r = traduzir(o["entrada"].asString.repeat(vezes)) { urls.add(it); resp("\"ok\",\"x\"") }
            when (esp["tipo"].asString) {
                "ok" -> {
                    assertEquals(nome, Resultado.Traducao("ok"), r)
                    assertEquals(nome, listOf(base + esp["q"].asString.repeat(vezes)), urls)
                }
                "grande_demais" -> {
                    assertEquals(nome, Resultado.GrandeDemais(esp["n"].asInt), r)
                    assertTrue(nome, urls.isEmpty())
                }
                "vazio" -> { assertEquals(nome, Resultado.Vazio, r); assertTrue(nome, urls.isEmpty()) }
                "sem_letras" -> { assertEquals(nome, Resultado.SemLetras, r); assertTrue(nome, urls.isEmpty()) }
                else -> error("tipo desconhecido: " + esp["tipo"])
            }
        }
    }

    @Test fun traduzirEnviaTextoJuntado() {
        var url = ""
        traduzir("dis-\ntributed\nsystems") { url = it; resp("\"x\",\"y\"") }
        assertTrue(url, url.endsWith("q=distributed+systems"))
    }
}
