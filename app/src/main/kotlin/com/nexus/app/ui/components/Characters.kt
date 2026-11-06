package com.nexus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.NexusColors
import kotlin.math.*

/**
 * Character portraits. PLACEHOLDER ART for Milestone 1: stylised vector busts that lock in each
 * character's silhouette and colour language (ART_DIRECTION.md §Characters). Rigged 3D models
 * replace these in Milestone 2; the visual identity rules stay the same.
 */
enum class Character(val displayName: String, val role: String, val accent: Color) {
    AIRA("AIRA", "AI Guide", NexusColors.ElectricCyan),
    VYON("VYON", "The Explorer", NexusColors.ElectricCyan),
    ORION("ORION", "Operations Robot", Color(0xFF6FB7FF)),
    KAEL("KAEL", "The Strategist", Color(0xFF4F7DFF)),
    NEXA("NEXA", "Nexus Intelligence", NexusColors.Purple),
}

@Composable
fun CharacterPortrait(character: Character, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val t by rememberSceneTime()
    Canvas(modifier.size(size)) {
        when (character) {
            Character.AIRA -> drawAira(t)
            Character.VYON -> drawVyon(t)
            Character.ORION -> drawOrion(t)
            Character.KAEL -> drawVyon(t, kael = true)
            Character.NEXA -> drawNexa(t)
        }
    }
}

private fun DrawScope.halo(c: Offset, r: Float, color: Color, a: Float = 0.35f) =
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), c, r), r, c)

private fun DrawScope.drawAira(t: Float) {
    val w = size.width; val cx = w / 2; val bob = sin(t * 1.3f) * w * 0.012f
    val holo = NexusColors.ElectricCyan
    halo(Offset(cx, w * 0.45f), w * 0.5f, holo, 0.28f)
    // Halo ring
    drawOval(holo.copy(alpha = 0.7f), Offset(cx - w * 0.3f, w * 0.06f + bob), Size(w * 0.6f, w * 0.12f), style = Stroke(w * 0.012f))
    // Shoulders / gown (translucent)
    val body = Path().apply {
        moveTo(cx - w * 0.36f, w); cubicTo(cx - w * 0.34f, w * 0.78f, cx - w * 0.2f, w * 0.7f, cx, w * 0.69f + bob)
        cubicTo(cx + w * 0.2f, w * 0.7f, cx + w * 0.34f, w * 0.78f, cx + w * 0.36f, w); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(Color(0xCCEAF6FF), holo.copy(alpha = 0.15f)), w * 0.68f, w))
    // Neck + head
    drawRect(Color(0xCCD8ECF8), Offset(cx - w * 0.04f, w * 0.56f + bob), Size(w * 0.08f, w * 0.14f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFFF4FAFF), Color(0xFFBFD8EA)), w * 0.22f, w * 0.62f), Offset(cx - w * 0.13f, w * 0.24f + bob), Size(w * 0.26f, w * 0.34f))
    // Flowing hair arcs (silver)
    val hair = Path().apply {
        moveTo(cx - w * 0.15f, w * 0.42f + bob); cubicTo(cx - w * 0.2f, w * 0.18f, cx + w * 0.2f, w * 0.14f, cx + w * 0.16f, w * 0.38f + bob)
        cubicTo(cx + w * 0.24f, w * 0.55f, cx + w * 0.2f, w * 0.7f, cx + w * 0.12f, w * 0.78f + bob)
        cubicTo(cx + w * 0.14f, w * 0.6f, cx + w * 0.1f, w * 0.36f, cx, w * 0.3f + bob)
        cubicTo(cx - w * 0.08f, w * 0.33f, cx - w * 0.12f, w * 0.38f, cx - w * 0.15f, w * 0.42f + bob); close()
    }
    drawPath(hair, Brush.verticalGradient(listOf(Color(0xFFE8F1FA), Color(0xFF9FB8CC))))
    // Eyes (cyan, calm)
    for (s in listOf(-1, 1)) drawLine(holo, Offset(cx + s * w * 0.035f - w * 0.025f, w * 0.41f + bob), Offset(cx + s * w * 0.035f + w * 0.02f, w * 0.41f + bob), w * 0.012f)
    // Holographic scanlines + core gem
    for (k in 0 until 18) { val y = w * 0.2f + k * w * 0.045f + (t * w * 0.03f) % (w * 0.045f); drawLine(holo.copy(alpha = 0.08f), Offset(cx - w * 0.38f, y), Offset(cx + w * 0.38f, y), w * 0.004f) }
    drawCircle(holo, w * 0.022f, Offset(cx, w * 0.8f + bob)); halo(Offset(cx, w * 0.8f + bob), w * 0.09f, holo, 0.6f)
}

private fun DrawScope.drawVyon(t: Float, kael: Boolean = false) {
    val w = size.width; val cx = w / 2; val breathe = sin(t * 1.1f) * w * 0.006f
    val suit = if (kael) Color(0xFF2A3140) else Color(0xFF0E1B2E)
    val trim = if (kael) Color(0xFFB9C3D1) else Color(0xFF3B5470)
    val glow = if (kael) Color(0xFF4F7DFF) else NexusColors.ElectricCyan
    halo(Offset(cx, w * 0.55f), w * 0.5f, glow, 0.18f)
    // Armoured shoulders
    val body = Path().apply {
        moveTo(cx - w * 0.44f, w); lineTo(cx - w * 0.4f, w * 0.76f); lineTo(cx - w * 0.22f, w * 0.66f + breathe)
        lineTo(cx + w * 0.22f, w * 0.66f + breathe); lineTo(cx + w * 0.4f, w * 0.76f); lineTo(cx + w * 0.44f, w); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(suit, Color(0xFF050B14)), w * 0.66f, w))
    drawPath(Path().apply { moveTo(cx - w * 0.4f, w * 0.76f); lineTo(cx - w * 0.22f, w * 0.66f + breathe); lineTo(cx - w * 0.16f, w * 0.8f) }, trim, style = Stroke(w * 0.012f))
    drawPath(Path().apply { moveTo(cx + w * 0.4f, w * 0.76f); lineTo(cx + w * 0.22f, w * 0.66f + breathe); lineTo(cx + w * 0.16f, w * 0.8f) }, trim, style = Stroke(w * 0.012f))
    // Chest reactor
    val core = Offset(cx, w * 0.84f + breathe)
    halo(core, w * 0.12f, glow, 0.7f); drawCircle(Color.White, w * 0.022f, core)
    drawCircle(glow, w * 0.045f, core, style = Stroke(w * 0.01f))
    // Helmet
    val helm = Path().apply {
        moveTo(cx - w * 0.16f, w * 0.62f); cubicTo(cx - w * 0.2f, w * 0.38f, cx - w * 0.14f, w * 0.2f, cx, w * 0.19f)
        cubicTo(cx + w * 0.14f, w * 0.2f, cx + w * 0.2f, w * 0.38f, cx + w * 0.16f, w * 0.62f); close()
    }
    drawPath(helm, Brush.verticalGradient(listOf(trim, suit), w * 0.2f, w * 0.62f))
    // Visor (cyan) with sweeping reflection
    val visor = Path().apply {
        moveTo(cx - w * 0.13f, w * 0.38f); quadraticTo(cx, w * 0.33f, cx + w * 0.13f, w * 0.38f)
        lineTo(cx + w * 0.11f, w * 0.47f); quadraticTo(cx, w * 0.5f, cx - w * 0.11f, w * 0.47f); close()
    }
    drawPath(visor, Brush.horizontalGradient(listOf(glow.copy(alpha = 0.55f), glow, glow.copy(alpha = 0.55f))))
    val sx = cx - w * 0.13f + ((t * 0.25f) % 1f) * w * 0.26f
    drawLine(Color.White.copy(alpha = 0.6f), Offset(sx, w * 0.36f), Offset(sx - w * 0.03f, w * 0.48f), w * 0.01f)
    if (kael) drawLine(Color(0xFFB9C3D1), Offset(cx, w * 0.2f), Offset(cx, w * 0.33f), w * 0.016f)
}

private fun DrawScope.drawOrion(t: Float) {
    val w = size.width; val cx = w / 2; val bob = sin(t * 2f) * w * 0.03f; val cy = w * 0.45f + bob
    val blue = Color(0xFF6FB7FF)
    // Hover glow under the robot
    drawOval(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = 0.4f), Color.Transparent), Offset(cx, w * 0.86f), w * 0.25f), Offset(cx - w * 0.25f, w * 0.8f), Size(w * 0.5f, w * 0.12f))
    // Body shell (rounded, white + metallic blue)
    drawOval(Brush.verticalGradient(listOf(Color(0xFFF2F7FC), Color(0xFFB4C6D8)), cy - w * 0.25f, cy + w * 0.25f), Offset(cx - w * 0.28f, cy - w * 0.24f), Size(w * 0.56f, w * 0.48f))
    drawOval(blue, Offset(cx - w * 0.28f, cy + w * 0.02f), Size(w * 0.56f, w * 0.22f), style = Stroke(w * 0.02f))
    // Face screen
    drawRoundRect(Color(0xFF0A1726), Offset(cx - w * 0.19f, cy - w * 0.14f), Size(w * 0.38f, w * 0.2f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
    val blink = if ((t % 4f) > 3.85f) 0.2f else 1f
    for (s in listOf(-1, 1)) {
        val e = Offset(cx + s * w * 0.08f, cy - w * 0.04f)
        halo(e, w * 0.07f, NexusColors.ElectricCyan, 0.7f)
        drawOval(NexusColors.ElectricCyan, Offset(e.x - w * 0.03f, e.y - w * 0.035f * blink), Size(w * 0.06f, w * 0.07f * blink))
    }
    // Energy core + side fins
    drawCircle(NexusColors.ElectricCyan, w * 0.03f, Offset(cx, cy + w * 0.14f)); halo(Offset(cx, cy + w * 0.14f), w * 0.08f, NexusColors.ElectricCyan, 0.6f)
    for (s in listOf(-1, 1)) drawRoundRect(blue, Offset(cx + s * w * 0.3f - w * 0.04f, cy - w * 0.06f), Size(w * 0.08f, w * 0.16f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
    drawLine(Color(0xFFB4C6D8), Offset(cx, cy - w * 0.24f), Offset(cx, cy - w * 0.32f), w * 0.015f)
    drawCircle(NexusColors.NeonGreen, w * 0.02f * (0.8f + 0.2f * sin(t * 4f)), Offset(cx, cy - w * 0.33f))
}

private fun DrawScope.drawNexa(t: Float) {
    val w = size.width; val c = Offset(w / 2, w / 2)
    halo(c, w * 0.5f, NexusColors.Purple, 0.4f); halo(c, w * 0.3f, NexusColors.ElectricCyan, 0.5f)
    for (k in 0 until 3) {
        val a = t * (0.4f + k * 0.2f)
        drawOval(listOf(NexusColors.ElectricCyan, NexusColors.Purple, Color.White)[k].copy(alpha = 0.55f),
            Offset(c.x - w * (0.36f - k * 0.05f), c.y - w * 0.08f * (1 + cos(a))), Size(w * (0.72f - k * 0.1f), w * 0.16f * (1 + cos(a))), style = Stroke(w * 0.008f))
    }
    for (i in 0 until 40) {
        val a = i * 2.4f + t * (0.3f + (i % 5) * 0.08f); val r = w * (0.18f + (i % 7) * 0.035f)
        drawCircle(if (i % 3 == 0) NexusColors.Purple else NexusColors.ElectricCyan, w * 0.008f, Offset(c.x + cos(a) * r, c.y + sin(a) * r * 0.8f))
    }
    drawCircle(Color.White, w * 0.06f * (1f + 0.08f * sin(t * 2f)), c)
}
