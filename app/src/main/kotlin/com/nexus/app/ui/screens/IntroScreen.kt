package com.nexus.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.app.NexusViewModel
import com.nexus.app.platform.AudioController
import com.nexus.app.ui.components.drawStarfield
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import kotlin.math.*

/**
 * Screen 01 — Cinematic introduction (~7 s, skippable).
 * 0.0 stars fade in · 1.2 energy core emerges · 2.2 particles circulate · 3.6 core ignites + NEXUS
 * logo · 4.8 tagline · 6.2 camera pushes into the galaxy · 7.0 done.
 */
@Composable
fun IntroScreen(vm: NexusViewModel, onDone: () -> Unit) {
    val reduce = LocalNexusPrefs.current.reduceMotion
    val total = if (reduce) 3.5f else 7.0f
    val time = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    val finish = { if (!finished) { finished = true; onDone() } }
    LaunchedEffect(Unit) {
        vm.audio.play(AudioController.Sfx.INTRO)
        time.animateTo(total, tween((total * 1000).toInt(), easing = LinearEasing))
        finish()
    }
    val t = if (reduce) time.value * 2f else time.value
    fun phase(start: Float, dur: Float) = ((t - start) / dur).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            val push = phase(6.2f, 0.8f); scaleX = 1f + push * push * 1.8f; scaleY = scaleX; alpha = 1f - push * 0.9f
        }) {
            drawStarfield(t, density = phase(0f, 1.6f), drift = 0.006f)
            val c = Offset(size.width / 2, size.height * 0.42f)
            val emerge = phase(1.2f, 1.4f); val ignite = phase(3.6f, 0.5f)
            val r = size.minDimension * (0.05f + 0.06f * emerge + 0.03f * ignite)
            // Core glow + shockwave on ignition
            drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = 0.55f * emerge), NexusColors.Purple.copy(alpha = 0.2f * emerge), Color.Transparent), c, r * 6f), r * 6f, c)
            drawCircle(Brush.radialGradient(listOf(Color.White, NexusColors.ElectricCyan, Color.Transparent), c, r * 1.6f), r * 1.6f * emerge, c)
            if (ignite > 0f && ignite < 1f) drawCircle(NexusColors.ElectricCyan.copy(alpha = 1f - ignite), r * (2f + ignite * 18f), c, style = Stroke(6f * (1f - ignite)))
            // Circulating energy particles on tilted orbits
            val pc = phase(2.2f, 1.0f)
            for (i in 0 until 90) {
                val orbit = i % 3; val tilt = 0.35f + orbit * 0.22f
                val a = i * 0.7f + t * (1.2f + orbit * 0.35f) * (if (orbit == 1) -1 else 1)
                val rr = r * (2.4f + orbit * 1.1f + (i % 7) * 0.12f)
                val p = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr * tilt * (if (orbit == 2) -1 else 1))
                drawCircle((if (i % 4 == 0) NexusColors.NeonGreen else NexusColors.ElectricCyan).copy(alpha = 0.8f * pc), 2.2f + (i % 3), p)
            }
        }
        Column(Modifier.align(Alignment.Center).padding(top = 260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val logo = phase(3.7f, 0.9f)
            Text("NEXUS", style = NexusType.Hero.copy(fontSize = 54.sp, letterSpacing = (24 - 14 * logo).sp),
                color = Color.White, modifier = Modifier.alpha(logo))
            Spacer(Modifier.height(10.dp))
            Text("YOUR FOCUS. YOUR UNIVERSE.", style = NexusType.Label.copy(fontSize = 14.sp, letterSpacing = 4.sp),
                color = NexusColors.ElectricCyan, modifier = Modifier.alpha(phase(4.8f, 0.8f)))
        }
        Text("SKIP", style = NexusType.Label, color = NexusColors.TextSecondary,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(20.dp).clickable { finish() }.padding(10.dp))
    }
}
