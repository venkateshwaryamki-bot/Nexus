package com.nexus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.domain.focus.FocusCoordinator
import com.nexus.domain.focus.FocusRules.Companion.MINUTE
import com.nexus.domain.focus.ReconcileResult
import com.nexus.domain.focus.SessionState
import com.nexus.domain.reward.Reward
import com.nexus.domain.reward.RewardKind
import com.nexus.domain.time.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettlementTest {
    private lateinit var db: NexusDatabase
    private val clock = FakeClock()
    private lateinit var coordinator: FocusCoordinator
    private lateinit var progression: RoomProgressionRepository
    private lateinit var players: PlayerRepository

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), NexusDatabase::class.java)
            .allowMainThreadQueries().build()
        progression = RoomProgressionRepository(db) { clock.wall }
        players = PlayerRepository(db) { clock.wall }
        players.ensureInitialized()
        coordinator = FocusCoordinator(RoomSessionRepository(db), progression, clock, zone = { ZoneOffset.UTC })
    }

    @After fun tearDown() = db.close()

    @Test fun `completed session is persisted and rewarded once`() = runBlocking {
        val s = coordinator.start("Study", 25, "command_centre")
        clock.advance(25 * MINUTE)
        val results = (1..10).map { async(Dispatchers.IO) { coordinator.reconcile() } }.awaitAll()
        assertEquals(1, results.count { it is ReconcileResult.Closed && it.newlySettled })
        val p = db.player().get()!!
        assertEquals(25, p.energyBalance); assertEquals(25, p.lifetimeEnergy); assertEquals(75, p.totalXp)
        assertEquals(1, db.ledger().count())
        assertEquals(SessionState.COMPLETED.name, db.sessions().stateOf(s.id))
        val snap = players.observePlayer().first()!!
        assertEquals(25, snap.civilization.developmentEnergy)
    }

    @Test fun `direct duplicate settle is rejected by the database`() = runBlocking {
        val s = coordinator.start("", 15, "e"); clock.advance(15 * MINUTE)
        val closed = coordinator.timer.complete(s, clock)
        val day = LocalDate.of(2026, 1, 1)
        assertTrue(progression.settle(closed, Reward(15, 55, RewardKind.COMPLETED), day).settled)
        val again = progression.settle(closed, Reward(999, 999, RewardKind.COMPLETED), day)
        assertFalse(again.settled); assertEquals(15, again.reward.energy)
        assertEquals(15, db.player().get()!!.energyBalance)
    }

    @Test fun `open session survives a new database handle (process death)`() = runBlocking {
        coordinator.start("Code", 45, "e"); clock.advance(10 * MINUTE)
        val reopened = FocusCoordinator(RoomSessionRepository(db), progression, clock, zone = { ZoneOffset.UTC })
        val r = reopened.reconcile() as ReconcileResult.StillOpen
        assertEquals(35 * MINUTE, reopened.timer.remainingMs(r.session, clock))
    }

    @Test fun `stats aggregate by day and streak counts`() = runBlocking {
        repeat(2) { coordinator.start("", 25, "e"); clock.advance(25 * MINUTE); coordinator.reconcile() }
        coordinator.start("", 60, "e"); clock.advance(12 * MINUTE); coordinator.endEarly()
        val stats = players.observeStats { LocalDate.ofEpochDay(clock.wall / 86_400_000) }.first()
        assertEquals(2, stats.completedSessions); assertEquals(1, stats.endedEarlySessions)
        assertEquals((50 + 12) * MINUTE, stats.totalFocusMs)
        assertEquals(25 * MINUTE, stats.longestSessionMs)
        assertEquals(1, stats.streak.current)
        assertEquals(56, db.player().get()!!.energyBalance) // 25 + 25 + 6
    }
}
