package com.nexus.domain.focus

import com.nexus.domain.time.Clock

/** Result of evaluating an open session against the current time. */
sealed interface SessionEvaluation {
    data class Running(val focusedMs: Long, val remainingMs: Long) : SessionEvaluation
    data class Paused(val focusedMs: Long, val remainingMs: Long, val pauseRemainingMs: Long) : SessionEvaluation
    /** The planned duration has been reached; the session must be completed. */
    data object DueForCompletion : SessionEvaluation
    /** A rule was broken while away (e.g. pause limit); the session must be ended. */
    data class DueForTermination(val reason: EndReason) : SessionEvaluation
    data object Closed : SessionEvaluation
}

class IllegalSessionTransition(message: String) : IllegalStateException(message)

/**
 * Pure, deterministic state machine for focus sessions. It holds no state itself; every function
 * takes a session and a [Clock] and returns a new session. This makes it trivially testable and lets
 * the same logic run in the UI, an alarm receiver or a boot receiver.
 */
class FocusTimerEngine(private val rules: FocusRules = FocusRules.Default) {

    fun start(id: String, task: String, minutes: Int, environmentId: String, clock: Clock): FocusSession {
        require(rules.isValidDuration(minutes)) {
            "Duration must be ${rules.minDurationMinutes}-${rules.maxDurationMinutes} minutes, was $minutes"
        }
        val now = clock.wallMillis()
        return FocusSession(
            id = id,
            task = task.trim(),
            plannedMs = minutes * FocusRules.MINUTE,
            environmentId = environmentId,
            createdWall = now,
            state = SessionState.RUNNING,
            segmentStartWall = now,
            segmentStartElapsed = clock.elapsedMillis(),
            segmentBootId = clock.bootId(),
        )
    }

    /** Length of the currently open running segment, and whether it had to be bridged/was suspicious. */
    private data class Segment(val ms: Long, val rebootBridged: Boolean, val anomaly: Boolean)

    private fun openSegment(s: FocusSession, clock: Clock): Segment {
        if (s.state != SessionState.RUNNING) return Segment(0, false, false)
        val startWall = s.segmentStartWall ?: return Segment(0, false, false)
        val wallDelta = clock.wallMillis() - startWall
        return if (s.segmentBootId == clock.bootId() && s.segmentStartElapsed != null) {
            val mono = (clock.elapsedMillis() - s.segmentStartElapsed).coerceAtLeast(0)
            val anomaly = kotlin.math.abs(wallDelta - mono) > rules.clockSkewToleranceMs
            Segment(mono, false, anomaly)
        } else {
            // Device rebooted: the monotonic anchor is gone. Bridge with wall time, never negative.
            Segment(wallDelta.coerceAtLeast(0), true, false)
        }
    }

    fun focusedMs(s: FocusSession, clock: Clock): Long =
        (s.bankedFocusMs + openSegment(s, clock).ms).coerceAtMost(s.plannedMs)

    fun remainingMs(s: FocusSession, clock: Clock): Long = (s.plannedMs - focusedMs(s, clock)).coerceAtLeast(0)

    private fun currentPauseMs(s: FocusSession, clock: Clock): Long {
        if (s.state != SessionState.PAUSED) return 0
        val wallStart = s.pausedAtWall ?: return 0
        return if (s.pausedBootId == clock.bootId() && s.pausedAtElapsed != null) {
            (clock.elapsedMillis() - s.pausedAtElapsed).coerceAtLeast(0)
        } else {
            (clock.wallMillis() - wallStart).coerceAtLeast(0)
        }
    }

    fun evaluate(s: FocusSession, clock: Clock): SessionEvaluation = when (s.state) {
        SessionState.COMPLETED, SessionState.ABANDONED -> SessionEvaluation.Closed
        SessionState.RUNNING -> {
            val focused = s.bankedFocusMs + openSegment(s, clock).ms
            if (focused >= s.plannedMs) SessionEvaluation.DueForCompletion
            else SessionEvaluation.Running(focused, s.plannedMs - focused)
        }
        SessionState.PAUSED -> {
            val pauseLeft = rules.maxTotalPauseMs - s.totalPausedMs - currentPauseMs(s, clock)
            if (pauseLeft <= 0) SessionEvaluation.DueForTermination(EndReason.PAUSE_LIMIT_EXCEEDED)
            else SessionEvaluation.Paused(s.bankedFocusMs, s.plannedMs - s.bankedFocusMs, pauseLeft)
        }
    }

    fun canPause(s: FocusSession) = s.state == SessionState.RUNNING && s.pauseCount < rules.maxPauses

    fun pause(s: FocusSession, clock: Clock): FocusSession {
        if (s.state != SessionState.RUNNING) throw IllegalSessionTransition("Can only pause a running session")
        if (s.pauseCount >= rules.maxPauses) throw IllegalSessionTransition("Pause limit (${rules.maxPauses}) reached")
        val seg = openSegment(s, clock)
        val banked = s.bankedFocusMs + seg.ms
        if (banked >= s.plannedMs) throw IllegalSessionTransition("Session is already due for completion")
        return s.copy(
            state = SessionState.PAUSED,
            bankedFocusMs = banked,
            segmentStartWall = null, segmentStartElapsed = null, segmentBootId = null,
            pauseCount = s.pauseCount + 1,
            pausedAtWall = clock.wallMillis(),
            pausedAtElapsed = clock.elapsedMillis(),
            pausedBootId = clock.bootId(),
            recoveredAcrossReboot = s.recoveredAcrossReboot || seg.rebootBridged,
            clockAnomaly = s.clockAnomaly || seg.anomaly,
        )
    }

    fun resume(s: FocusSession, clock: Clock): FocusSession {
        if (s.state != SessionState.PAUSED) throw IllegalSessionTransition("Can only resume a paused session")
        val pausedFor = currentPauseMs(s, clock)
        if (s.totalPausedMs + pausedFor >= rules.maxTotalPauseMs) {
            throw IllegalSessionTransition("Pause limit exceeded; session must be ended")
        }
        val now = clock.wallMillis()
        return s.copy(
            state = SessionState.RUNNING,
            totalPausedMs = s.totalPausedMs + pausedFor,
            pausedAtWall = null, pausedAtElapsed = null, pausedBootId = null,
            segmentStartWall = now,
            segmentStartElapsed = clock.elapsedMillis(),
            segmentBootId = clock.bootId(),
        )
    }

    /**
     * Closes a session that is due for completion. The completion timestamp is back-dated to the
     * moment the planned duration was actually reached, so a session reconciled hours later (e.g.
     * after the app was killed) is still attributed to the correct day.
     */
    fun complete(s: FocusSession, clock: Clock): FocusSession {
        if (evaluate(s, clock) != SessionEvaluation.DueForCompletion) {
            throw IllegalSessionTransition("Session is not due for completion")
        }
        val seg = openSegment(s, clock)
        val overshoot = (s.bankedFocusMs + seg.ms - s.plannedMs).coerceAtLeast(0)
        return s.copy(
            state = SessionState.COMPLETED,
            bankedFocusMs = s.plannedMs,
            segmentStartWall = null, segmentStartElapsed = null, segmentBootId = null,
            endedWall = clock.wallMillis() - overshoot,
            endReason = EndReason.COMPLETED,
            recoveredAcrossReboot = s.recoveredAcrossReboot || seg.rebootBridged,
            clockAnomaly = s.clockAnomaly || seg.anomaly,
        )
    }

    /** Ends an open session early (user choice or rule violation). Focus banked so far is kept. */
    fun abandon(s: FocusSession, reason: EndReason, clock: Clock): FocusSession {
        if (!s.state.isOpen) throw IllegalSessionTransition("Session already closed")
        require(reason != EndReason.COMPLETED) { "Use complete() for completed sessions" }
        val seg = openSegment(s, clock)
        val endWall = when {
            // A pause-limit termination happened at the moment the allowance ran out, not "now".
            reason == EndReason.PAUSE_LIMIT_EXCEEDED && s.pausedAtWall != null ->
                s.pausedAtWall + (rules.maxTotalPauseMs - s.totalPausedMs)
            else -> clock.wallMillis()
        }
        return s.copy(
            state = SessionState.ABANDONED,
            bankedFocusMs = (s.bankedFocusMs + seg.ms).coerceAtMost(s.plannedMs),
            segmentStartWall = null, segmentStartElapsed = null, segmentBootId = null,
            pausedAtWall = null, pausedAtElapsed = null, pausedBootId = null,
            endedWall = endWall.coerceAtMost(clock.wallMillis()),
            endReason = reason,
            recoveredAcrossReboot = s.recoveredAcrossReboot || seg.rebootBridged,
            clockAnomaly = s.clockAnomaly || seg.anomaly,
        )
    }

    fun recordInterruption(s: FocusSession): FocusSession =
        if (s.state == SessionState.RUNNING) s.copy(interruptions = s.interruptions + 1) else s
}
