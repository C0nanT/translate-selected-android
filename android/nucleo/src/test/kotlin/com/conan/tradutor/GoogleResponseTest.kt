package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleResponseTest {
    private fun caseFile(name: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "cases/$name") }
        .first { it.exists() }

    // the cases live in cases/google_response.json (root) and also apply to the PC
    @Test fun sharedGoogleResponseCases() {
        val cases = JsonParser.parseString(caseFile("google_response.json").readText()).asJsonArray
        assertTrue(cases.size() > 0)
        for (c in cases) {
            val o = c.asJsonObject
            val name = "broken case: " + o["name"].asString
            val exp = o["expected"].asJsonObject
            val r = translate("x") { o["body"].asString }
            val expected = when (exp["type"].asString) {
                "translation" -> Result.Translation(exp["text"].asString)
                "failure" -> Result.Failure(exp["reason"].asString)
                else -> error("unknown type: " + exp["type"])
            }
            assertEquals(name, expected, r)
        }
    }
}
