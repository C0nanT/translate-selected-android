package com.conan.tradutor

import org.junit.Assert.assertEquals
import org.junit.Test

class PosicaoTest {
    private val tela = Tamanho(1000, 600)
    private val janela = Tamanho(400, 100)

    @Test fun dentroDaTelaNaoMuda() {
        assertEquals(Pos(20, 30), ajustarPosicao(Pos(20, 30), tela, janela, centradaX = false))
    }

    @Test fun cantoSuperiorEsquerdoNaoPassaDaBordaDireitaNemDeBaixo() {
        assertEquals(Pos(600, 500), ajustarPosicao(Pos(900, 580), tela, janela, centradaX = false))
        assertEquals(Pos(0, 0), ajustarPosicao(Pos(-50, -5), tela, janela, centradaX = false))
    }

    @Test fun centradaXMedeDesvioDoCentro() {
        assertEquals(Pos(300, 500), ajustarPosicao(Pos(900, 700), tela, janela, centradaX = true))
        assertEquals(Pos(-300, 0), ajustarPosicao(Pos(-900, -10), tela, janela, centradaX = true))
    }

    @Test fun janelaMaiorQueATelaFicaNoCanto() {
        assertEquals(Pos(0, 0), ajustarPosicao(Pos(50, 50), Tamanho(300, 80), janela, centradaX = true))
        assertEquals(Pos(0, 0), ajustarPosicao(Pos(50, 50), Tamanho(300, 80), janela, centradaX = false))
    }
}
