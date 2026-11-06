package com.nexus.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerEntity::class, SessionEntity::class, RewardLedgerEntity::class,
        DailyStatEntity::class, PlanetProgressEntity::class, SettingEntity::class, BuildingEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // v2: credits on player + ledger
        AutoMigration(from = 2, to = 3), // v3: building table
    ],
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun player(): PlayerDao
    abstract fun sessions(): SessionDao
    abstract fun ledger(): LedgerDao
    abstract fun daily(): DailyStatDao
    abstract fun planets(): PlanetDao
    abstract fun settings(): SettingDao
    abstract fun buildings(): BuildingDao

    companion object {
        const val NAME = "nexus.db"
        fun create(context: Context): NexusDatabase =
            Room.databaseBuilder(context.applicationContext, NexusDatabase::class.java, NAME)
                // WAL is Room's default on capable devices; explicit for clarity: readers never block the settle txn.
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
