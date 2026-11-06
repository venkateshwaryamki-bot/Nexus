package com.nexus.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nexus.domain.focus.EndReason
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.SessionState

/** Single-row player profile and wallet. */
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Explorer",
    val avatarId: Int = 0,
    val createdAtWall: Long,
    val energyBalance: Long = 0,
    val lifetimeEnergy: Long = 0,
    val totalXp: Long = 0,
    val onboardingComplete: Boolean = false,
    /** "guest" in the local-first MVP. Future: "google", "email". */
    val accountType: String = "guest",
)

@Entity(tableName = "focus_session", indices = [Index("state"), Index("endedWall")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val task: String,
    val plannedMs: Long,
    val environmentId: String,
    val createdWall: Long,
    val state: String,
    val bankedFocusMs: Long,
    val segmentStartWall: Long?,
    val segmentStartElapsed: Long?,
    val segmentBootId: String?,
    val pauseCount: Int,
    val pausedAtWall: Long?,
    val pausedAtElapsed: Long?,
    val pausedBootId: String?,
    val totalPausedMs: Long,
    val interruptions: Int,
    val endedWall: Long?,
    val endReason: String?,
    val recoveredAcrossReboot: Boolean,
    val clockAnomaly: Boolean,
) {
    fun toDomain() = FocusSession(
        id, task, plannedMs, environmentId, createdWall, SessionState.valueOf(state), bankedFocusMs,
        segmentStartWall, segmentStartElapsed, segmentBootId, pauseCount, pausedAtWall, pausedAtElapsed,
        pausedBootId, totalPausedMs, interruptions, endedWall, endReason?.let { EndReason.valueOf(it) },
        recoveredAcrossReboot, clockAnomaly,
    )

    companion object {
        fun from(s: FocusSession) = SessionEntity(
            s.id, s.task, s.plannedMs, s.environmentId, s.createdWall, s.state.name, s.bankedFocusMs,
            s.segmentStartWall, s.segmentStartElapsed, s.segmentBootId, s.pauseCount, s.pausedAtWall,
            s.pausedAtElapsed, s.pausedBootId, s.totalPausedMs, s.interruptions, s.endedWall,
            s.endReason?.name, s.recoveredAcrossReboot, s.clockAnomaly,
        )
    }
}

/**
 * One row per settled session. The primary key on [sessionId] is the database-level guarantee
 * that a session can never be rewarded twice.
 */
@Entity(tableName = "reward_ledger")
data class RewardLedgerEntity(
    @PrimaryKey val sessionId: String,
    val energy: Int,
    val xp: Int,
    val kind: String,
    val restLimited: Boolean,
    val focusMinutes: Int,
    val dayKey: String,
    val grantedAtWall: Long,
)

/** Pre-aggregated daily statistics (local calendar day, ISO yyyy-MM-dd). */
@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    @PrimaryKey val dayKey: String,
    val focusMs: Long = 0,
    val completedSessions: Int = 0,
    val endedEarlySessions: Int = 0,
    val energyEarned: Int = 0,
    val xpEarned: Int = 0,
    /** Focus minutes that went through the reward formula (input to the daily limiter). */
    val rewardedMinutes: Int = 0,
)

@Entity(tableName = "planet_progress")
data class PlanetProgressEntity(
    @PrimaryKey val planetId: String,
    val unlocked: Boolean,
    val developmentEnergy: Long = 0,
)

@Entity(tableName = "setting")
data class SettingEntity(@PrimaryKey val key: String, val value: String)
