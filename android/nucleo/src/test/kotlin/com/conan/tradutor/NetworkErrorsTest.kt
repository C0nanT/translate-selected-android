package com.conan.tradutor

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NetworkErrorsTest {
    private fun caseFile(name: String) = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "cases/$name") }
        .first { it.exists() }

    // the cases live in cases/network_errors.json (root) and also apply to the PC
    @Test fun sharedNetworkErrorCases() {
        val cases = JsonParser.parseString(caseFile("network_errors.json").readText()).asJsonArray
        assertTrue(cases.size() > 0)
        for (c in cases) {
            val o = c.asJsonObject
            val f = o["failure"].asJsonObject
            val error: NetworkError = when (f["type"].asString) {
                "timeout" -> NetworkError.Timeout
                "no_connection" -> NetworkError.NoConnection
                "http" -> NetworkError.Http(f["code"].asInt)
                "other" -> NetworkError.Other(f["detail"].asString)
                else -> error("unknown type: " + f["type"])
            }
            val r = translate("x") { throw error }
            assertEquals("broken case: " + o["name"].asString, Result.Failure(o["expected"].asString), r)
        }
    }

    @Test fun exceptionOutsideTheContractBecomesOther() {
        assertEquals(Result.Failure("boom"), translate("x") { throw java.io.IOException("boom") })
    }
}
