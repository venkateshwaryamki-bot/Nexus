package com.nexus.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.R
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.*
import com.nexus.domain.civilization.Civilization
import com.nexus.domain.focus.EndReason
import com.nexus.domain.focus.SessionState
import com.nexus.domain.progression.Progression
import com.nexus.domain.reward.RewardKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Session debrief (blueprint "Session Complete!"): rewards, planet progress, level/stage changes, honest notes. */
@Composable
fun ResultScreen(vm: NexusViewModel, onContinue: () -> Unit) {
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val o = outcome ?: run { LaunchedEffect(Unit) { onContinue() }; return }
    val r = o.result; val s = r.session; val reward = r.reward
    val completed = s.state == SessionState.COMPLETED
    val reduce = LocalNexusPrefs.current.reduceMotion
    val p = o.after
    // "Before" is derived from the reward, so it is exact regardless of DB flow timing.
    val levelAfter = p?.level; val levelBefore = p?.let { Progression.levelFor(it.totalXp - reward.xp) }
    val civAfter = p?.civilization; val civBefore = p?.let { Civilization.statusFor(it.civilization.developmentEnergy - reward.energy) }
    val evolved = civAfter != null && civBefore != null && civAfter.stage != civBefore.stage

    // Orchestrated entrance: crystal (0–0.9 s), counters staggered, then progress bar.
    val intro = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(Motion.reward * 2, easing = LinearEasing)) }
    fun phase(start: Float, len: Float) = ((intro.value - start) / len).coerceIn(0f, 1f)
    val time = rememberSceneTime()
    val accent = if (completed) NexusColors.ElectricCyan else NexusColors.CosmicGold

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Spacer(Modifier.height(Space.lg))
                Text(if (completed) "Session Complete!" else "Session Ended", style = NexusType.Title, color = NexusColors.TextPrimary)
                Text(s.task.ifBlank { "Deep focus" } + " · ${s.bankedFocusMs / 60_000} min focused", style = NexusType.Small, color = NexusColors.TextSecondary)

                // Hero crystal with rays and burst.
                Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val t = time.value; val c = center; val pop = phase(0f, 0.35f)
                        val rays = 14
                        for (i in 0 until rays) {
                            val a = i * (2 * PI.toFloat() / rays) + t * 0.15f
                            val len = size.minDimension * (0.42f + 0.06f * sin(t * 1.5f + i))
                            val wdt = 0.07f
                            val path = Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + cos(a - wdt) * len, c.y + sin(a - wdt) * len)
                                lineTo(c.x + cos(a + wdt) * len, c.y + sin(a + wdt) * len); close()
                            }
                            drawPath(path, Brush.radialGradient(listOf(accent.copy(alpha = 0.32f * pop), Color.Transparent), c, len))
                        }
                        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f * pop), Color.Transparent), c, size.minDimension * 0.4f), size.minDimension * 0.4f, c)
                        // Burst sparks flying out once.
                        val b = phase(0.05f, 0.5f)
                        if (b in 0.001f..0.999f) for (i in 0 until 24) {
                            val a = i * 0.2618f; val rr = size.minDimension * (0.12f + 0.4f * b)
                            drawCircle((if (i % 3 == 0) NexusColors.NeonGreen else accent).copy(alpha = 1f - b), 4f * (1f - b) + 1.5f, Offset(c.x + cos(a) * rr, c.y + sin(a) * rr))
                        }
                        // Platform rings under the crystal.
                        for (k in 0 until 3) {
                            val rw = size.minDimension * (0.2f + k * 0.09f)
                            drawOval(accent.copy(alpha = (0.55f - k * 0.15f) * pop), Offset(c.x - rw, c.y + size.minDimension * 0.3f - rw * 0.18f),
                                androidx.compose.ui.geometry.Size(rw * 2, rw * 0.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
                        }
                    }
                    Image(painterResource(R.drawable.energy_core), null, Modifier.size(150.dp).graphicsLayer {
                        val pop = phase(0f, 0.35f)
                        val over = if (pop < 1f) sin(pop * PI.toFloat() / 2) * (1f + 0.15f * sin(pop * PI.toFloat())) else 1f
                        scaleX = over; scaleY = over
                        translationY = if (reduce) 0f else sin(time.value * 1.3f) * 6f * density
                        alpha = if (completed) 1f else 0.6f
                    })
                }

                // Reward rows (staggered count-up).
                NexusCard(Modifier.fillMaxWidth(), tone = if (completed) CardTone.Success else CardTone.Premium) {
                    RewardRow(Glyph.ENERGY, NexusColors.NeonGreen, reward.energy, "Energy", phase(0.25f, 0.3f))
                    RewardRow(Glyph.STAR, NexusColors.CosmicGold, reward.xp, "XP", phase(0.35f, 0.3f))
                    RewardRow(Glyph.CREDIT, NexusColors.Purple, reward.credits, "Credits", phase(0.45f, 0.3f))
                }

                if (levelAfter != null && levelBefore != null && levelAfter.level > levelBefore.level) {
                    Banner("Level up · Lv. ${levelAfter.level}", if (levelAfter.rank != levelBefore.rank) "New rank: ${levelAfter.rank.title}" else "Keep going, Explorer.",
                        NexusColors.CosmicGold, Modifier.alpha(phase(0.6f, 0.2f)))
                }

                // Planet progress: before → after.
                if (civAfter != null && civBefore != null) {
                    val beforeP = if (evolved) 0f else civBefore.progressToNext
                    val bar = beforeP + (civAfter.progressToNext - beforeP) * FastOutSlowInEasing.transform(phase(0.6f, 0.35f))
                    NexusCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                        contentDescription = if (evolved) "Nexus Prime evolved from ${civBefore.stage.title} to ${civAfter.stage.title}"
                            else "Nexus Prime progress ${(civBefore.progressToNext * 100).toInt()} to ${(civAfter.progressToNext * 100).toInt()} percent" }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val levels by vm.buildingLevels.collectAsStateWithLifecycle()
                            NexusIsland(civAfter.stage.ordinal, levels, civAfter.stage.title, Modifier.size(64.dp), animate = false)
                            Spacer(Modifier.width(Space.sm))
                            Column(Modifier.weight(1f)) {
                                Text("NEXUS PRIME", style = NexusType.Heading, color = NexusColors.TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (evolved) "${civBefore.stage.title} → " else "Progress ${(civBefore.progressToNext * 100).toInt()}% → ",
                                        style = NexusType.Small, color = NexusColors.TextSecondary)
                                    Text(if (evolved) civAfter.stage.title else "${(civAfter.progressToNext * 100).toInt()}%", style = NexusType.NumericSmall, color = NexusColors.NeonGreen)
                                }
                                Spacer(Modifier.height(6.dp))
                                EnergyBar(bar, Modifier.fillMaxWidth(), colors = listOf(NexusColors.ElectricCyan, NexusColors.NeonGreen))
                            }
                        }
                        if (evolved) { Spacer(Modifier.height(Space.xs)); Text("Nexus Prime evolved! ${civAfter.stage.description}", style = NexusType.Small, color = NexusColors.ElectricCyan) }
                    }
                }

                // Honest notes about what happened.
                val notes = buildList {
                    add(when {
                        completed -> "You completed the full ${s.plannedMinutes}-minute session. The energy has been channelled to Nexus Prime."
                        s.endReason == EndReason.PAUSE_LIMIT_EXCEEDED -> "The session ended because the pause allowance ran out."
                        reward.kind == RewardKind.PARTIAL -> "Ended early after ${s.bankedFocusMs / 60_000} minutes. Partial energy (half rate). Credits come only from completed sessions."
                        else -> "Ended before the 10-minute minimum, so no energy this time. Your focus time is still recorded."
                    })
                    if (reward.restLimited) add("Healthy-play limit applied: you've focused a lot today, so energy was reduced. Consider resting.")
                    if (s.interruptions > 0) add("You left NEXUS ${s.interruptions} time${if (s.interruptions > 1) "s" else ""} during this session.")
                    if (s.pauseCount > 0) add("Pauses used: ${s.pauseCount} (${s.totalPausedMs / 60_000} min).")
                    if (s.recoveredAcrossReboot) add("Your device restarted during the session; the time was recovered from the system clock.")
                    if (s.clockAnomaly) add("A clock change was detected and ignored; only real elapsed time counted.")
                }
                Column(Modifier.fillMaxWidth().alpha(phase(0.75f, 0.25f))) {
                    notes.forEach { Text("›  $it", style = NexusType.Small, color = NexusColors.TextSecondary, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                Spacer(Modifier.height(Space.xs))
            }
            NexusButton("Continue", onContinue, Modifier.fillMaxWidth().padding(Space.gutter), trailing = Glyph.ARROW_RIGHT)
        }
    }
}

@Composable
private fun RewardRow(glyph: Glyph, tint: Color, amount: Int, label: String, progress: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(0.3f + 0.7f * progress), verticalAlignment = Alignment.CenterVertically) {
        NexusIcon(glyph, tint, size = 24.dp)
        Spacer(Modifier.width(Space.sm))
        Text("+${(amount * progress).toInt()}", style = NexusType.Numeric, color = NexusColors.TextPrimary)
        Spacer(Modifier.width(6.dp))
        Text(label, style = NexusType.Body, color = NexusColors.TextSecondary)
    }
}

@Composable
private fun Banner(title: String, text: String, accent: Color, modifier: Modifier = Modifier) {
    NexusCard(modifier.fillMaxWidth(), tone = CardTone.Premium) {
        Text(title, style = NexusType.Heading, color = accent)
        Text(text, style = NexusType.Small, color = NexusColors.TextPrimary)
    }
}
