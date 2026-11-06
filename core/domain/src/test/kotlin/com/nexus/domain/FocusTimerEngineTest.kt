package com.nexus.domain

import com.nexus.domain.focus.*
import com.nexus.domain.focus.FocusRules.Companion.MINUTE
import com.nexus.domain.time.FakeClock
import org.junit.Assert.*
import org.junit.Test

class FocusTimerEngineTest {
    private val engine = FocusTimerEngine()
    private val clock = FakeClock()
    private fun start(min: Int = 25) = engine.start("s1", " Read chapter 3 ", min, "command_centre", clock)

    @Test fun `start records planned duration and trims task`() {
        val s = start()
        assertEquals(25 * MINUTE, s.plannedMs)
        assertEquals("Read chapter 3", s.task)
        assertEquals(SessionState.RUNNING, s.state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects durations outside the rules`() { engine.start("x", "", 2, "e", clock) }

    @Test fun `remaining time is derived from timestamps`() {
        val s = start()
        clock.advance(10 * MINUTE + 30_000)
        assertEquals(10 * MINUTE + 30_000, engine.focusedMs(s, clock))
        assertEquals(14 * MINUTE + 30_000, engine.remainingMs(s, clock))
        assertTrue(engine.evaluate(s, clock) is SessionEvaluation.Running)
    }

    @Test fun `becomes due exactly at planned duration and completion is back-dated`() {
        val s = start(15)
        clock.advance(15 * MINUTE - 1)
        assertTrue(engine.evaluate(s, clock) is SessionEvaluation.Running)
        clock.advance(1)
        assertEquals(SessionEvaluation.DueForCompletion, engine.evaluate(s, clock))
        val reachedAt = clock.wall
        clock.advance(3 * 60 * MINUTE) // app was dead for 3 hours
        val done = engine.complete(s, clock)
        assertEquals(SessionState.COMPLETED, done.state)
        assertEquals(15 * MINUTE, done.bankedFocusMs)
        assertEquals(reachedAt, done.endedWall)
    }

    @Test fun `paused time does not count as focus`() {
        var s = start()
        clock.advance(5 * MINUTE)
        s = engine.pause(s, clock)
        clock.advance(10 * MINUTE)
        assertEquals(5 * MINUTE, engine.focusedMs(s, clock))
        s = engine.resume(s, clock)
        assertEquals(10 * MINUTE, s.totalPausedMs)
        clock.advance(5 * MINUTE)
        assertEquals(10 * MINUTE, engine.focusedMs(s, clock))
    }

    @Test fun `pause count is limited`() {
        var s = start()
        repeat(3) { s = engine.resume(engine.pause(s, clock), clock); clock.advance(1000) }
        assertFalse(engine.canPause(s))
        assertThrows(IllegalSessionTransition::class.java) { engine.pause(s, clock) }
    }

    @Test fun `exceeding total pause allowance terminates the session at the right time`() {
        var s = start()
        clock.advance(12 * MINUTE)
        s = engine.pause(s, clock)
        val pausedAt = clock.wall
        clock.advance(16 * MINUTE)
        val ev = engine.evaluate(s, clock)
        assertEquals(SessionEvaluation.DueForTermination(EndReason.PAUSE_LIMIT_EXCEEDED), ev)
        assertThrows(IllegalSessionTransition::class.java) { engine.resume(s, clock) }
        val ended = engine.abandon(s, EndReason.PAUSE_LIMIT_EXCEEDED, clock)
        assertEquals(SessionState.ABANDONED, ended.state)
        assertEquals(12 * MINUTE, ended.bankedFocusMs)
        assertEquals(pausedAt + 15 * MINUTE, ended.endedWall)
    }

    @Test fun `wall clock manipulation within a boot is ignored`() {
        val s = start()
        clock.advance(5 * MINUTE)
        clock.wall += 24 * 60 * MINUTE // user jumps the clock a day forward
        assertEquals(5 * MINUTE, engine.focusedMs(s, clock))
        assertTrue(engine.evaluate(s, clock) is SessionEvaluation.Running)
        clock.advance(20 * MINUTE)
        val done = engine.complete(s, clock)
        assertTrue(done.clockAnomaly)
    }

    @Test fun `reboot during a running session is bridged with wall time and flagged`() {
        var s = start()
        clock.advance(10 * MINUTE)
        clock.reboot(offMs = 2 * MINUTE)
        assertEquals(12 * MINUTE, engine.focusedMs(s, clock))
        s = engine.pause(s, clock)
        assertTrue(s.recoveredAcrossReboot)
        assertEquals("monotonic anchors are cleared on pause", null, s.segmentStartElapsed)
    }

    @Test fun `reboot while paused measures the pause with wall time`() {
        var s = start()
        clock.advance(MINUTE)
        s = engine.pause(s, clock)
        clock.reboot(offMs = 20 * MINUTE)
        assertEquals(SessionEvaluation.DueForTermination(EndReason.PAUSE_LIMIT_EXCEEDED), engine.evaluate(s, clock))
    }

    @Test fun `negative wall delta after reboot never produces negative focus`() {
        val s = start()
        clock.advance(3 * MINUTE)
        clock.reboot(offMs = 0)
        clock.wall -= 60 * MINUTE
        assertEquals(0, engine.focusedMs(s, clock))
    }

    @Test fun `interruptions are only counted while running`() {
        var s = start()
        s = engine.recordInterruption(s)
        assertEquals(1, s.interruptions)
        s = engine.recordInterruption(engine.pause(s, clock))
        assertEquals(1, s.interruptions)
    }

    @Test fun `closed sessions cannot transition again`() {
        val s = start(5)
        clock.advance(5 * MINUTE)
        val done = engine.complete(s, clock)
        assertEquals(SessionEvaluation.Closed, engine.evaluate(done, clock))
        assertThrows(IllegalSessionTransition::class.java) { engine.complete(done, clock) }
        assertThrows(IllegalSessionTransition::class.java) { engine.abandon(done, EndReason.ENDED_BY_USER, clock) }
    }
}
