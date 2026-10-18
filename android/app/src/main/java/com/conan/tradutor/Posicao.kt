package com.conan.tradutor

data class Tamanho(val largura: Int, val altura: Int)
data class Pos(val x: Int, val y: Int)

// Mantem a janela dentro da tela. Funcao pura, usada pelo popup e pela bolha.
// centradaX = false: x e a distancia da borda esquerda (bolha, gravidade TOP|START).
// centradaX = true: x e o desvio do centro (popup, gravidade BOTTOM|CENTER_HORIZONTAL).
// y sempre vai de 0 a (tela - janela): distancia do topo (bolha) ou da base (popup).
fun ajustarPosicao(pos: Pos, tela: Tamanho, janela: Tamanho, centradaX: Boolean): Pos {
    val sobraX = maxOf(0, tela.largura - janela.largura)
    val x = if (centradaX) pos.x.coerceIn(-sobraX / 2, sobraX / 2) else pos.x.coerceIn(0, sobraX)
    val y = pos.y.coerceIn(0, maxOf(0, tela.altura - janela.altura))
    return Pos(x, y)
}
