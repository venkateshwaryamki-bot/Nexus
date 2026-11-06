package com.nexus.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** An existing v1 install must upgrade to v2 (credits) without losing any progress. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NexusDatabase::class.java)

    @Test fun v1ToLatestKeepsProgress() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO player (id, username, avatarId, createdAtWall, energyBalance, lifetimeEnergy, totalXp, onboardingComplete, accountType) VALUES (1, 'Nova', 2, 0, 125, 300, 900, 1, 'guest')")
            execSQL("INSERT INTO reward_ledger (sessionId, energy, xp, kind, restLimited, focusMinutes, dayKey, grantedAtWall) VALUES ('s1', 25, 75, 'COMPLETED', 0, 25, '2026-11-01', 0)")
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true).close()
        helper.runMigrationsAndValidate(dbName, 3, true).close()
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), NexusDatabase::class.java, dbName).allowMainThreadQueries().build()
        runBlocking {
            val p = db.player().get()!!
            assertEquals("Nova", p.username); assertEquals(125, p.energyBalance); assertEquals(900, p.totalXp); assertEquals(0, p.credits)
            assertEquals(0, db.ledger().get("s1")!!.credits)
        }
        db.close()
    }
}
