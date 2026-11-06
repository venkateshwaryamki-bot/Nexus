package com.nexus.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import kotlin.math.*

/** Seconds since composition; frozen when reduce-motion is on. Drives all Canvas animation. */
@Composable
fun rememberSceneTime(): State<Float> {
    val reduce = LocalNexusPrefs.current.reduceMotion
    val t = remember { mutableFloatStateOf(0f) }
    if (!reduce) LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.floatValue = (it - start) / 1e9f }
    }
    return t
}

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val depth: Float)
private val stars = run { val rnd = java.util.Random(11); List(170) { Star(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f, rnd.nextFloat()) } }

fun DrawScope.drawStarfield(t: Float, density: Float = 1f, drift: Float = 0.004f) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF030B15), NexusColors.DeepSpaceNavy, Color(0xFF0A1D31))))
    drawCircle(Brush.radialGradient(listOf(NexusColors.Purple.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.2f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.15f, size.height * 0.2f))
    drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = 0.10f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.75f), size.width * 0.9f), size.width * 0.9f, Offset(size.width * 0.9f, size.height * 0.75f))
    val n = (stars.size * density.coerceIn(0f, 1f)).toInt()
    for (i in 0 until n) {
        val s = stars[i]
        val x = ((s.x + t * drift * (0.3f + s.depth)) % 1f) * size.width
        val tw = 0.55f + 0.45f * sin(t * (0.8f + s.depth * 2f) + s.phase)
        drawCircle(Color(0xFFDDEBFF).copy(alpha = (0.25f + 0.6f * s.depth) * tw), s.r * (0.6f + s.depth) * this.density, Offset(x, s.y * size.height))
    }
}

@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val t by rememberSceneTime()
    Canvas(modifier.fillMaxSize()) { drawStarfield(t) }
}
