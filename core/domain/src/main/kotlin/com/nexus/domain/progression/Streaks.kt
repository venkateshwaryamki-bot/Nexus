package com.nexus.domain.progression

import java.time.LocalDate

data class StreakInfo(
    val current: Int,
    val longest: Int,
    /** Shields left to bridge a single missed day without breaking the streak. */
    val shieldsAvailable: Int,
    val activeToday: Boolean,
)

/**
 * Forgiving streaks: every 7 consecutive active days earns a Streak Shield (max 2). A shield is
 * automatically spent to bridge one missed day. A streak is never broken by "today" not being
 * active yet — the user still has the rest of the day.
 */
object Streaks {
    const val SHIELD_EVERY_DAYS = 7
    const val MAX_SHIELDS = 2

    fun compute(activeDays: Set<LocalDate>, today: LocalDate): StreakInfo {
        if (activeDays.isEmpty()) return StreakInfo(0, 0, 0, false)
        val sorted = activeDays.filter { !it.isAfter(today) }.sorted()
        // Walk forward through history, simulating shield earning/spending.
        var run = 0; var longest = 0; var shields = 0; var sinceShield = 0
        var prev: LocalDate? = null
        for (day in sorted) {
            if (prev != null) {
                val gap = java.time.temporal.ChronoUnit.DAYS.between(prev, day) - 1
                if (gap > 0) {
                    if (gap == 1L && shields > 0) shields-- else { run = 0; shields = 0; sinceShield = 0 }
                }
            }
            run++; sinceShield++
            if (sinceShield >= SHIELD_EVERY_DAYS) { sinceShield = 0; shields = (shields + 1).coerceAtMost(MAX_SHIELDS) }
            longest = maxOf(longest, run)
            prev = day
        }
        val last = prev!!
        val daysSinceLast = java.time.temporal.ChronoUnit.DAYS.between(last, today)
        val current = when {
            daysSinceLast <= 1 -> run                    // active today or yesterday
            daysSinceLast == 2L && shields > 0 -> run    // one missed day, a shield will cover it
            else -> 0
        }
        return StreakInfo(current, longest, if (current == 0) 0 else shields, daysSinceLast == 0L)
    }
}
