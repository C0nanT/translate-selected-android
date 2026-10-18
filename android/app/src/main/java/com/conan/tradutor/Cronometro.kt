package com.conan.tradutor

// tempo restante do popup. sem Android: o "agora" (ms) vem em cada chamada.
// estados: parado (nao iniciou, ex.: carregando), correndo, pausado (dedo encostado).
class Cronometro(private val totalMs: Long) {
    var restanteMs = totalMs
        private set
    private var ativo = false      // ja foi iniciado
    private var pausado = false
    private var ultimo = 0L

    val acabou get() = ativo && restanteMs <= 0
    val fracao get() = (restanteMs.toFloat() / totalMs).coerceIn(0f, 1f)

    // (re)inicia com o tempo cheio; se o dedo esta encostado, so corre ao soltar
    fun iniciar(agora: Long) {
        restanteMs = totalMs
        ativo = true
        ultimo = agora
    }

    fun pausar(agora: Long) {
        descontar(agora)
        pausado = true
    }

    fun retomar(agora: Long) {
        if (!pausado) return
        pausado = false
        ultimo = agora
    }

    // desconta o tempo corrido desde a ultima chamada e devolve o restante
    fun descontar(agora: Long): Long {
        if (ativo && !pausado) {
            restanteMs = maxOf(0L, restanteMs - (agora - ultimo))
            ultimo = agora
        }
        return restanteMs
    }
}
