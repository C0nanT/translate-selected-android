package com.conan.tradutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CronometroTest {
    @Test fun contaAteZero() {
        val c = Cronometro(15_000)
        c.iniciar(1_000)
        assertEquals(10_000L, c.descontar(6_000))
        assertFalse(c.acabou)
        assertEquals(0L, c.descontar(16_000))
        assertTrue(c.acabou)
    }

    @Test fun pausaERetomadaNaoPerdemNemGanhamTempo() {
        val c = Cronometro(15_000)
        c.iniciar(0)
        c.pausar(4_000)
        assertEquals(11_000L, c.descontar(60_000))  // pausado: relogio andando nao desconta
        c.retomar(60_000)
        assertEquals(8_000L, c.descontar(63_000))
    }

    @Test fun reiniciarEnquantoPausadoNaoCorre() {
        val c = Cronometro(15_000)
        c.iniciar(0)
        c.pausar(2_000)
        c.iniciar(5_000)
        assertEquals(15_000L, c.descontar(50_000))
        assertEquals(1f, c.fracao, 0f)
        c.retomar(50_000)
        assertEquals(14_000L, c.descontar(51_000))
    }

    @Test fun parado_naoDesconta_ePausarRetomarNaoInicia() {
        val c = Cronometro(15_000)
        assertEquals(15_000L, c.descontar(99_000))
        c.pausar(100_000)
        c.retomar(200_000)
        assertEquals(15_000L, c.descontar(300_000))
        assertFalse(c.acabou)
    }

    @Test fun reiniciarVoltaAoTotal() {
        val c = Cronometro(15_000)
        c.iniciar(0)
        c.descontar(10_000)
        c.iniciar(10_000)
        assertEquals(15_000L, c.descontar(10_000))
    }

    @Test fun fracaoDaBarra() {
        val c = Cronometro(10_000)
        assertEquals(1f, c.fracao, 0f)
        c.iniciar(0)
        c.descontar(2_500)
        assertEquals(0.75f, c.fracao, 0f)
        c.descontar(20_000)
        assertEquals(0f, c.fracao, 0f)
    }

    @Test fun pararVoltaAoEstadoParadoDoCarregamento() {
        val c = Cronometro(15_000)
        assertFalse(c.iniciado)
        c.iniciar(0)
        c.descontar(5_000)
        assertTrue(c.iniciado)
        c.parar()
        assertFalse(c.iniciado)
        assertEquals(15_000L, c.descontar(99_000))
        assertFalse(c.acabou)
    }

    @Test fun resultadoComDedoEncostadoDepoisDeCarregarSoCorreAoSoltar() {
        val c = Cronometro(15_000)
        c.parar()                 // carregando
        c.pausar(1_000)           // dedo encosta durante o carregamento
        c.iniciar(8_000)          // resultado chega
        assertEquals(15_000L, c.descontar(30_000))
        c.retomar(30_000)
        assertEquals(13_000L, c.descontar(32_000))
    }
}
