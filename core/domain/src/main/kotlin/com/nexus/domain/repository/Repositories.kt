package com.nexus.domain.repository

import com.nexus.domain.focus.FocusSession
import com.nexus.domain.reward.Reward
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Storage contracts. The Android implementation is Room (core/data). A future cloud backend would
 * sit behind these same interfaces (sync layer) without touching domain or UI code.
 */
interface SessionRepository {
    fun observeOpenSession(): Flow<FocusSession?>
    suspend fun openSession(): FocusSession?
    suspend fun insertNew(session: FocusSession)
    /** Updates an OPEN session. Returns false if the session is no longer open (already settled). */
    suspend fun updateOpen(session: FocusSession): Boolean
    suspend fun recentSessions(limit: Int): List<FocusSession>
    fun observeRecentSessions(limit: Int): Flow<List<FocusSession>>
}

data class SettlementResult(val settled: Boolean, val reward: Reward)

interface ProgressionRepository {
    /**
     * Atomically closes [closedSession] and grants [reward] exactly once.
     * Implementations MUST make the transition open → closed and the reward grant a single
     * transaction guarded by the session id, so concurrent or repeated calls (UI + alarm receiver +
     * boot receiver) can never double-pay. Returns settled=false if it was already settled.
     */
    suspend fun settle(closedSession: FocusSession, reward: Reward, day: LocalDate): SettlementResult

    /** Focus minutes already rewarded on [day] (input to the healthy-play limiter). */
    suspend fun rewardedFocusMinutesOn(day: LocalDate): Int
}

sealed interface BuildResult {
    data class Built(val type: com.nexus.domain.civilization.BuildingType, val level: Int, val xp: Int) : BuildResult
    data class Rejected(val reason: com.nexus.domain.civilization.BuildCheck) : BuildResult
    /** The building changed since the UI read it (e.g. a double tap); nothing was spent. */
    data object Stale : BuildResult
}

interface BuildingRepository {
    fun observeLevels(): kotlinx.coroutines.flow.Flow<Map<com.nexus.domain.civilization.BuildingType, Int>>
    /** Atomically checks unlock + funds, spends, and raises the level from [expectedLevel] by one. */
    suspend fun build(type: com.nexus.domain.civilization.BuildingType, expectedLevel: Int): BuildResult
}
