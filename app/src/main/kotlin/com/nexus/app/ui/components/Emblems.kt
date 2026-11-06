package com.nexus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import kotlin.math.cos
import kotlin.math.sin

/** Explorer avatars: six emblem insignias (geometry + accent). */
val AvatarAccents = listOf(NexusColors.ElectricCyan, NexusColors.NeonGreen, NexusColors.CosmicGold, NexusColors.Purple, Color(0xFF6FB7FF), Color(0xFFFF8A5B))
val AvatarNames = listOf("Vanguard", "Pathfinder", "Sentinel", "Voidwalker", "Navigator", "Ignition")

@Composable
fun AvatarEmblem(id: Int, modifier: Modifier = Modifier, size: Dp = 44.dp, selected: Boolean = true) {
    val accent = AvatarAccents[id.mod(AvatarAccents.size)]
    Canvas(modifier.size(size)) {
        val w = this.size.width; val c = Offset(w / 2, w / 2)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), NexusColors.MidnightBlue), c, w / 2), w / 2, c)
        drawCircle(accent.copy(alpha = if (selected) 1f else 0.4f), w / 2 - w * 0.03f, c, style = Stroke(w * 0.05f))
        val s = w * 0.26f
        fun poly(n: Int, r: Float, rot: Float) = Path().apply {
            for (k in 0 until n) { val a = rot + k * 2 * Math.PI.toFloat() / n
                val p = Offset(c.x + cos(a) * r, c.y + sin(a) * r); if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
        when (id.mod(6)) {
            0 -> drawPath(poly(3, s, -1.5708f), accent)
            1 -> { drawPath(poly(4, s, 0f), accent, style = Stroke(w * 0.05f)); drawCircle(accent, w * 0.06f, c) }
            2 -> drawPath(poly(6, s, 0f), accent, style = Stroke(w * 0.06f))
            3 -> { drawCircle(accent, s, c, style = Stroke(w * 0.05f)); drawOval(accent, Offset(c.x - s * 1.25f, c.y - s * 0.3f), Size(s * 2.5f, s * 0.6f), style = Stroke(w * 0.035f)) }
            4 -> drawPath(poly(4, s * 1.1f, -1.5708f), accent)
            else -> drawPath(poly(5, s, -1.5708f), accent)
        }
    }
}

/** The five worlds as small orbs (galaxy map art for Milestone 2 builds on these colour identities). */
enum class PlanetIdentity(val title: String, val theme: String, val base: Color, val glow: Color) {
    NEXUS_PRIME("NEXUS PRIME", "Headquarters", Color(0xFF0E3A66), NexusColors.ElectricCyan),
    ORION("ORION", "Knowledge & innovation", Color(0xFF1B4F8C), Color(0xFF8FD8FF)),
    AETHER("AETHER", "Clarity & balance", Color(0xFF0D5C5A), Color(0xFF4CF5D2)),
    TITAN("TITAN", "Discipline & ambition", Color(0xFF6A2A12), Color(0xFFFF8A3D)),
    ECLIPSE("ECLIPSE", "Mastery", Color(0xFF2A1450), NexusColors.Purple),
}

fun DrawScope.drawPlanetOrb(p: PlanetIdentity, c: Offset, r: Float, locked: Boolean) {
    drawCircle(Brush.radialGradient(listOf(p.glow.copy(alpha = if (locked) 0.12f else 0.35f), Color.Transparent), c, r * 1.8f), r * 1.8f, c)
    drawCircle(Brush.radialGradient(listOf(p.glow.copy(alpha = if (locked) 0.35f else 1f), p.base, Color(0xFF02060C)), Offset(c.x - r * 0.4f, c.y - r * 0.4f), r * 1.8f), r, c)
    if (p == PlanetIdentity.TITAN || p == PlanetIdentity.ECLIPSE) drawOval(p.glow.copy(alpha = if (locked) 0.25f else 0.7f), Offset(c.x - r * 1.6f, c.y - r * 0.25f), Size(r * 3.2f, r * 0.5f), style = Stroke(r * 0.08f))
}
