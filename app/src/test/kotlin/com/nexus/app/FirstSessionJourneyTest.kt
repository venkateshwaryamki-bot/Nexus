package com.nexus.app

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nexus.domain.focus.FocusRules.Companion.MINUTE
import com.nexus.domain.time.FakeClock
import com.nexus.world.scene.GraphicsQuality
import com.nexus.world.scene.WorldHost
import com.nexus.world.scene.WorldState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Records what the app tells the 3D world, standing in for the GL host (no GPU in unit tests). */
class FakeWorldView(context: Context) : View(context), WorldHost {
    override fun setWorldState(state: WorldState) { states += state }
    override fun setQuality(quality: GraphicsQuality) {}
    override fun playEnergyPulse(amount: Int) { pulses += amount }
    override fun onHostResume() {}
    override fun onHostPause() {}
    companion object { val states = mutableListOf<WorldState>(); val pulses = mutableListOf<Int>() }
}

class TestNexusApplication : NexusApplication() {
    override fun createContainer() = AppContainer(this, clock, ::FakeWorldView)
    companion object { val clock = FakeClock(wall = java.time.Instant.parse("2026-11-06T09:00:00Z").toEpochMilli()) }
}

/**
 * End-to-end journey through the real app (real Room DB, real coordinator, real Compose UI):
 * intro → guest registration → character intro → home → focus session → completion → rewards
 * reflected in the world. Screenshots of every step are written to app/build/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestNexusApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FirstSessionJourneyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val clock get() = TestNexusApplication.clock

    private fun shot(name: String) {
        rule.waitForIdle()
        val dir = File(System.getProperty("nexus.screenshotDir") ?: "build/screenshots").apply { mkdirs() }
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun advanceUi(ms: Long) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }

    @Test fun firstFocusSessionGrowsTheCivilization() {
        rule.mainClock.autoAdvance = false
        advanceUi(4200)
        shot("01_intro")
        rule.onNodeWithText("SKIP").performClick(); advanceUi(800)

        rule.onNodeWithText("Continue as guest").assertIsDisplayed()
        shot("02_registration")
        rule.onNodeWithText("Continue as guest").performClick(); advanceUi(800)
        rule.onNode(hasSetTextAction()).performTextInput("Nova")
        rule.onNodeWithText("SENTINEL").performClick(); advanceUi(300)
        shot("03_callsign")
        rule.onNodeWithText("Confirm explorer").performClick(); advanceUi(1500)

        advanceUi(3000)
        rule.onNodeWithText("Welcome, Explorer. Your universe is waiting to be rebuilt.").assertExists()
        shot("04_aira")
        repeat(2) { rule.onRoot().performClick(); advanceUi(200); rule.onRoot().performClick(); advanceUi(600) }
        shot("05_vyon_planets")
        rule.onNodeWithText("SKIP").performClick(); advanceUi(1500)

        // Home: abandoned outpost, zero energy.
        rule.onNodeWithText("ABANDONED OUTPOST").assertExists()
        rule.onNodeWithText("Nova").assertExists()
        assertEquals(0, FakeWorldView.states.last().stage)
        shot("06_home_outpost")

        rule.onNodeWithText("Enter Focus Chamber").performClick(); advanceUi(800)
        rule.onNode(hasSetTextAction()).performTextInput("Read chapter 4")
        rule.onNodeWithText("60 MIN").performClick(); advanceUi(300)
        shot("07_focus_setup")
        rule.onNodeWithText("Begin focus · 60 min").performScrollTo().performClick()
        waitUntilText("REMAINING")
        clock.advance(17 * MINUTE + 25_000); advanceUi(600)
        rule.onNodeWithText("42:35").assertExists()
        shot("08_chamber_running")

        rule.onNodeWithText("Pause (3 left)").performClick()
        waitUntilText("PAUSED")
        clock.advance(2 * MINUTE); advanceUi(600)
        rule.onNodeWithText("42:35").assertExists() // paused time does not count
        shot("09_chamber_paused")
        rule.onNodeWithText("Resume").performClick()
        waitUntilText("REMAINING")

        // The phone sits on the desk for the rest of the hour.
        clock.advance(43 * MINUTE); advanceUi(1000)
        waitUntilText("SESSION COMPLETE")
        advanceUi(1600) // reward counters animate up
        rule.onNodeWithText("+60").assertExists()
        rule.onNodeWithText("+145").assertExists()
        shot("10_session_complete")

        val c = (rule.activity.application as NexusApplication).container
        val player = runBlocking { c.db.player().get()!! }
        assertEquals(60, player.energyBalance); assertEquals(145, player.totalXp)
        assertEquals("exactly one reward row", 1, runBlocking { c.db.ledger().count() })

        rule.onNodeWithText("Return to Nexus Prime").performScrollTo().performClick(); advanceUi(2000)
        rule.onNodeWithText("SMALL SETTLEMENT").assertExists()
        assertEquals(1, FakeWorldView.states.last().stage)
        assertTrue(FakeWorldView.states.last().development > 0f)
        advanceUi(800)
        assertEquals(listOf(60), FakeWorldView.pulses)
        shot("11_home_settlement")

        rule.onNodeWithText("PROFILE").performClick(); advanceUi(1200)
        shot("12_profile")
    }

    private fun waitUntilText(text: String) {
        repeat(40) {
            if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            advanceUi(250)
        }
        fail("Timed out waiting for '$text'")
    }
}
