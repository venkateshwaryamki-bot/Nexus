package com.nexus.domain.focus

enum class SessionState { RUNNING, PAUSED, COMPLETED, ABANDONED;
    val isOpen get() = this == RUNNING || this == PAUSED
}

enum class EndReason {
    /** Full planned duration focused. */
    COMPLETED,
    /** User chose to end the session early. */
    ENDED_BY_USER,
    /** Paused for longer than the rules allow. */
    PAUSE_LIMIT_EXCEEDED,
}

/**
 * Persistent record of a focus session. All progress is derived from timestamps, never from a
 * ticking UI timer, so the session survives backgrounding, process death and (approximately) reboots.
 *
 * Time model: focused time = [bankedFocusMs] (closed segments) + the open segment, if RUNNING.
 * An open segment is anchored on both clocks so that it can be measured monotonically within a boot
 * and bridged with the wall clock across a reboot.
 */
data class FocusSession(
    val id: String,
    val task: String,
    val plannedMs: Long,
    val environmentId: String,
    val createdWall: Long,
    val state: SessionState,
    val bankedFocusMs: Long = 0,
    val segmentStartWall: Long? = null,
    val segmentStartElapsed: Long? = null,
    val segmentBootId: String? = null,
    val pauseCount: Int = 0,
    val pausedAtWall: Long? = null,
    val pausedAtElapsed: Long? = null,
    val pausedBootId: String? = null,
    val totalPausedMs: Long = 0,
    /** Times the user left the app while the session was running (tracked, not penalised). */
    val interruptions: Int = 0,
    val endedWall: Long? = null,
    val endReason: EndReason? = null,
    /** Some focus time was measured with the wall clock because the device rebooted. */
    val recoveredAcrossReboot: Boolean = false,
    /** The wall clock was changed while running; monotonic time was used instead. */
    val clockAnomaly: Boolean = false,
) {
    val plannedMinutes: Int get() = (plannedMs / FocusRules.MINUTE).toInt()
}
