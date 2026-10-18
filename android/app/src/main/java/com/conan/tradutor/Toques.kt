package com.conan.tradutor

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ignora toques a menos de minMs do ultimo aceito. sem Android: o "agora" (ms) vem em cada chamada.
class Toques(private val minMs: Long) {
    private var ultimo: Long? = null

    fun aceitar(agora: Long): Boolean {
        val u = ultimo
        if (u != null && agora - u < minMs) return false
        ultimo = agora
        return true
    }
}

// um executor so, compartilhado por todos os popups: pedidos nao empilham threads
object Requisicoes {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
}
