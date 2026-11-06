package com.nexus.domain.reward

import com.nexus.domain.focus.EndReason
import com.nexus.domain.focus.FocusRules
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.SessionState

enum class RewardKind { COMPLETED, PARTIAL, NONE }

data class Reward(val energy: Int, val xp: Int, val kind: RewardKind, val restLimited: Boolean = false, val credits: Int = 0) {
    companion object { val None = Reward(0, 0, RewardKind.NONE) }
}

/**
 * Transparent, configurable reward formula (see GAME_DESIGN.md §Energy).
 *
 * Completed session: 1 energy per planned minute, 2 XP per minute + a completion bonus.
 * Ended early: once [FocusRules.partialRewardMinFocusMs] is banked, half energy and 1 XP per focused minute.
 *
 * Credits: earned only by *completing* a session — 1 per [minutesPerCredit] planned minutes (25 min → 5).
 * They are never granted for partial sessions and are not affected by the limiter's energy reduction,
 * but stop once the daily energy cap is reached.
 *
 * Healthy-play limiter: focus beyond [fullRateDailyMinutes] in one day earns energy at a reduced
 * rate and beyond [dailyEnergyCapMinutes] earns none. Time is still recorded in statistics. This keeps
 * competition about consistency rather than overwork.
 */
data class RewardPolicy(
    val energyPerMinute: Int = 1,
    val xpPerMinute: Int = 2,
    val completionBonusXp: Int = 25,
    val partialEnergyFactor: Double = 0.5,
    val partialXpPerMinute: Int = 1,
    val fullRateDailyMinutes: Int = 360,
    val reducedRateFactor: Double = 0.5,
    val dailyEnergyCapMinutes: Int = 600,
    val minutesPerCredit: Int = 5,
    val rules: FocusRules = FocusRules.Default,
) {
    /**
     * @param focusedMinutesEarlierToday focus minutes already rewarded today, used by the limiter.
     */
    fun rewardFor(session: FocusSession, focusedMinutesEarlierToday: Int): Reward {
        val focusedMinutes = (session.bankedFocusMs / FocusRules.MINUTE).toInt()
        return when (session.state) {
            SessionState.COMPLETED -> {
                val (energyMinutes, limited) = limitedMinutes(session.plannedMinutes, focusedMinutesEarlierToday)
                Reward(
                    energy = (energyMinutes * energyPerMinute).toInt(),
                    xp = session.plannedMinutes * xpPerMinute + completionBonusXp,
                    kind = RewardKind.COMPLETED,
                    restLimited = limited,
                    credits = if (focusedMinutesEarlierToday >= dailyEnergyCapMinutes) 0 else session.plannedMinutes / minutesPerCredit,
                )
            }
            SessionState.ABANDONED -> {
                if (session.endReason == EndReason.COMPLETED ||
                    session.bankedFocusMs < rules.partialRewardMinFocusMs) return Reward.None
                val (energyMinutes, limited) = limitedMinutes(focusedMinutes, focusedMinutesEarlierToday)
                Reward(
                    energy = (energyMinutes * energyPerMinute * partialEnergyFactor).toInt(),
                    xp = focusedMinutes * partialXpPerMinute,
                    kind = RewardKind.PARTIAL,
                    restLimited = limited,
                )
            }
            else -> Reward.None
        }
    }

    /** Applies the daily limiter. Returns effective full-rate-equivalent minutes and whether it applied. */
    internal fun limitedMinutes(minutes: Int, earlierToday: Int): Pair<Double, Boolean> {
        var effective = 0.0
        var limited = false
        for (m in earlierToday until earlierToday + minutes) {
            effective += when {
                m < fullRateDailyMinutes -> 1.0
                m < dailyEnergyCapMinutes -> { limited = true; reducedRateFactor }
                else -> { limited = true; 0.0 }
            }
        }
        return effective to limited
    }

    companion object { val Default = RewardPolicy() }
}
