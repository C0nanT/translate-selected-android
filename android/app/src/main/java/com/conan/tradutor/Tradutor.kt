package com.conan.tradutor

import java.net.URLEncoder

// Kotlin puro (sem Android): a busca HTTP vem de fora, recebe a URL e devolve o corpo
// (ou lanca excecao em falha de rede/timeout).
sealed class Resultado {
    data class Traducao(val texto: String) : Resultado()
    data class GrandeDemais(val n: Int) : Resultado()
    data class Erro(val motivo: String) : Resultado()
}

fun traduzir(texto: String, buscar: (String) -> String): Resultado {
    if (texto.length > MAX_CHARS) return Resultado.GrandeDemais(texto.length)
    val corpo = try {
        buscar(urlTraducao(texto))
    } catch (e: Exception) {
        return Resultado.Erro(e.message ?: e.javaClass.simpleName)
    }
    return try {
        Resultado.Traducao(extrair(corpo))
    } catch (e: Exception) {
        Resultado.Erro("Resposta inesperada do tradutor")
    }
}

fun urlTraducao(texto: String): String =
    "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=pt&dt=t&q=" +
        URLEncoder.encode(texto, "UTF-8")

// data[0] = partes; concatena o primeiro elemento de cada uma, ignorando vazias
private fun extrair(corpo: String): String {
    val data = JsonMini(corpo).lerTudo() as List<*>
    val partes = data[0] as List<*>
    return partes.joinToString("") {
        val p = (it as List<*>)[0]
        if (p != null && p !is String) error("parte invalida")
        p as String? ?: ""
    }
}

// parser JSON minimo (so o necessario): listas, strings, numeros, true/false/null, objetos
private class JsonMini(private val s: String) {
    private var i = 0

    fun lerTudo(): Any? {
        val v = valor()
        espaco()
        if (i != s.length) error("lixo no fim")
        return v
    }

    private fun espaco() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun valor(): Any? {
        espaco()
        if (i >= s.length) error("fim inesperado")
        return when (val c = s[i]) {
            '[' -> {
                i++
                val l = ArrayList<Any?>()
                espaco()
                if (s.getOrNull(i) == ']') { i++; return l }
                while (true) {
                    l.add(valor()); espaco()
                    when (s.getOrNull(i++)) { ',' -> {} ; ']' -> return l; else -> error("lista") }
                }
                @Suppress("UNREACHABLE_CODE") l
            }
            '{' -> {
                i++
                val m = HashMap<String, Any?>()
                espaco()
                if (s.getOrNull(i) == '}') { i++; return m }
                while (true) {
                    espaco()
                    val k = valor() as? String ?: error("chave")
                    espaco()
                    if (s.getOrNull(i++) != ':') error("':'")
                    m[k] = valor(); espaco()
                    when (s.getOrNull(i++)) { ',' -> {} ; '}' -> return m; else -> error("objeto") }
                }
                @Suppress("UNREACHABLE_CODE") m
            }
            '"' -> string()
            else -> if (c == '-' || c.isDigit()) numero() else literal()
        }
    }

    private fun literal(): Any? {
        for ((nome, v) in listOf("true" to true, "false" to false, "null" to null)) {
            if (s.startsWith(nome, i)) { i += nome.length; return v }
        }
        error("valor invalido")
    }

    private fun numero(): Double {
        val ini = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        return s.substring(ini, i).toDouble()
    }

    private fun string(): String {
        i++ // aspas de abertura
        val sb = StringBuilder()
        while (true) {
            if (i >= s.length) error("string aberta")
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    when (val e = s.getOrNull(i++) ?: error("escape")) {
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        '"', '\\', '/' -> sb.append(e)
                        else -> error("escape invalido")
                    }
                }
                else -> sb.append(c)
            }
        }
    }
}
