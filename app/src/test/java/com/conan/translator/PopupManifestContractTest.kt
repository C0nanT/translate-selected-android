package com.conan.translator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// enforces the contract listed in PopupActivity: every @style/Popup entry point in the manifest
class PopupManifestContractTest {
    private val androidNs = "http://schemas.android.com/apk/res/android"
    private val requiredChanges = listOf(
        "orientation", "screenSize", "smallestScreenSize", "screenLayout",
        "keyboard", "keyboardHidden", "navigation", "uiMode",
    )

    private fun manifestFile() = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "app/src/main/AndroidManifest.xml") }
        .first { it.exists() }

    private fun popupActivities(xml: String): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val nodes = doc.getElementsByTagName("activity")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .filter { it.getAttributeNS(androidNs, "theme") == "@style/Popup" }
    }

    private fun violations(xml: String): List<String> = popupActivities(xml).flatMap { a ->
        val name = a.getAttributeNS(androidNs, "name")
        val changes = a.getAttributeNS(androidNs, "configChanges").split("|")
        requiredChanges.filter { it !in changes }
            .map { "$name: configChanges is missing $it" } +
            listOfNotNull(
                if (a.getAttributeNS(androidNs, "excludeFromRecents") != "true")
                    "$name: excludeFromRecents must be \"true\"" else null,
            )
    }

    @Test fun realManifestPopupEntryPointsHonourTheContract() {
        val xml = manifestFile().readText()
        assertTrue("no @style/Popup activity found", popupActivities(xml).isNotEmpty())
        assertEquals(emptyList<String>(), violations(xml))
    }

    @Test fun missingUiModeIsReportedWithActivityAndPiece() {
        val xml = """
            <manifest xmlns:android="$androidNs"><application>
              <activity android:name=".FooActivity" android:theme="@style/Popup"
                android:excludeFromRecents="true"
                android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|navigation"/>
            </application></manifest>
        """.trimIndent()
        assertEquals(listOf(".FooActivity: configChanges is missing uiMode"), violations(xml))
    }

    @Test fun missingExcludeFromRecentsIsReported() {
        val xml = """
            <manifest xmlns:android="$androidNs"><application>
              <activity android:name=".Bar" android:theme="@style/Popup"
                android:configChanges="${requiredChanges.joinToString("|")}"/>
            </application></manifest>
        """.trimIndent()
        assertEquals(listOf(".Bar: excludeFromRecents must be \"true\""), violations(xml))
    }
}
