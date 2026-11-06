package com.nexus.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nexus.app.R
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pre-rendered 2.5D Nexus Prime sprites, one per civilization stage (tools/art/nexus_island.py). */
val IslandStageArt = intArrayOf(R.drawable.island_stage0, R.drawable.island_stage1, R.drawable.island_stage2, R.drawable.island_stage3, R.drawable.island_stage4)

/**
 * The living headquarters: stage sprite with idle float, a breathing aura, drifting energy motes and
 * an energy-pulse shockwave when [pulseKey] changes. Everything animates in the draw/layer phase only
 * (no recomposition per frame), and freezes under reduce-motion.
 *
 * @param development 0..1 progress within the current stage; subtly brightens the aura.
 */
@Composable
fun NexusIsland(stage: Int, development: Float, stageTitle: String, modifier: Modifier = Modifier, pulseKey: Int = 0) {
    val reduce = LocalNexusPrefs.current.reduceMotion
    val time = rememberSceneTime()
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) { if (pulseKey > 0) { pulse.snapTo(0f); pulse.animateTo(1f, tween(2200, easing = LinearOutSlowInEasing)) } }
    val stageClamped = stage.coerceIn(0, 4)
    val auraStrength = 0.10f + 0.06f * stageClamped + 0.05f * development

    Box(modifier.semantics { contentDescription = "Nexus Prime, $stageTitle" }, contentAlignment = Alignment.Center) {
        // Aura + energy motes behind the island.
        Canvas(Modifier.fillMaxSize()) {
            val t = time.value
            val c = Offset(size.width / 2, size.height * 0.42f)
            val breathe = if (reduce) 1f else 1f + 0.05f * sin(t * 1.1f)
            val p = pulse.value
            val flash = if (p < 1f) (1f - p) * 0.35f else 0f
            drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = auraStrength + flash), NexusColors.Purple.copy(alpha = auraStrength * 0.4f), Color.Transparent),
                c, size.minDimension * 0.55f * breathe), size.minDimension * 0.55f * breathe, c)
            if (stageClamped > 0) {
                val motes = 10 + stageClamped * 6
                for (i in 0 until motes) {
                    val a = i * 2.399f + (if (reduce) 0f else t * (0.08f + (i % 5) * 0.02f))
                    val r = size.minDimension * (0.32f + (i % 7) * 0.025f)
                    val y = c.y + sin(a) * r * 0.32f - (if (reduce) 0f else ((t * 0.04f + i * 0.13f) % 1f) * size.height * 0.12f)
                    val alpha = 0.25f + 0.5f * (0.5f + 0.5f * sin(t * 2f + i))
                    drawCircle((if (i % 4 == 0) NexusColors.NeonGreen else NexusColors.ElectricCyan).copy(alpha = alpha),
                        (1.5f + (i % 3)) * density, Offset(c.x + cos(a) * r, y))
                }
            }
            // Reward shockwave: an ellipse racing out across the island plane.
            if (p < 1f) {
                val w = size.minDimension * (0.15f + p * 0.75f)
                drawOval(NexusColors.NeonGreen.copy(alpha = (1f - p) * 0.9f), Offset(c.x - w, c.y + size.height * 0.02f - w * 0.32f),
                    Size(w * 2, w * 0.64f), style = Stroke((1f - p) * 8f * density))
            }
        }
        Crossfade(stageClamped, animationSpec = tween(900), label = "islandStage") { st ->
            Image(
                painterResource(IslandStageArt[st]), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val t = time.value
                    translationY = if (reduce) 0f else sin(t * 0.9f) * 6f * density
                    rotationZ = if (reduce) 0f else sin(t * 0.45f) * 0.6f
                    val p = pulse.value
                    val pop = if (p < 1f) sin(p * PI.toFloat()) * 0.025f else 0f
                    scaleX = 1f + pop; scaleY = 1f + pop
                },
            )
        }
    }
}
