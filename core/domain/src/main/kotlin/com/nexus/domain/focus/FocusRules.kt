package com.nexus.domain.focus

/**
 * The user-facing focus session rules. These are shown verbatim (in friendlier words) in the Focus
 * Chamber so that the reward system is transparent. Changing a value here changes the rules everywhere.
 */
data class FocusRules(
    val minDurationMinutes: Int = 5,
    val maxDurationMinutes: Int = 180,
    /** Pauses allowed per session. */
    val maxPauses: Int = 3,
    /** Total paused time allowed per session; exceeding it ends the session as interrupted. */
    val maxTotalPauseMs: Long = 15 * MINUTE,
    /** Ending early still earns a partial reward once this much focus has been banked. */
    val partialRewardMinFocusMs: Long = 10 * MINUTE,
    /**
     * Within one boot, if the wall clock and monotonic clock disagree by more than this, the wall
     * clock was changed. We keep using the monotonic clock and flag the session.
     */
    val clockSkewToleranceMs: Long = 2 * MINUTE,
) {
    val presetMinutes: List<Int> get() = listOf(15, 25, 45, 60, 90)

    fun isValidDuration(minutes: Int) = minutes in minDurationMinutes..maxDurationMinutes

    companion object {
        const val MINUTE = 60_000L
        val Default = FocusRules()
    }
}
