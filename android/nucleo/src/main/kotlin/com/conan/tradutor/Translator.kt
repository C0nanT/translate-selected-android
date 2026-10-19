package com.conan.tradutor

import java.io.IOException
import java.net.URLEncoder

// Pure Kotlin (no Android): the HTTP fetch comes from outside, receives the URL and returns the body
// (or throws on network failure/timeout).
sealed class Result {
    data class Translation(val text: String) : Result()
    data class TooLarge(val n: Int) : Result()
    object Empty : Result()
    object NoLetters : Result()
    data class Failure(val reason: String) : Result()
}

// Contract between the network adapter and the core (equal to NetworkError in pc/nucleo.py): the fetch
// throws NetworkError; the adapter converts platform exceptions (see Rede.kt).
// kinds: timeout, no_connection, http (+ code), other (+ detail).
sealed class NetworkError : IOException() {
    object Timeout : NetworkError()
    object NoConnection : NetworkError()
    class Http(val code: Int) : NetworkError()
    class Other(val detail: String) : NetworkError()

    // the only place for network error messages (user-facing, pt-BR)
    override val message: String
        get() = when (this) {
            Timeout -> "Tempo esgotado"
            NoConnection -> "Sem conexão com a internet"
            is Http -> "Erro do servidor (HTTP $code)"
            is Other -> detail
        }
}

// text copied from a PDF has \n at every line end (and a hyphen in split words);
// Google translates each loose line and loses context. Joins the lines; a blank
// line still separates paragraphs.
// Copy of join_lines in pc/nucleo.py: keep both equal,
// checked by the shared cases in cases/join_lines.json.
fun joinLines(text: String): String =
    text.replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n\\s*"))
        .joinToString("\n\n") { par ->
            par.replace(Regex("-[ \t]*\n[ \t]*(?=\\p{Ll})"), "")  // dis-|tributed
                .replace(Regex("-[ \t]*\n[ \t]*"), "-")              // Monolith-|To
                .replace(Regex("[ \t]*\n[ \t]*"), " ")
                .replace(Regex("[ \t]{2,}"), " ")
                .trim()
        }

fun translate(raw: String, fetch: (String) -> String): Result {
    val text = joinLines(raw)
    if (text.isBlank()) return Result.Empty
    if (text.none { it.isLetter() }) return Result.NoLetters
    val n = text.codePointCount(0, text.length)  // an emoji counts 1, like len() on the PC
    if (n > MAX_CHARS) return Result.TooLarge(n)
    val url = translationUrl(text)
    if (url.length > MAX_URL) return Result.TooLarge(n)  // n here is <= MAX_CHARS: the message shows the character count, not the URL size
    val body = try {
        fetch(url)
    } catch (e: Exception) {
        return Result.Failure(reasonOf(e))
    }
    return try {
        Result.Translation(extract(body))
    } catch (e: Exception) {
        Result.Failure("Resposta inesperada do Google Tradutor")
    }
}

// exception outside the contract (not NetworkError) falls back to its own message, as "other"
private fun reasonOf(e: Exception): String =
    if (e is NetworkError) e.message else e.message ?: e.javaClass.simpleName

fun translationUrl(text: String): String =
    "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$TARGET_LANGUAGE&dt=t&q=" +
        encodeQuery(text)

// Single encoding rule for q (equal to translation_url in pc/nucleo.py): RFC 3986. Only
// A-Z a-z 0-9 - . _ ~ stay raw; space becomes +; everything else becomes %XX in UTF-8.
// URLEncoder diverges on ~ (gives %7E) and * (leaves raw): fixes both here, Python is the standard.
private fun encodeQuery(text: String): String =
    URLEncoder.encode(text, "UTF-8").replace("*", "%2A").replace("%7E", "~")

// data[0] = parts; concatenates the first element of each, ignoring empty ones
private fun extract(body: String): String {
    val data = JsonMini(body).readAll() as List<*>
    val parts = data[0] as List<*>
    return parts.joinToString("") {
        val p = (it as List<*>)[0]
        if (p != null && p !is String) error("invalid part")
        p as String? ?: ""
    }
}

// minimal JSON parser (only what is needed): lists, strings, numbers, true/false/null; an object is an error
private class JsonMini(private val s: String) {
    private var i = 0

    fun readAll(): Any? {
        val v = value()
        skipSpace()
        if (i != s.length) error("trailing garbage")
        return v
    }

    private fun skipSpace() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun value(): Any? {
        skipSpace()
        if (i >= s.length) error("unexpected end")
        return when (val c = s[i]) {
            '[' -> {
                i++
                val l = ArrayList<Any?>()
                skipSpace()
                if (s.getOrNull(i) == ']') { i++; return l }
                while (true) {
                    l.add(value()); skipSpace()
                    when (s.getOrNull(i++)) { ',' -> {} ; ']' -> return l; else -> error("list") }
                }
            }
            '"' -> string()
            else -> if (c == '-' || c.isDigit()) number() else literal()
        }
    }

    private fun literal(): Any? {
        for ((name, v) in listOf("true" to true, "false" to false, "null" to null)) {
            if (s.startsWith(name, i)) { i += name.length; return v }
        }
        error("invalid value")
    }

    private fun number(): Double {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        return s.substring(start, i).toDouble()
    }

    private fun string(): String {
        i++ // opening quote
        val sb = StringBuilder()
        while (true) {
            if (i >= s.length) error("open string")
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    when (val e = s.getOrNull(i++) ?: error("escape")) {
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        '"', '\\', '/' -> sb.append(e)
                        else -> error("invalid escape")
                    }
                }
                else -> sb.append(c)
            }
        }
    }
}
