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
        rule.onNodeWithContentDescription("Nexus Prime, Abandoned Outpost").assertExists()
        rule.onNodeWithText("Abandoned Outpost").assertExists()
        rule.onNodeWithText("Nova").assertExists()
        rule.onNodeWithText("0 / 60 energy to Small Settlement").assertExists()
        shot("06_home_outpost")

        rule.onNodeWithText("Start Focus").performClick(); advanceUi(800)
        rule.onNode(hasSetTextAction()).performTextInput("Read chapter 4")
        rule.onNodeWithText("60 min").performClick(); advanceUi(300)
        shot("07_focus_setup")
        rule.onNodeWithText("Begin focus · 60 min").performClick()
        waitUntilText("Focus Session")
        clock.advance(17 * MINUTE + 25_000); advanceUi(600)
        rule.onNodeWithText("42:35").assertExists()
        shot("08_chamber_running")

        val coreDesc = rule.onNode(hasContentDescription("Energy core", substring = true)).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first()
        assertTrue("core charge reflects focus progress: $coreDesc", coreDesc.contains("2") && coreDesc.endsWith("% charged"))
        println("core: $coreDesc")
        rule.onNodeWithContentDescription("Pause").performClick()
        waitUntilText("Paused")
        clock.advance(2 * MINUTE); advanceUi(600)
        rule.onNodeWithText("42:35").assertExists() // paused time does not count
        shot("09_chamber_paused")
        rule.onNodeWithText("Pause allowance left 13:00 · pauses used 1/3").assertExists()
        rule.onNodeWithContentDescription("Resume").performClick()
        waitUntilText("Focus Session")

        // The phone sits on the desk for the rest of the hour.
        clock.advance(43 * MINUTE); advanceUi(600)
        waitUntilText("Session complete") // chamber completion burst
        shot("09b_chamber_completing")
        advanceUi(2000)
        waitUntilText("Session Complete!")
        advanceUi(3200) // orchestrated reward entrance
        rule.onNodeWithText("+60").assertExists()
        rule.onNodeWithText("+145").assertExists()
        shot("10_session_complete")

        val c = (rule.activity.application as NexusApplication).container
        val player = runBlocking { c.db.player().get()!! }
        assertEquals(60, player.energyBalance); assertEquals(145, player.totalXp); assertEquals(12, player.credits)
        rule.onNodeWithText("+12").assertExists()
        rule.onNodeWithContentDescription("Nexus Prime evolved from Abandoned Outpost to Small Settlement").assertExists()
        assertEquals("exactly one reward row", 1, runBlocking { c.db.ledger().count() })

        rule.onNodeWithText("Continue").performClick(); advanceUi(2000)
        rule.onNodeWithContentDescription("Nexus Prime, Small Settlement").assertExists()
        rule.onNodeWithText("60 / 300 energy to Research Colony").assertExists()
        rule.onNodeWithText("+60 energy channelled").assertExists() // reward feedback on the island
        rule.onNodeWithContentDescription("60 energy").assertExists()
        rule.onNodeWithContentDescription("12 credits").assertExists()
        shot("11_home_settlement")

        // Phase 5: spend the energy on the first structure.
        rule.onNodeWithText("Build").performClick(); advanceUi(1200)
        rule.onNodeWithContentDescription("60 energy available").assertExists()
        shot("11b_build_before")
        rule.onAllNodesWithText("Build").filterToOne(hasClickAction() and hasAnyAncestor(hasText("Energy Reactor"))).performClick()
        waitUntil { rule.onAllNodesWithContentDescription("30 energy available").fetchSemanticsNodes().isNotEmpty() }
        waitUntil { rule.onAllNodesWithContentDescription("Level 1 of 3").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Nexus Prime, Small Settlement, 1 structures").assertExists()
        advanceUi(800)
        shot("11c_build_after")
        assertEquals(165, runBlocking { c.db.player().get()!!.totalXp }) // 145 + 20 for the reactor
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; advanceUi(1200)

        rule.onNode(hasText("Profile") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); advanceUi(1200)
        shot("12_profile")
    }

    private fun waitUntil(cond: () -> Boolean) {
        repeat(40) { if (cond()) return; Thread.sleep(50); advanceUi(250) }
        fail("Timed out waiting for condition")
    }

    private fun waitUntilText(text: String) {
        repeat(40) {
            if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return
            advanceUi(250)
        }
        fail("Timed out waiting for '$text'")
    }
}
