package com.conan.translator

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionTest {
    private val screen = Size(1000, 600)
    private val window = Size(400, 100)

    @Test fun insideScreenDoesNotMove() {
        assertEquals(Position(20, 30), clampPosition(Position(20, 30), screen, window, centeredX = false))
    }

    @Test fun clampsAtRightAndBottomEdgesAndAtLeftAndTop() {
        assertEquals(Position(600, 500), clampPosition(Position(900, 580), screen, window, centeredX = false))
        assertEquals(Position(0, 0), clampPosition(Position(-50, -5), screen, window, centeredX = false))
    }

    @Test fun centeredXMeasuresOffsetFromCenter() {
        assertEquals(Position(300, 500), clampPosition(Position(900, 700), screen, window, centeredX = true))
        assertEquals(Position(-300, 0), clampPosition(Position(-900, -10), screen, window, centeredX = true))
    }

    @Test fun windowLargerThanScreenStaysInCorner() {
        assertEquals(Position(0, 0), clampPosition(Position(50, 50), Size(300, 80), window, centeredX = true))
        assertEquals(Position(0, 0), clampPosition(Position(50, 50), Size(300, 80), window, centeredX = false))
    }
}
