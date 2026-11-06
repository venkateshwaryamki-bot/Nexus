package com.nexus.app.focus

import com.nexus.app.platform.PlatformStatus
import com.nexus.domain.focus.FocusCoordinator
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.ReconcileResult
import com.nexus.domain.focus.SessionEvaluation
import com.nexus.domain.focus.SessionState
import com.nexus.domain.repository.SessionRepository
import com.nexus.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android glue around the pure [FocusCoordinator]: after every state change it re-syncs the
 * system-level side effects (completion alarm + ongoing notification). Nothing here holds timer
 * state; it can be torn down by process death at any moment and rebuilt from the database.
 */
class FocusSystem(
    private val coordinator: FocusCoordinator,
    private val sessions: SessionRepository,
    private val alarms: FocusAlarmScheduler,
    private val notifier: FocusNotifier,
    private val clock: Clock,
    private val platform: PlatformStatus,
) {
    val engine get() = coordinator.timer

    /**
     * The most recent newly-settled session, whoever settled it (UI, alarm, boot). The UI shows its
     * debrief and acknowledges it. Process-lifetime only; the reward itself is already persisted.
     */
    private val _unseenResult = MutableStateFlow<ReconcileResult.Closed?>(null)
    val unseenResult: StateFlow<ReconcileResult.Closed?> = _unseenResult
    fun acknowledgeResult() { _unseenResult.value = null }
    fun observeOpenSession(): Flow<FocusSession?> = sessions.observeOpenSession()

    suspend fun start(task: String, minutes: Int, environmentId: String): FocusSession =
        coordinator.start(task, minutes, environmentId).also { sync(it) }

    suspend fun pause(): FocusSession? = coordinator.pause()?.also { sync(it) }
    suspend fun resume(): FocusSession? = coordinator.resume()?.also { sync(it) }
    suspend fun recordInterruption() { coordinator.recordInterruption() }

    suspend fun endEarly(): ReconcileResult = coordinator.endEarly().also { afterReconcile(it, fromBackground = false) }

    /** Called from UI, alarm, boot and app-start. Idempotent. */
    suspend fun reconcileAndSync(fromBackground: Boolean = false): ReconcileResult =
        coordinator.reconcile().also { afterReconcile(it, fromBackground) }

    private fun afterReconcile(r: ReconcileResult, fromBackground: Boolean) {
        when (r) {
            is ReconcileResult.StillOpen -> sync(r.session)
            is ReconcileResult.Closed -> {
                alarms.cancel(); notifier.cancelOngoing()
                if (r.newlySettled) _unseenResult.value = r
                if (r.newlySettled && (fromBackground || !platform.isAppInForeground())) notifier.showResult(r)
            }
            ReconcileResult.NoSession -> { alarms.cancel(); notifier.cancelOngoing() }
        }
    }

    private fun sync(s: FocusSession) {
        when (val ev = engine.evaluate(s, clock)) {
            is SessionEvaluation.Running -> {
                alarms.scheduleIn(ev.remainingMs)
                notifier.showRunning(s, ev.remainingMs, canPause = engine.canPause(s))
            }
            is SessionEvaluation.Paused -> {
                // Wake up when the pause allowance runs out so the session can be closed on time.
                alarms.scheduleIn(ev.pauseRemainingMs)
                notifier.showPaused(s, ev.remainingMs, ev.pauseRemainingMs)
            }
            else -> if (s.state == SessionState.RUNNING || s.state == SessionState.PAUSED) alarms.scheduleIn(0)
        }
    }
}
