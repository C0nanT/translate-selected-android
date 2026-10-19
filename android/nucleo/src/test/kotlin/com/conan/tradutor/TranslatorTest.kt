package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class TranslatorTest {
    private fun response(vararg parts: String) =
        "[[" + parts.joinToString(",") { "[$it]" } + "],null,\"en\"]"

    @Test fun emptyTextDoesNotHitNetwork() {
        var called = false
        assertEquals(Result.Empty, translate("") { called = true; "" })
        assertEquals(Result.Empty, translate("  \n \r\n\t ") { called = true; "" })
        assertFalse(called)
    }

    @Test fun noLettersDoesNotHitNetwork() {
        var called = false
        for (t in listOf("42", "—", "3,14 % + 7", "content://media/external/1"
            .filter { !it.isLetter() })) {
            assertEquals(Result.NoLetters, translate(t) { called = true; "" })
        }
        assertFalse(called)
    }

    @Test fun oneLetterIsEnough() {
        val r = translate("42 a") { response("\"ok\",\"a\"") }
        assertEquals(Result.Translation("ok"), r)
    }

    @Test fun aboveTheLimitDoesNotHitNetwork() {
        var called = false
        val r = translate("a".repeat(MAX_CHARS + 1)) { called = true; "" }
        assertEquals(Result.TooLarge(MAX_CHARS + 1), r)
        assertFalse(called)
    }

    @Test fun emojiCountsOneCharacterPerCodePoint() {
        var called = false
        val r = translate("a" + "😀".repeat(MAX_CHARS)) { called = true; "" }
        assertEquals(Result.TooLarge(MAX_CHARS + 1), r)
        assertFalse(called)
    }

    @Test fun encodedUrlTooLargeDoesNotHitNetwork() {
        var called = false
        // 3000 characters (within MAX_CHARS), but each "é" becomes 6 in the URL
        val r = translate("é".repeat(MAX_CHARS)) { called = true; "" }
        assertEquals(Result.TooLarge(MAX_CHARS), r)
        assertFalse(called)
    }

    @Test fun atTheLimitTranslates() {
        val r = translate("a".repeat(MAX_CHARS)) { response("\"ok\",\"a\"") }
        assertEquals(Result.Translation("ok"), r)
    }

    @Test fun networkFailureBecomesFailure() {
        val r = translate("x") { throw IOException("sem rede") }
        assertTrue(r is Result.Failure)
        assertTrue((r as Result.Failure).reason.contains("sem rede"))
    }

    @Test fun urlHasParametersAndEncodedText() {
        var url = ""
        translate("olá mundo&x=1") { url = it; response("\"a\",\"b\"") }
        assertTrue(url.startsWith("https://translate.googleapis.com/translate_a/single?"))
        for (p in listOf("client=gtx", "sl=auto", "tl=pt", "dt=t")) {
            assertTrue(p, url.contains(p))
        }
        assertTrue(url, url.endsWith("q=ol%C3%A1+mundo%26x%3D1"))
    }

    private fun caseFile(name: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "cases/$name") }
        .first { it.exists() }

    // the cases live in cases/join_lines.json (root) and also apply to the PC
    @Test fun sharedJoinLinesCases() {
        val cases = JsonParser.parseString(caseFile("join_lines.json").readText()).asJsonArray
        assertTrue(cases.size() > 0)
        for (c in cases) {
            val o = c.asJsonObject
            assertEquals(
                "broken case: " + o["name"].asString,
                o["expected"].asString,
                joinLines(o["input"].asString),
            )
        }
    }

    // the cases live in cases/validation.json (root) and also apply to the PC
    @Test fun sharedValidationCases() {
        val base = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=pt&dt=t&q="
        val cases = JsonParser.parseString(caseFile("validation.json").readText()).asJsonArray
        assertTrue(cases.size() > 0)
        for (c in cases) {
            val o = c.asJsonObject
            val name = "broken case: " + o["name"].asString
            val times = o["times"]?.asInt ?: 1
            val exp = o["expected"].asJsonObject
            val urls = mutableListOf<String>()
            val r = translate(o["input"].asString.repeat(times)) { urls.add(it); response("\"ok\",\"x\"") }
            when (exp["type"].asString) {
                "ok" -> {
                    assertEquals(name, Result.Translation("ok"), r)
                    assertEquals(name, listOf(base + exp["q"].asString.repeat(times)), urls)
                }
                "too_large" -> {
                    assertEquals(name, Result.TooLarge(exp["n"].asInt), r)
                    assertTrue(name, urls.isEmpty())
                }
                "empty" -> { assertEquals(name, Result.Empty, r); assertTrue(name, urls.isEmpty()) }
                "no_letters" -> { assertEquals(name, Result.NoLetters, r); assertTrue(name, urls.isEmpty()) }
                else -> error("unknown type: " + exp["type"])
            }
        }
    }

    @Test fun translateSendsJoinedText() {
        var url = ""
        translate("dis-\ntributed\nsystems") { url = it; response("\"x\",\"y\"") }
        assertTrue(url, url.endsWith("q=distributed+systems"))
    }
}
