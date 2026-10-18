package com.conan.tradutor

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URLEncoder

// Kotlin puro (sem Android): a busca HTTP vem de fora, recebe a URL e devolve o corpo
// (ou lanca excecao em falha de rede/timeout).
sealed class Resultado {
    data class Traducao(val texto: String) : Resultado()
    data class GrandeDemais(val n: Int) : Resultado()
    data class Erro(val motivo: String) : Resultado()
}

// busca lanca isto em resposta HTTP diferente de 200
class ErroHttp(val codigo: Int) : IOException("HTTP $codigo")

// texto copiado de PDF traz \n em cada fim de linha (e hifen em palavra cortada);
// o Google traduz cada linha solta e perde o contexto. Junta as linhas; linha em
// branco continua separando paragrafos.
fun juntarLinhas(texto: String): String =
    texto.replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n\\s*"))
        .joinToString("\n\n") { par ->
            par.replace(Regex("-[ \t]*\n[ \t]*(?=\\p{Ll})"), "")  // dis-|tributed
                .replace(Regex("-[ \t]*\n[ \t]*"), "-")              // Monolith-|To
                .replace(Regex("[ \t]*\n[ \t]*"), " ")
                .replace(Regex("[ \t]{2,}"), " ")
                .trim()
        }

fun traduzir(bruto: String, buscar: (String) -> String): Resultado {
    val texto = juntarLinhas(bruto)
    if (texto.length > MAX_CHARS) return Resultado.GrandeDemais(texto.length)
    val corpo = try {
        buscar(urlTraducao(texto))
    } catch (e: Exception) {
        return Resultado.Erro(motivoDe(e))
    }
    return try {
        Resultado.Traducao(extrair(corpo))
    } catch (e: Exception) {
        Resultado.Erro("Resposta inesperada do Google Tradutor")
    }
}

// motivo legivel em pt-BR; caso desconhecido cai na mensagem da excecao
private fun motivoDe(e: Exception): String = when (e) {
    is ErroHttp -> "Erro do servidor (HTTP ${e.codigo})"
    is SocketTimeoutException -> "Tempo esgotado"
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> "Sem conexão com a internet"
    else -> e.message ?: e.javaClass.simpleName
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

// parser JSON minimo (so o necessario): listas, strings, numeros, true/false/null; objeto vira erro
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
