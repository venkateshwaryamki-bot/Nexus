package com.nexus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1") fun observe(): Flow<PlayerEntity?>
    @Query("SELECT * FROM player WHERE id = 1") suspend fun get(): PlayerEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfAbsent(p: PlayerEntity)
    @Query("UPDATE player SET username = :name, avatarId = :avatar, onboardingComplete = 1 WHERE id = 1")
    suspend fun completeOnboarding(name: String, avatar: Int)
    @Query("UPDATE player SET energyBalance = energyBalance + :energy, lifetimeEnergy = lifetimeEnergy + :energy, totalXp = totalXp + :xp WHERE id = 1")
    suspend fun grant(energy: Int, xp: Int): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM focus_session WHERE state IN ('RUNNING','PAUSED') ORDER BY createdWall DESC LIMIT 1")
    fun observeOpen(): Flow<SessionEntity?>
    @Query("SELECT * FROM focus_session WHERE state IN ('RUNNING','PAUSED') ORDER BY createdWall DESC LIMIT 1")
    suspend fun open(): SessionEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(s: SessionEntity)
    @Query("SELECT state FROM focus_session WHERE id = :id") suspend fun stateOf(id: String): String?
    @androidx.room.Update suspend fun update(s: SessionEntity): Int
    @Query("SELECT * FROM focus_session WHERE state IN ('COMPLETED','ABANDONED') ORDER BY endedWall DESC LIMIT :limit")
    suspend fun recentClosed(limit: Int): List<SessionEntity>
    @Query("SELECT * FROM focus_session WHERE state IN ('COMPLETED','ABANDONED') ORDER BY endedWall DESC LIMIT :limit")
    fun observeRecentClosed(limit: Int): Flow<List<SessionEntity>>
    @Query("SELECT COALESCE(MAX(bankedFocusMs),0) FROM focus_session WHERE state = 'COMPLETED'")
    fun observeLongestCompletedMs(): Flow<Long>
}

@Dao
interface LedgerDao {
    /** ABORT: inserting a second reward for the same session throws and rolls back the transaction. */
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(e: RewardLedgerEntity)
    @Query("SELECT * FROM reward_ledger WHERE sessionId = :id") suspend fun get(id: String): RewardLedgerEntity?
    @Query("SELECT COUNT(*) FROM reward_ledger") suspend fun count(): Int
}

@Dao
interface DailyStatDao {
    @Query("SELECT * FROM daily_stat WHERE dayKey = :day") suspend fun get(day: String): DailyStatEntity?
    @Upsert suspend fun upsert(e: DailyStatEntity)
    @Query("SELECT * FROM daily_stat ORDER BY dayKey DESC LIMIT :limit") fun observeRecent(limit: Int): Flow<List<DailyStatEntity>>
    @Query("SELECT dayKey FROM daily_stat WHERE completedSessions > 0") fun observeActiveDays(): Flow<List<String>>
    @Query("SELECT COALESCE(SUM(focusMs),0) AS focusMs, COALESCE(SUM(completedSessions),0) AS completed, COALESCE(SUM(endedEarlySessions),0) AS endedEarly FROM daily_stat")
    fun observeTotals(): Flow<Totals>
}

data class Totals(val focusMs: Long, val completed: Int, val endedEarly: Int)

@Dao
interface PlanetDao {
    @Query("SELECT * FROM planet_progress WHERE planetId = :id") fun observe(id: String): Flow<PlanetProgressEntity?>
    @Query("SELECT * FROM planet_progress WHERE planetId = :id") suspend fun get(id: String): PlanetProgressEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfAbsent(p: PlanetProgressEntity)
    @Query("UPDATE planet_progress SET developmentEnergy = developmentEnergy + :energy WHERE planetId = :id")
    suspend fun addEnergy(id: String, energy: Int)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM setting WHERE `key` = :key") suspend fun get(key: String): String?
    @Query("SELECT * FROM setting") fun observeAll(): Flow<List<SettingEntity>>
    @Upsert suspend fun put(e: SettingEntity)
}
