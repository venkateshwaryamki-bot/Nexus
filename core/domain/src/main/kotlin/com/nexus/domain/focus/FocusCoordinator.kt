package com.nexus.domain.focus

import com.nexus.domain.repository.ProgressionRepository
import com.nexus.domain.repository.SessionRepository
import com.nexus.domain.reward.Reward
import com.nexus.domain.reward.RewardPolicy
import com.nexus.domain.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface ReconcileResult {
    data object NoSession : ReconcileResult
    data class StillOpen(val session: FocusSession, val evaluation: SessionEvaluation) : ReconcileResult
    /** The session was closed by this call (and rewarded if eligible). */
    data class Closed(val session: FocusSession, val reward: Reward, val newlySettled: Boolean) : ReconcileResult
}

/**
 * Use-case layer for focus sessions. Every entry point (UI, alarm, boot, notification actions)
 * goes through here. All mutations are serialised with a mutex in-process, and settlement is
 * additionally idempotent at the database level, so duplicate rewards are impossible.
 */
class FocusCoordinator(
    private val sessions: SessionRepository,
    private val progression: ProgressionRepository,
    private val clock: Clock,
    private val engine: FocusTimerEngine = FocusTimerEngine(),
    private val policy: RewardPolicy = RewardPolicy.Default,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val idFactory: () -> String = { java.util.UUID.randomUUID().toString() },
) {
    private val mutex = Mutex()

    val timer: FocusTimerEngine get() = engine

    suspend fun start(task: String, minutes: Int, environmentId: String): FocusSession = mutex.withLock {
        reconcileLocked() // settle any stale session first
        check(sessions.openSession() == null) { "A focus session is already active" }
        engine.start(idFactory(), task, minutes, environmentId, clock).also { sessions.insertNew(it) }
    }

    suspend fun pause(): FocusSession? = mutate { engine.pause(it, clock) }
    suspend fun resume(): FocusSession? = mutate { engine.resume(it, clock) }
    suspend fun recordInterruption(): FocusSession? = mutate { engine.recordInterruption(it) }

    /** User ends the session early. Returns the settlement (partial reward if eligible). */
    suspend fun endEarly(): ReconcileResult = mutex.withLock {
        val open = sessions.openSession() ?: return@withLock ReconcileResult.NoSession
        // If the time is actually up, completing beats abandoning.
        if (engine.evaluate(open, clock) == SessionEvaluation.DueForCompletion) return@withLock closeAndSettle(engine.complete(open, clock))
        closeAndSettle(engine.abandon(open, EndReason.ENDED_BY_USER, clock))
    }

    /** Brings the stored session up to date with the clock. Safe to call any time, from anywhere. */
    suspend fun reconcile(): ReconcileResult = mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked(): ReconcileResult {
        val open = sessions.openSession() ?: return ReconcileResult.NoSession
        return when (val ev = engine.evaluate(open, clock)) {
            SessionEvaluation.DueForCompletion -> closeAndSettle(engine.complete(open, clock))
            is SessionEvaluation.DueForTermination -> closeAndSettle(engine.abandon(open, ev.reason, clock))
            else -> ReconcileResult.StillOpen(open, ev)
        }
    }

    private suspend fun closeAndSettle(closed: FocusSession): ReconcileResult.Closed {
        val day = Instant.ofEpochMilli(closed.endedWall ?: clock.wallMillis()).atZone(zone()).toLocalDate()
        val earlier = progression.rewardedFocusMinutesOn(day)
        val reward = policy.rewardFor(closed, earlier)
        val result = progression.settle(closed, reward, day)
        return ReconcileResult.Closed(closed, if (result.settled) reward else result.reward, result.settled)
    }

    private suspend fun mutate(block: (FocusSession) -> FocusSession): FocusSession? = mutex.withLock {
        val r = reconcileLocked()
        if (r !is ReconcileResult.StillOpen) return@withLock null
        val updated = block(r.session)
        if (sessions.updateOpen(updated)) updated else null
    }

    fun today(): LocalDate = Instant.ofEpochMilli(clock.wallMillis()).atZone(zone()).toLocalDate()
}
