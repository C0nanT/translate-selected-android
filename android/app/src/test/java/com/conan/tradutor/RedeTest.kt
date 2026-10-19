package com.conan.tradutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RedeTest {
    @Test fun timeoutViraTempoEsgotado() {
        assertSame(ErroRede.TempoEsgotado, converterExcecao(SocketTimeoutException("Read timed out")))
    }

    @Test fun hostDesconhecidoRecusadaOuSemRotaViraSemConexao() {
        for (e in listOf(UnknownHostException("x"), ConnectException("refused"), NoRouteToHostException("rota"))) {
            assertSame(e.toString(), ErroRede.SemConexao, converterExcecao(e))
        }
    }

    @Test fun erroRedeJaConvertidoPassaIntacto() {
        val http = ErroRede.Http(503)
        assertSame(http, converterExcecao(http))
    }

    @Test fun restoViraOutroComDetalhe() {
        val r = converterExcecao(IOException("boom"))
        assertTrue(r is ErroRede.Outro)
        assertEquals("boom", (r as ErroRede.Outro).detalhe)
        assertEquals("IllegalStateException", (converterExcecao(IllegalStateException()) as ErroRede.Outro).detalhe)
    }
}
