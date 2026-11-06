package com.nexus.app

import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nexus.app.ui.screens.DesignSystemScreen
import com.nexus.app.ui.theme.NexusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the design-system reference (all tokens + components) and saves a full-length capture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestNexusApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun rendersEveryComponent() {
        // No infinite animations on this screen, so the test clock may advance normally.
        rule.setContent { NexusTheme { DesignSystemScreen(onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("Start Focus").assertExists()
        rule.onNodeWithText("Deep Worker").assertExists()
        rule.onNodeWithContentDescription("Start focus").assertExists()
        val tab = { label: String -> hasText(label) and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab) }
        rule.onNode(tab("Universe")).assertIsSelected()
        // Tap a nav tab: selection must move.
        rule.onNode(tab("Arena")).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.mainClock.advanceTimeBy(300); rule.waitForIdle()
        rule.onNode(tab("Arena")).assertIsSelected()
        rule.onNode(tab("Universe")).assertIsNotSelected()

        val v = rule.activity.window.decorView
        val w = v.width; val h = v.height * 4
        rule.runOnUiThread {
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
        }
        rule.mainClock.advanceTimeBy(100); rule.waitForIdle()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(bmp)) }
        val dir = File(System.getProperty("nexus.screenshotDir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "ds_reference.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
