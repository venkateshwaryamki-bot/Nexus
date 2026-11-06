package com.nexus.domain

import com.nexus.domain.focus.*
import com.nexus.domain.focus.FocusRules.Companion.MINUTE
import com.nexus.domain.repository.ProgressionRepository
import com.nexus.domain.repository.SessionRepository
import com.nexus.domain.repository.SettlementResult
import com.nexus.domain.reward.Reward
import com.nexus.domain.reward.RewardKind
import com.nexus.domain.time.FakeClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** In-memory repositories mirroring the Room implementation's guarantees. */
class InMemoryStore : SessionRepository, ProgressionRepository {
    val all = MutableStateFlow<Map<String, FocusSession>>(emptyMap())
    val ledger = mutableMapOf<String, Reward>()
    var energy = 0; var xp = 0
    val minutesByDay = mutableMapOf<LocalDate, Int>()

    override fun observeOpenSession(): Flow<FocusSession?> = all.map { m -> m.values.firstOrNull { it.state.isOpen } }
    override suspend fun openSession() = all.value.values.firstOrNull { it.state.isOpen }
    override suspend fun insertNew(session: FocusSession) { all.value = all.value + (session.id to session) }
    override suspend fun updateOpen(session: FocusSession): Boolean {
        val cur = all.value[session.id] ?: return false
        if (!cur.state.isOpen) return false
        all.value = all.value + (session.id to session); return true
    }
    override suspend fun recentSessions(limit: Int) = all.value.values.toList().takeLast(limit)
    override fun observeRecentSessions(limit: Int) = all.map { it.values.toList() }
    override suspend fun settle(closedSession: FocusSession, reward: Reward, day: LocalDate): SettlementResult = synchronized(this) {
        val cur = all.value[closedSession.id]
        if (cur == null || !cur.state.isOpen || ledger.containsKey(closedSession.id)) return@synchronized SettlementResult(false, ledger[closedSession.id] ?: Reward.None)
        all.value = all.value + (closedSession.id to closedSession)
        ledger[closedSession.id] = reward; energy += reward.energy; xp += reward.xp
        if (reward.kind != RewardKind.NONE) minutesByDay[day] = (minutesByDay[day] ?: 0) + (closedSession.bankedFocusMs / MINUTE).toInt()
        SettlementResult(true, reward)
    }
    override suspend fun rewardedFocusMinutesOn(day: LocalDate) = minutesByDay[day] ?: 0
}

class FocusCoordinatorTest {
    // 06:00 UTC so a full day of sessions stays on one calendar day.
    private val clock = FakeClock(wall = java.time.Instant.parse("2026-11-06T06:00:00Z").toEpochMilli())
    private val store = InMemoryStore()
    private var n = 0
    private val c = FocusCoordinator(store, store, clock, zone = { ZoneOffset.UTC }, idFactory = { "s${++n}" })

    @Test fun `full session grants reward exactly once across repeated reconciles`() = runTest {
        c.start("Deep work", 25, "command_centre")
        clock.advance(25 * MINUTE)
        val results = (1..20).map { async { c.reconcile() } }.awaitAll()
        assertEquals(1, results.count { it is ReconcileResult.Closed && it.newlySettled })
        assertEquals(25, store.energy); assertEquals(75, store.xp)
        assertEquals(ReconcileResult.NoSession, c.reconcile())
    }

    @Test fun `settlement is idempotent even when called directly twice`() = runTest {
        val s = c.start("", 15, "e"); clock.advance(15 * MINUTE)
        val closed = c.timer.complete(s, clock)
        assertTrue(store.settle(closed, Reward(15, 55, RewardKind.COMPLETED), LocalDate.now()).settled)
        assertFalse(store.settle(closed, Reward(15, 55, RewardKind.COMPLETED), LocalDate.now()).settled)
        assertEquals(15, store.energy)
    }

    @Test fun `only one active session at a time`() = runTest {
        c.start("", 25, "e")
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { c.start("", 25, "e") } }
    }

    @Test fun `starting a new session settles a stale finished one first`() = runTest {
        c.start("", 15, "e"); clock.advance(60 * MINUTE)
        val second = c.start("", 25, "e")
        assertEquals("s2", second.id); assertEquals(15, store.energy)
    }

    @Test fun `end early gives partial reward and closes the session`() = runTest {
        c.start("", 60, "e"); clock.advance(20 * MINUTE)
        val r = c.endEarly() as ReconcileResult.Closed
        assertEquals(SessionState.ABANDONED, r.session.state)
        assertEquals(10, r.reward.energy)
        assertNull(store.openSession())
    }

    @Test fun `end early after time is up completes instead`() = runTest {
        c.start("", 15, "e"); clock.advance(16 * MINUTE)
        val r = c.endEarly() as ReconcileResult.Closed
        assertEquals(SessionState.COMPLETED, r.session.state); assertEquals(15, store.energy)
    }

    @Test fun `pause and resume through coordinator persist`() = runTest {
        c.start("", 25, "e"); clock.advance(MINUTE)
        assertEquals(SessionState.PAUSED, c.pause()!!.state)
        assertEquals(SessionState.PAUSED, store.openSession()!!.state)
        clock.advance(MINUTE)
        assertEquals(SessionState.RUNNING, c.resume()!!.state)
    }

    @Test fun `pause limit violation is settled on reconcile`() = runTest {
        c.start("", 60, "e"); clock.advance(30 * MINUTE); c.pause(); clock.advance(40 * MINUTE)
        val r = c.reconcile() as ReconcileResult.Closed
        assertEquals(EndReason.PAUSE_LIMIT_EXCEEDED, r.session.endReason)
        assertEquals(15, store.energy) // 30 focused minutes, half rate
    }

    @Test fun `daily limiter uses minutes already rewarded that day`() = runTest {
        repeat(4) { c.start("", 90, "e"); clock.advance(90 * MINUTE); c.reconcile() } // 360 min
        assertEquals(360, store.energy)
        c.start("", 60, "e"); clock.advance(60 * MINUTE)
        val r = c.reconcile() as ReconcileResult.Closed
        assertEquals(30, r.reward.energy); assertTrue(r.reward.restLimited)
    }
}
