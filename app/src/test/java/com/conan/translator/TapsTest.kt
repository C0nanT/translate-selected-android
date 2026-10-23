package com.conan.translator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapsTest {
    @Test fun firstTapAlwaysTranslates() {
        assertTrue(Taps(500).accept(0))
    }

    @Test fun tapWithin500msOfThePreviousIsIgnored() {
        val t = Taps(500)
        assertTrue(t.accept(1_000))
        assertFalse(t.accept(1_499))
    }

    @Test fun ignoredTapDoesNotPostponeTheNext() {
        val t = Taps(500)
        t.accept(1_000)
        t.accept(1_400)
        assertTrue(t.accept(1_500))
    }
}
