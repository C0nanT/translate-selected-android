package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ErrosRedeTest {
    private fun arquivo(nome: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "casos/$nome") }
        .first { it.exists() }

    // os casos moram em casos/erros_rede.json (raiz) e valem tambem para o PC
    @Test fun casosCompartilhadosDeErrosDeRede() {
        val casos = JsonParser.parseString(arquivo("erros_rede.json").readText()).asJsonArray
        assertTrue(casos.size() > 0)
        for (c in casos) {
            val o = c.asJsonObject
            val f = o["falha"].asJsonObject
            val erro: ErroRede = when (f["tipo"].asString) {
                "tempo_esgotado" -> ErroRede.TempoEsgotado
                "sem_conexao" -> ErroRede.SemConexao
                "http" -> ErroRede.Http(f["codigo"].asInt)
                "outro" -> ErroRede.Outro(f["detalhe"].asString)
                else -> error("tipo desconhecido: " + f["tipo"])
            }
            val r = traduzir("x") { throw erro }
            assertEquals("caso quebrado: " + o["nome"].asString, Resultado.Erro(o["esperado"].asString), r)
        }
    }

    @Test fun excecaoForaDoContratoViraOutro() {
        assertEquals(Resultado.Erro("boom"), traduzir("x") { throw java.io.IOException("boom") })
    }
}
