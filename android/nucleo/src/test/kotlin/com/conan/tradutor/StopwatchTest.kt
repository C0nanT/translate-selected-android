package com.conan.tradutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {
    @Test fun countsDownToZero() {
        val s = Stopwatch(15_000)
        s.start(1_000)
        assertEquals(10_000L, s.elapse(6_000))
        assertFalse(s.finished)
        assertEquals(0L, s.elapse(16_000))
        assertTrue(s.finished)
    }

    @Test fun pauseAndResumeNeitherLoseNorGainTime() {
        val s = Stopwatch(15_000)
        s.start(0)
        s.pause(4_000)
        assertEquals(11_000L, s.elapse(60_000))  // paused: wall clock running does not count down
        s.resume(60_000)
        assertEquals(8_000L, s.elapse(63_000))
    }

    @Test fun restartWhilePausedDoesNotRun() {
        val s = Stopwatch(15_000)
        s.start(0)
        s.pause(2_000)
        s.start(5_000)
        assertEquals(15_000L, s.elapse(50_000))
        assertEquals(1f, s.fraction, 0f)
        s.resume(50_000)
        assertEquals(14_000L, s.elapse(51_000))
    }

    @Test fun idleDoesNotCountDown_andPauseResumeDoesNotStart() {
        val s = Stopwatch(15_000)
        assertEquals(15_000L, s.elapse(99_000))
        s.pause(100_000)
        s.resume(200_000)
        assertEquals(15_000L, s.elapse(300_000))
        assertFalse(s.finished)
    }

    @Test fun restartResetsToFullTime() {
        val s = Stopwatch(15_000)
        s.start(0)
        s.elapse(10_000)
        s.start(10_000)
        assertEquals(15_000L, s.elapse(10_000))
    }

    @Test fun barFraction() {
        val s = Stopwatch(10_000)
        assertEquals(1f, s.fraction, 0f)
        s.start(0)
        s.elapse(2_500)
        assertEquals(0.75f, s.fraction, 0f)
        s.elapse(20_000)
        assertEquals(0f, s.fraction, 0f)
    }

    @Test fun stopReturnsToIdleLoadingState() {
        val s = Stopwatch(15_000)
        assertFalse(s.started)
        s.start(0)
        s.elapse(5_000)
        assertTrue(s.started)
        s.stop()
        assertFalse(s.started)
        assertEquals(15_000L, s.elapse(99_000))
        assertFalse(s.finished)
    }

    @Test fun resultWithFingerDownDuringLoadingOnlyRunsAfterRelease() {
        val s = Stopwatch(15_000)
        s.stop()                  // loading
        s.pause(1_000)            // finger goes down while loading
        s.start(8_000)            // result arrives
        assertEquals(15_000L, s.elapse(30_000))
        s.resume(30_000)
        assertEquals(13_000L, s.elapse(32_000))
    }
}
