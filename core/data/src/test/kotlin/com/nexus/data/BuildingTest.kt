package com.nexus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.domain.civilization.BuildCheck
import com.nexus.domain.civilization.BuildingType
import com.nexus.domain.repository.BuildResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BuildingTest {
    private lateinit var db: NexusDatabase
    private lateinit var repo: RoomBuildingRepository

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), NexusDatabase::class.java).allowMainThreadQueries().build()
        PlayerRepository(db).ensureInitialized()
        repo = RoomBuildingRepository(db)
    }
    @After fun tearDown() = db.close()

    private fun fund(energy: Int, credits: Int, dev: Int = 0) = runBlocking {
        db.player().grant(energy, 0, credits)
        db.openHelper.writableDatabase.execSQL("UPDATE planet_progress SET developmentEnergy = $dev")
    }

    @Test fun `building spends balance, adds XP, keeps planet development`() = runBlocking {
        fund(100, 0, dev = 100)
        val r = repo.build(BuildingType.REACTOR, 0) as BuildResult.Built
        assertEquals(1, r.level)
        val p = db.player().get()!!
        assertEquals(70, p.energyBalance); assertEquals(100, p.lifetimeEnergy); assertEquals(20, p.totalXp)
        assertEquals(mapOf(BuildingType.REACTOR to 1), repo.observeLevels().first())
        assertEquals(100L, db.planets().get(Planets.NEXUS_PRIME)!!.developmentEnergy)
    }

    @Test fun `insufficient funds and locked stage are rejected without spending`() = runBlocking {
        fund(10, 0)
        assertTrue((repo.build(BuildingType.REACTOR, 0) as BuildResult.Rejected).reason is BuildCheck.NotEnough)
        fund(5000, 500, dev = 0)
        assertTrue((repo.build(BuildingType.SPACEPORT, 0) as BuildResult.Rejected).reason is BuildCheck.Locked)
        assertEquals(5010, db.player().get()!!.energyBalance)
    }

    @Test fun `double tap only builds once`() = runBlocking {
        fund(1000, 100, dev = 400)
        val results = (1..8).map { async(Dispatchers.IO) { repo.build(BuildingType.LAB, 0) } }.awaitAll()
        assertEquals(1, results.count { it is BuildResult.Built })
        assertEquals(1000 - 60, db.player().get()!!.energyBalance.toInt())
        assertEquals(1, repo.observeLevels().first()[BuildingType.LAB])
    }

    @Test fun `upgrades to max level then stops`() = runBlocking {
        fund(5000, 500, dev = 3000)
        for (lv in 0 until 3) assertTrue(repo.build(BuildingType.COLONY, lv) is BuildResult.Built)
        assertEquals(BuildCheck.MaxLevel, (repo.build(BuildingType.COLONY, 3) as BuildResult.Rejected).reason)
        assertEquals(5000 - 50 - 120 - 250, db.player().get()!!.energyBalance.toInt())
        assertEquals(500 - 8 - 20, db.player().get()!!.credits.toInt())
    }
}
