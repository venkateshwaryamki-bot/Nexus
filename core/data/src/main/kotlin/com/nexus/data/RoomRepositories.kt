package com.nexus.data

import androidx.room.withTransaction
import com.nexus.domain.civilization.Civilization
import com.nexus.domain.civilization.CivilizationStatus
import com.nexus.domain.focus.FocusRules
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.SessionState
import com.nexus.domain.progression.LevelInfo
import com.nexus.domain.progression.Progression
import com.nexus.domain.progression.StreakInfo
import com.nexus.domain.progression.Streaks
import com.nexus.domain.repository.ProgressionRepository
import com.nexus.domain.repository.SessionRepository
import com.nexus.domain.repository.SettlementResult
import com.nexus.domain.reward.Reward
import com.nexus.domain.reward.RewardKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

object Planets { const val NEXUS_PRIME = "nexus_prime" }

class RoomSessionRepository(private val db: NexusDatabase) : SessionRepository {
    private val dao = db.sessions()
    override fun observeOpenSession(): Flow<FocusSession?> = dao.observeOpen().map { it?.toDomain() }
    override suspend fun openSession(): FocusSession? = dao.open()?.toDomain()
    override suspend fun insertNew(session: FocusSession) = dao.insert(SessionEntity.from(session))
    override suspend fun updateOpen(session: FocusSession): Boolean = db.withTransaction {
        val state = dao.stateOf(session.id) ?: return@withTransaction false
        if (!SessionState.valueOf(state).isOpen) return@withTransaction false
        dao.update(SessionEntity.from(session)) == 1
    }
    override suspend fun recentSessions(limit: Int) = dao.recentClosed(limit).map { it.toDomain() }
    override fun observeRecentSessions(limit: Int) = dao.observeRecentClosed(limit).map { l -> l.map { it.toDomain() } }
}

class RoomProgressionRepository(
    private val db: NexusDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : ProgressionRepository {

    override suspend fun settle(closedSession: FocusSession, reward: Reward, day: LocalDate): SettlementResult {
        require(!closedSession.state.isOpen) { "settle() needs a closed session" }
        return db.withTransaction {
            // Guard 1: the session must still be open in the database. Any concurrent or repeated
            // settle (alarm + UI + boot receiver) observes the closed state and exits here.
            val state = db.sessions().stateOf(closedSession.id)
            if (state == null || !SessionState.valueOf(state).isOpen) {
                val prior = db.ledger().get(closedSession.id)
                return@withTransaction SettlementResult(false, prior?.let {
                    Reward(it.energy, it.xp, RewardKind.valueOf(it.kind), it.restLimited, it.credits)
                } ?: Reward.None)
            }
            db.sessions().update(SessionEntity.from(closedSession))
            val minutes = (closedSession.bankedFocusMs / FocusRules.MINUTE).toInt()
            // Guard 2: ledger primary key. A duplicate insert aborts and rolls back the whole txn.
            db.ledger().insert(
                RewardLedgerEntity(closedSession.id, reward.energy, reward.xp, reward.kind.name,
                    reward.restLimited, minutes, day.toString(), now(), reward.credits)
            )
            if (reward.energy > 0 || reward.xp > 0 || reward.credits > 0) {
                db.player().grant(reward.energy, reward.xp, reward.credits)
                db.planets().addEnergy(Planets.NEXUS_PRIME, reward.energy)
            }
            val key = day.toString()
            val d = db.daily().get(key) ?: DailyStatEntity(key)
            val completed = closedSession.state == SessionState.COMPLETED
            db.daily().upsert(d.copy(
                focusMs = d.focusMs + closedSession.bankedFocusMs,
                completedSessions = d.completedSessions + if (completed) 1 else 0,
                endedEarlySessions = d.endedEarlySessions + if (completed) 0 else 1,
                energyEarned = d.energyEarned + reward.energy,
                xpEarned = d.xpEarned + reward.xp,
                rewardedMinutes = d.rewardedMinutes + if (reward.kind != RewardKind.NONE) minutes else 0,
            ))
            SettlementResult(true, reward)
        }
    }

    override suspend fun rewardedFocusMinutesOn(day: LocalDate): Int =
        db.daily().get(day.toString())?.rewardedMinutes ?: 0
}

/** Read models consumed by the UI. */
data class PlayerSnapshot(
    val username: String,
    val avatarId: Int,
    val onboardingComplete: Boolean,
    val energyBalance: Long,
    val lifetimeEnergy: Long,
    val totalXp: Long,
    val credits: Long,
    val level: LevelInfo,
    val civilization: CivilizationStatus,
)

data class ProfileStats(
    val totalFocusMs: Long,
    val completedSessions: Int,
    val endedEarlySessions: Int,
    val longestSessionMs: Long,
    val streak: StreakInfo,
    val recentDays: List<DailyStatEntity>,
) {
    val completionRate: Float get() {
        val total = completedSessions + endedEarlySessions
        return if (total == 0) 0f else completedSessions.toFloat() / total
    }
}

class PlayerRepository(private val db: NexusDatabase, private val now: () -> Long = System::currentTimeMillis) {
    /** Creates the local guest profile and starting planet on first launch. Idempotent. */
    suspend fun ensureInitialized() = db.withTransaction {
        db.player().insertIfAbsent(PlayerEntity(createdAtWall = now()))
        db.planets().insertIfAbsent(PlanetProgressEntity(Planets.NEXUS_PRIME, unlocked = true))
    }

    suspend fun completeOnboarding(username: String, avatarId: Int) =
        db.player().completeOnboarding(username.trim().take(20).ifBlank { "Explorer" }, avatarId)

    fun observePlayer(): Flow<PlayerSnapshot?> =
        db.player().observe().combine(db.planets().observe(Planets.NEXUS_PRIME)) { p, planet -> p?.let { snapshotOf(it, planet) } }

    /** Reads the committed state directly (no flow latency); used right after a settlement. */
    suspend fun snapshot(): PlayerSnapshot? = db.withTransaction {
        db.player().get()?.let { snapshotOf(it, db.planets().get(Planets.NEXUS_PRIME)) }
    }

    private fun snapshotOf(it: PlayerEntity, planet: PlanetProgressEntity?) =
        PlayerSnapshot(it.username, it.avatarId, it.onboardingComplete, it.energyBalance, it.lifetimeEnergy, it.totalXp, it.credits,
            Progression.levelFor(it.totalXp), Civilization.statusFor((planet?.developmentEnergy ?: 0L).toInt()))

    fun observeStats(today: () -> LocalDate): Flow<ProfileStats> = combine(
        db.daily().observeTotals(), db.sessions().observeLongestCompletedMs(),
        db.daily().observeActiveDays(), db.daily().observeRecent(30),
    ) { totals, longest, days, recent ->
        ProfileStats(totals.focusMs, totals.completed, totals.endedEarly, longest,
            Streaks.compute(days.map(LocalDate::parse).toSet(), today()), recent)
    }
}

class SettingsRepository(private val db: NexusDatabase) {
    fun observe(): Flow<Map<String, String>> = db.settings().observeAll().map { l -> l.associate { it.key to it.value } }
    suspend fun put(key: String, value: String) = db.settings().put(SettingEntity(key, value))
    suspend fun get(key: String): String? = db.settings().get(key)

    /** Wipes all progress and returns to a fresh guest profile. */
    suspend fun resetProgress() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { db.clearAllTables() }
}
