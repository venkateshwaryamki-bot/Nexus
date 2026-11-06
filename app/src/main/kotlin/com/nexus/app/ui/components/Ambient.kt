package com.nexus.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nexus.app.ui.theme.NexusColors
import kotlin.math.*

/**
 * Focus Chamber ambient environments. Deliberately lightweight 2D Canvas scenes (not the GL world):
 * a focus session can run for 90 minutes and the chamber must stay battery-cheap. Motion is slow
 * and low-contrast so it supports concentration rather than competing for attention.
 */
enum class FocusEnvironment(val id: String, val title: String, val requiredLevel: Int, val accent: Color) {
    COMMAND_CENTRE("command_centre", "Nexus Command Centre", 1, NexusColors.ElectricCyan),
    OBSERVATORY("observatory", "Deep Space Observatory", 3, NexusColors.Purple),
    AETHER_FOREST("aether_forest", "Aether Forest", 6, Color(0xFF4CF5D2)),
    TITAN_STATION("titan_station", "Titan Research Station", 10, Color(0xFFFF8A3D));

    companion object { fun byId(id: String) = entries.firstOrNull { it.id == id } ?: COMMAND_CENTRE }
}

fun DrawScope.drawEnvironment(env: FocusEnvironment, t: Float, energy: Float) {
    when (env) {
        FocusEnvironment.COMMAND_CENTRE -> commandCentre(t, energy)
        FocusEnvironment.OBSERVATORY -> observatory(t)
        FocusEnvironment.AETHER_FOREST -> aether(t)
        FocusEnvironment.TITAN_STATION -> titan(t)
    }
}

private fun DrawScope.commandCentre(t: Float, energy: Float) {
    drawStarfield(t, 0.6f, 0.002f)
    val w = size.width; val h = size.height
    // Panoramic window frame + console horizon
    val horizon = h * 0.78f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0071526)), horizon - h * 0.1f, h), Offset(0f, horizon - h * 0.1f), Size(w, h))
    for (k in -6..6) { val x = w / 2 + k * w * 0.16f
        drawLine(NexusColors.ElectricCyan.copy(alpha = 0.08f), Offset(w / 2 + k * w * 0.02f, horizon), Offset(x * 1.0f + (x - w / 2) * 1.5f, h), 2f) }
    for (k in 0 until 5) { val y = horizon + (h - horizon) * (k / 5f).pow(1.6f)
        drawLine(NexusColors.ElectricCyan.copy(alpha = 0.07f), Offset(0f, y), Offset(w, y), 2f) }
    // Distant Nexus Prime on the horizon charging with session progress
    val pc = Offset(w * 0.5f, horizon - h * 0.02f); val pr = w * 0.34f
    drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = 0.15f + 0.25f * energy), Color.Transparent), pc, pr * 1.5f), pr * 1.5f, pc)
    drawArc(Brush.verticalGradient(listOf(Color(0xFF184C7A), Color(0xFF061321)), pc.y - pr, pc.y), 180f, 180f, true, Offset(pc.x - pr, pc.y - pr), Size(pr * 2, pr * 2))
    drawArc(NexusColors.ElectricCyan.copy(alpha = 0.5f + 0.4f * energy), 180f, 180f, false, Offset(pc.x - pr, pc.y - pr), Size(pr * 2, pr * 2), style = Stroke(3f))
    // Holographic side panels
    for (s in listOf(-1, 1)) {
        val x = if (s < 0) w * 0.04f else w * 0.82f
        for (k in 0 until 6) { val y = h * 0.62f + k * h * 0.022f; val len = w * 0.14f * (0.4f + 0.6f * abs(sin(t * 0.3f + k + s)))
            drawLine(NexusColors.ElectricCyan.copy(alpha = 0.22f), Offset(x, y), Offset(x + len, y), 3f) }
    }
}

private fun DrawScope.observatory(t: Float) {
    drawStarfield(t, 1f, 0.001f)
    val w = size.width; val h = size.height; val c = Offset(w * 0.5f, h * 0.36f)
    // Slowly rotating galaxy spiral
    for (i in 0 until 260) {
        val arm = i % 2; val d = (i / 260f)
        val a = d * 9f + arm * PI.toFloat() + t * 0.03f; val r = w * 0.45f * d
        val col = if (i % 5 == 0) NexusColors.Purple else Color(0xFFBFD8FF)
        drawCircle(col.copy(alpha = 0.5f * (1 - d) + 0.1f), (1.5f + 2.5f * (1 - d)) * density / 2.5f + 1f, Offset(c.x + cos(a) * r, c.y + sin(a) * r * 0.45f))
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1D6).copy(alpha = 0.7f), NexusColors.Purple.copy(alpha = 0.15f), Color.Transparent), c, w * 0.18f), w * 0.18f, c)
    // Observatory dome silhouette
    drawArc(Color(0xFF040B14), 180f, 180f, true, Offset(-w * 0.2f, h * 0.8f), Size(w * 1.4f, h * 0.5f))
    drawArc(NexusColors.Purple.copy(alpha = 0.4f), 180f, 180f, false, Offset(-w * 0.2f, h * 0.8f), Size(w * 1.4f, h * 0.5f), style = Stroke(2f))
}

private fun DrawScope.aether(t: Float) {
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF041A22), Color(0xFF06302F), Color(0xFF041418))))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF4CF5D2).copy(alpha = 0.18f), Color.Transparent), Offset(w * 0.5f, h * 0.3f), w), w, Offset(w * 0.5f, h * 0.3f))
    // Floating islands
    for ((k, p) in listOf(Offset(0.22f, 0.3f), Offset(0.75f, 0.22f), Offset(0.6f, 0.45f)).withIndex()) {
        val y = h * p.y + sin(t * 0.4f + k) * h * 0.008f; val x = w * p.x; val r = w * (0.12f - k * 0.02f)
        drawPath(Path().apply { moveTo(x - r, y); lineTo(x + r, y); lineTo(x + r * 0.2f, y + r * 0.9f); lineTo(x - r * 0.3f, y + r * 0.7f); close() }, Color(0xFF0B2B2E))
        drawOval(Color(0xFF1B5C55), Offset(x - r, y - r * 0.12f), Size(r * 2, r * 0.24f))
        drawCircle(Color(0xFF4CF5D2).copy(alpha = 0.35f), r * 0.25f, Offset(x - r * 0.3f, y - r * 0.25f))
    }
    // Bioluminescent forest silhouette + drifting spores
    for (k in 0 until 14) { val x = w * k / 13f; val th = h * (0.18f + 0.1f * abs(sin(k * 1.7f)))
        drawPath(Path().apply { moveTo(x - w * 0.05f, h); lineTo(x, h - th); lineTo(x + w * 0.05f, h); close() }, Color(0xFF031012))
        drawCircle(Color(0xFF4CF5D2).copy(alpha = 0.5f), 3f, Offset(x, h - th + 8f)) }
    for (i in 0 until 40) { val x = (i * 0.137f + t * 0.004f * (1 + i % 3)) % 1f; val y = 1f - ((i * 0.293f + t * 0.01f * (1 + i % 4)) % 1f)
        drawCircle(Color(0xFF8DFFE5).copy(alpha = 0.5f * sin(y * PI.toFloat())), 2.5f, Offset(x * w, y * h)) }
}

private fun DrawScope.titan(t: Float) {
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A0A08), Color(0xFF3A160C), Color(0xFF120605))))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF8A3D).copy(alpha = 0.3f), Color.Transparent), Offset(w * 0.5f, h * 0.55f), w), w, Offset(w * 0.5f, h * 0.55f))
    // Megastructure towers
    for (k in 0 until 9) { val x = w * (k + 0.5f) / 9f; val th = h * (0.25f + 0.25f * abs(sin(k * 2.3f)))
        drawRect(Color(0xFF0E0605), Offset(x - w * 0.035f, h - th), Size(w * 0.07f, th))
        for (j in 0 until 6) drawRect(Color(0xFFFF8A3D).copy(alpha = 0.25f + 0.2f * sin(t + j + k)), Offset(x - w * 0.02f, h - th + j * th / 7 + 10f), Size(w * 0.04f, 3f)) }
    // Reactor ring pulse
    val c = Offset(w * 0.5f, h * 0.42f); val pr = w * (0.18f + 0.01f * sin(t * 0.8f))
    drawCircle(Color(0xFFFF8A3D).copy(alpha = 0.6f), pr, c, style = Stroke(5f)); drawCircle(Color(0xFFFFD28A).copy(alpha = 0.5f), pr * 0.45f, c)
}
