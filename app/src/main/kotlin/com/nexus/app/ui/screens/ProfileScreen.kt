package com.nexus.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Screen 10 — Player profile, with the core of Screen 12 (statistics) for Milestone 1. */
@Composable
fun ProfileScreen(vm: NexusViewModel, onBack: () -> Unit, onSettings: () -> Unit) {
    val p = vm.player.collectAsStateWithLifecycle().value ?: return
    val stats = vm.stats.collectAsStateWithLifecycle().value
    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScreenHeader("Explorer profile", null, onBack) {
                NexusSecondaryButton("", onSettings, leading = Glyph.SETTINGS)
            }
            Spacer(Modifier.height(16.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarEmblem(p.avatarId, size = 76.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.username, style = NexusType.Title, color = Color.White)
                        Text("${p.level.rank.title.uppercase()} · LEVEL ${p.level.level}", style = NexusType.Label, color = NexusColors.ElectricCyan)
                        Spacer(Modifier.height(6.dp)); EnergyBar(p.level.progress)
                        Text("${p.level.xpIntoLevel} / ${p.level.xpForNextLevel} XP to level ${p.level.level + 1}", style = NexusType.Small, color = NexusColors.TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (stats != null) {
                val hours = stats.totalFocusMs / 3_600_000.0
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Focus hours", "%.1f".format(hours), Modifier.weight(1f), glyph = Glyph.CLOCK)
                    StatTile("Sessions", "${stats.completedSessions}", Modifier.weight(1f), NexusColors.NeonGreen, Glyph.CHECK)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Streak", "${stats.streak.current} d" + if (stats.streak.shieldsAvailable > 0) " · ${stats.streak.shieldsAvailable}🛡" else "", Modifier.weight(1f), NexusColors.CosmicGold, Glyph.FLAME)
                    StatTile("Longest", "${stats.longestSessionMs / 60_000} min", Modifier.weight(1f), NexusColors.Purple, Glyph.STAR)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Completion", "${(stats.completionRate * 100).toInt()}%", Modifier.weight(1f), glyph = Glyph.FOCUS)
                    StatTile("Best streak", "${stats.streak.longest} d", Modifier.weight(1f), NexusColors.CosmicGold, Glyph.FLAME)
                }
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    SectionLabel("Last 7 days · focus minutes")
                    Spacer(Modifier.height(12.dp))
                    WeekChart(stats.recentDays.associate { it.dayKey to (it.focusMs / 60_000).toInt() })
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionLabel("Planet development")
                Spacer(Modifier.height(8.dp))
                PlanetIdentity.entries.forEachIndexed { i, planet ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(36.dp)) { drawPlanetOrb(planet, center, size.width * 0.32f, locked = i != 0) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(planet.title, style = NexusType.BodyStrong, color = if (i == 0) Color.White else NexusColors.TextMuted)
                            Text(if (i == 0) p.civilization.stage.title else "Locked · ${planet.theme}", style = NexusType.Small, color = if (i == 0) planet.glow else NexusColors.TextMuted)
                        }
                        if (i == 0) Text("${(p.civilization.overallDevelopment * 100).toInt()}%", style = NexusType.Numeric, color = planet.glow)
                        else NexusIcon(Glyph.LOCK, NexusColors.TextMuted, size = 16.dp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth(), accent = NexusColors.CosmicGold) {
                SectionLabel("Achievements", color = NexusColors.CosmicGold)
                Spacer(Modifier.height(6.dp))
                Text("Achievement badges (First Contact, Energy Pioneer, Seven-Day Explorer…) come online in the next update. Progress you make now will count toward them.",
                    style = NexusType.Small, color = NexusColors.TextSecondary)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WeekChart(minutesByDay: Map<String, Int>) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { minutesByDay[it.toString()] ?: 0 }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(30)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val bw = size.width / 7f * 0.5f
        values.forEachIndexed { i, v ->
            val x = size.width * (i + 0.5f) / 7f - bw / 2
            val h = (size.height - 24f) * v / max
            drawRoundRect(Color(0x2227DFFF), Offset(x, 0f), Size(bw, size.height - 24f), CornerRadius(8f))
            if (v > 0) drawRoundRect(Brush.verticalGradient(listOf(NexusColors.NeonGreen, NexusColors.ElectricCyan), size.height - 24f - h, size.height - 24f),
                Offset(x, size.height - 24f - h), Size(bw, h), CornerRadius(8f))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        days.forEachIndexed { i, d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(), style = NexusType.Label, color = if (i == 6) NexusColors.ElectricCyan else NexusColors.TextMuted)
                Text("${values[i]}", style = NexusType.Small, color = NexusColors.TextSecondary)
            }
        }
    }
}
