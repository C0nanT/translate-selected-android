package com.conan.tradutor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToquesTest {
    @Test fun primeiroToqueSempreTraduz() {
        assertTrue(Toques(500).aceitar(0))
    }

    @Test fun toqueAMenosDe500msDoAnteriorEIgnorado() {
        val t = Toques(500)
        assertTrue(t.aceitar(1_000))
        assertFalse(t.aceitar(1_499))
    }

    @Test fun toqueIgnoradoNaoAdiaOProximo() {
        val t = Toques(500)
        t.aceitar(1_000)
        t.aceitar(1_400)
        assertTrue(t.aceitar(1_500))
    }
}
