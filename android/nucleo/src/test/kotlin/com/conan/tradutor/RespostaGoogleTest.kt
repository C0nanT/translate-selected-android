package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RespostaGoogleTest {
    private fun casosFile(nome: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "casos/$nome") }
        .first { it.exists() }

    // os casos moram em casos/resposta_google.json (raiz) e valem tambem para o PC
    @Test fun casosCompartilhadosDeRespostaGoogle() {
        val casos = JsonParser.parseString(casosFile("resposta_google.json").readText()).asJsonArray
        assertTrue(casos.size() > 0)
        for (c in casos) {
            val o = c.asJsonObject
            val nome = "caso quebrado: " + o["nome"].asString
            val esp = o["esperado"].asJsonObject
            val r = traduzir("x") { o["corpo"].asString }
            val esperado = when (esp["tipo"].asString) {
                "traducao" -> Resultado.Traducao(esp["texto"].asString)
                "erro" -> Resultado.Erro(esp["motivo"].asString)
                else -> error("tipo desconhecido: " + esp["tipo"])
            }
            assertEquals(nome, esperado, r)
        }
    }
}
