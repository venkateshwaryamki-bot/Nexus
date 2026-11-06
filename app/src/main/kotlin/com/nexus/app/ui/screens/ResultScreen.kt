package com.nexus.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import com.nexus.domain.civilization.Civilization
import com.nexus.domain.focus.EndReason
import com.nexus.domain.focus.SessionState
import com.nexus.domain.progression.Progression
import com.nexus.domain.reward.RewardKind

/** Session debrief: rewards, level/stage changes, and an honest account of what happened. */
@Composable
fun ResultScreen(vm: NexusViewModel, onContinue: () -> Unit) {
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val o = outcome ?: run { LaunchedEffect(Unit) { onContinue() }; return }
    val r = o.result; val s = r.session; val reward = r.reward
    val completed = s.state == SessionState.COMPLETED
    val p = o.after
    // "Before" is derived from the reward, so it is exact regardless of DB flow timing.
    val levelAfter = p?.level; val levelBefore = p?.let { Progression.levelFor(it.totalXp - reward.xp) }
    val stageAfter = p?.civilization?.stage; val stageBefore = p?.let { Civilization.statusFor(it.civilization.developmentEnergy - reward.energy).stage }
    val counter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { counter.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            CharacterPortrait(if (completed) Character.NEXA else Character.AIRA, size = 170.dp)
            Text(if (completed) "SESSION COMPLETE" else "SESSION ENDED", style = NexusType.Title, color = if (completed) NexusColors.NeonGreen else NexusColors.CosmicGold)
            Text(s.task.ifBlank { "Deep focus" }, style = NexusType.Body, color = NexusColors.TextSecondary)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Energy", "+${(reward.energy * counter.value).toInt()}", Modifier.weight(1f), NexusColors.NeonGreen, Glyph.ENERGY)
                StatTile("XP", "+${(reward.xp * counter.value).toInt()}", Modifier.weight(1f), NexusColors.ElectricCyan, Glyph.STAR)
                StatTile("Focused", "${s.bankedFocusMs / 60_000} min", Modifier.weight(1f), NexusColors.Purple, Glyph.CLOCK)
            }
            Spacer(Modifier.height(14.dp))
            if (levelAfter != null && levelBefore != null && levelAfter.level > levelBefore.level) {
                Banner("LEVEL UP · LV ${levelAfter.level}", if (levelAfter.rank != levelBefore.rank) "New rank: ${levelAfter.rank.title}" else "Keep going, Explorer.", NexusColors.CosmicGold)
            }
            if (stageAfter != null && stageBefore != null && stageAfter != stageBefore) {
                Banner("NEXUS PRIME EVOLVED", "${stageBefore.title} → ${stageAfter.title}. ${stageAfter.description}", NexusColors.ElectricCyan)
            }
            GlassPanel(Modifier.fillMaxWidth(), accent = if (completed) NexusColors.NeonGreen else NexusColors.CosmicGold) {
                SectionLabel("Debrief", color = NexusColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                val lines = buildList {
                    add(when {
                        completed -> "You completed the full ${s.plannedMinutes}-minute session. The energy has been channelled to Nexus Prime."
                        s.endReason == EndReason.PAUSE_LIMIT_EXCEEDED -> "The session ended because the pause allowance ran out."
                        reward.kind == RewardKind.PARTIAL -> "Ended early after ${s.bankedFocusMs / 60_000} minutes. Partial energy (half rate) granted."
                        else -> "Ended before the 10-minute minimum, so no energy this time. Your focus time is still recorded."
                    })
                    if (reward.restLimited) add("Healthy-play limit applied: you've focused a lot today, so energy was reduced. Consider resting.")
                    if (s.interruptions > 0) add("You left NEXUS ${s.interruptions} time${if (s.interruptions > 1) "s" else ""} during this session.")
                    if (s.pauseCount > 0) add("Pauses used: ${s.pauseCount} (${s.totalPausedMs / 60_000} min).")
                    if (s.recoveredAcrossReboot) add("Your device restarted during the session; the time was recovered from the system clock.")
                    if (s.clockAnomaly) add("A clock change was detected and ignored; only real elapsed time counted.")
                }
                lines.forEach { Text("›  $it", style = NexusType.Small, color = NexusColors.TextPrimary, modifier = Modifier.padding(vertical = 2.dp)) }
                if (levelAfter != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("LV ${levelAfter.level} · ${levelAfter.rank.title.uppercase()}  ·  ${levelAfter.xpIntoLevel}/${levelAfter.xpForNextLevel} XP", style = NexusType.Label, color = NexusColors.ElectricCyan)
                    Spacer(Modifier.height(6.dp)); EnergyBar(levelAfter.progress)
                }
            }
            Spacer(Modifier.height(20.dp))
            NexusPrimaryButton("Return to Nexus Prime", onContinue, Modifier.fillMaxWidth(), leading = Glyph.PLANETS)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Banner(title: String, text: String, accent: Color) {
    GlassPanel(Modifier.fillMaxWidth().padding(bottom = 14.dp), accent = accent) {
        Text(title, style = NexusType.Heading, color = accent)
        Text(text, style = NexusType.Small, color = NexusColors.TextPrimary)
    }
}
