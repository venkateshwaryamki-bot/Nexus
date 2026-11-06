package com.nexus.domain

import com.nexus.domain.civilization.Civilization
import com.nexus.domain.civilization.CivilizationStage
import com.nexus.domain.focus.*
import com.nexus.domain.focus.FocusRules.Companion.MINUTE
import com.nexus.domain.progression.Progression
import com.nexus.domain.progression.Rank
import com.nexus.domain.progression.Streaks
import com.nexus.domain.reward.RewardKind
import com.nexus.domain.reward.RewardPolicy
import com.nexus.domain.time.FakeClock
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RewardAndProgressionTest {
    private val policy = RewardPolicy.Default
    private val engine = FocusTimerEngine()
    private val clock = FakeClock()

    private fun completed(min: Int): FocusSession {
        val s = engine.start("id", "", min, "e", clock); clock.advance(min * MINUTE); return engine.complete(s, clock)
    }
    private fun abandonedAfter(min: Int, planned: Int = 60): FocusSession {
        val s = engine.start("id", "", planned, "e", clock); clock.advance(min * MINUTE)
        return engine.abandon(s, EndReason.ENDED_BY_USER, clock)
    }

    @Test fun `energy equals minutes for completed sessions`() {
        for (m in listOf(15, 25, 45, 60, 90)) {
            val r = policy.rewardFor(completed(m), 0)
            assertEquals(m, r.energy); assertEquals(m * 2 + 25, r.xp); assertEquals(RewardKind.COMPLETED, r.kind)
        }
    }

    @Test fun `credits are earned only for completed sessions`() {
        assertEquals(5, policy.rewardFor(completed(25), 0).credits)
        assertEquals(12, policy.rewardFor(completed(60), 0).credits)
        assertEquals(0, policy.rewardFor(abandonedAfter(30), 0).credits)
        assertEquals(0, policy.rewardFor(completed(60), 600).credits) // past the daily cap
    }

    @Test fun `ending early before ten minutes earns nothing`() {
        assertEquals(RewardKind.NONE, policy.rewardFor(abandonedAfter(9), 0).kind)
    }

    @Test fun `ending early after ten minutes earns half energy`() {
        val r = policy.rewardFor(abandonedAfter(30), 0)
        assertEquals(15, r.energy); assertEquals(30, r.xp); assertEquals(RewardKind.PARTIAL, r.kind)
    }

    @Test fun `daily limiter reduces then caps energy but not XP`() {
        val r1 = policy.rewardFor(completed(60), 330) // 30 full + 30 half
        assertEquals(45, r1.energy); assertTrue(r1.restLimited); assertEquals(145, r1.xp)
        val r2 = policy.rewardFor(completed(60), 600)
        assertEquals(0, r2.energy)
        assertFalse(policy.rewardFor(completed(60), 0).restLimited)
    }

    @Test fun `level curve`() {
        assertEquals(0, Progression.totalXpForLevel(1))
        assertEquals(100, Progression.totalXpForLevel(2))
        assertEquals(250, Progression.totalXpForLevel(3))
        assertEquals(1, Progression.levelFor(99).level)
        assertEquals(2, Progression.levelFor(100).level)
        val l = Progression.levelFor(175)
        assertEquals(2, l.level); assertEquals(75, l.xpIntoLevel); assertEquals(150, l.xpForNextLevel)
    }

    @Test fun `ranks map from levels`() {
        assertEquals(Rank.RECRUIT, Rank.forLevel(4)); assertEquals(Rank.EXPLORER, Rank.forLevel(5))
        assertEquals(Rank.COMMANDER, Rank.forLevel(10)); assertEquals(Rank.NEXUS_LEGEND, Rank.forLevel(80))
    }

    @Test fun `civilization stages`() {
        assertEquals(CivilizationStage.ABANDONED_OUTPOST, Civilization.statusFor(0).stage)
        assertEquals(CivilizationStage.SMALL_SETTLEMENT, Civilization.statusFor(60).stage)
        val s = Civilization.statusFor(180)
        assertEquals(0.5f, s.progressToNext, 1e-4f)
        assertEquals(1f, Civilization.statusFor(99999).progressToNext)
        assertNull(Civilization.statusFor(99999).nextStage)
        assertEquals("negative input is clamped", CivilizationStage.ABANDONED_OUTPOST, Civilization.statusFor(-5).stage)
        assertEquals(1, Progression.levelFor(-10).level)
    }

    @Test fun `streaks are forgiving`() {
        val today = LocalDate.of(2026, 11, 6)
        fun days(vararg back: Int) = back.map { today.minusDays(it.toLong()) }.toSet()
        assertEquals(0, Streaks.compute(emptySet(), today).current)
        // Today not active yet: yesterday's streak still counts.
        assertEquals(3, Streaks.compute(days(1, 2, 3), today).current)
        // Gap of two days without shields breaks it.
        assertEquals(0, Streaks.compute(days(3, 4, 5), today).current)
        // 7-day run earns a shield, which bridges one missed day.
        val withGap = days(0, 2, 3, 4, 5, 6, 7, 8)
        val info = Streaks.compute(withGap, today)
        assertEquals(8, info.current); assertEquals(0, info.shieldsAvailable); assertTrue(info.activeToday)
        assertEquals(8, info.longest)
    }
}
