package com.nexus.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.Route
import com.nexus.app.nexus
import com.nexus.app.platform.AudioController
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import com.nexus.domain.focus.SessionEvaluation
import com.nexus.world.scene.WorldHost
import com.nexus.world.scene.WorldState
import kotlinx.coroutines.delay

/** Screen 04 — Main home: the living 3D headquarters of Nexus Prime with a game HUD. */
@Composable
fun HomeScreen(vm: NexusViewModel, go: (Route) -> Unit) {
    val player by vm.player.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val open by vm.openSession.collectAsStateWithLifecycle()
    val p = player ?: return
    val civ = p.civilization
    var worldView by remember { mutableStateOf<WorldHost?>(null) }
    val worldFactory = androidx.compose.ui.platform.LocalContext.current.nexus.worldHostFactory
    var gainText by remember { mutableStateOf<Int?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // --- 3D world (fills the screen) ---------------------------------------------------------
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx -> worldFactory(ctx).also { worldView = it as WorldHost } }, modifier = Modifier.fillMaxSize(),
            update = { view -> val v = view as WorldHost
                v.setQuality(settings.quality)
                v.setWorldState(WorldState(development = civ.overallDevelopment, stage = civ.stage.ordinal))
            })
        DisposableEffect(lifecycle, worldView) {
            val v = worldView
            val obs = LifecycleEventObserver { _, e ->
                when (e) { Lifecycle.Event.ON_RESUME -> v?.onHostResume(); Lifecycle.Event.ON_PAUSE -> v?.onHostPause(); else -> {} }
            }
            if (v != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) v.onHostResume()
            lifecycle.addObserver(obs)
            onDispose { lifecycle.removeObserver(obs); v?.onHostPause() }
        }
        // Reward feedback: energy pulse across the planet + floating "+N" readout.
        LaunchedEffect(worldView) {
            val v = worldView ?: return@LaunchedEffect
            delay(700)
            val amount = vm.consumePulse()
            if (amount > 0) { v.playEnergyPulse(amount); vm.audio.play(AudioController.Sfx.ENERGY); gainText = amount; delay(2600); gainText = null }
        }
        // Vignettes keep HUD legible over the world.
        Box(Modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color(0xE6061321), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2061321)))))

        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            TopHud(vm, onProfile = { go(Route.PROFILE) })
            Spacer(Modifier.height(12.dp))
            PlanetStatusPanel(civ.stage.title, civ.nextStage?.title, civ.progressToNext, civ.developmentEnergy, civ.nextStage?.requiredEnergy)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(gainText != null, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { -it }) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 28.dp)
                        Text("+${gainText ?: 0} ENERGY CHANNELLED", style = NexusType.Title, color = NexusColors.NeonGreen)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val today = stats?.recentDays?.firstOrNull { it.dayKey == java.time.LocalDate.now().toString() }
            OrionTip(open != null, today?.completedSessions ?: 0, settings.dailyGoalSessions, stats?.streak?.current ?: 0)
            Spacer(Modifier.height(12.dp))
            if (open != null) ActiveSessionBanner(vm, onOpen = { go(Route.FOCUS) })
            else NexusPrimaryButton("Enter Focus Chamber", { go(Route.FOCUS) }, Modifier.fillMaxWidth(), leading = Glyph.FOCUS)
            Spacer(Modifier.height(12.dp))
            BottomDock(go)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopHud(vm: NexusViewModel, onProfile: () -> Unit) {
    val p = vm.player.collectAsStateWithLifecycle().value ?: return
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(30.dp)).clickable(onClick = onProfile).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarEmblem(p.avatarId, size = 46.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(p.username, style = NexusType.Heading, color = Color.White, maxLines = 1)
                Text("LV ${p.level.level} · ${p.level.rank.title.uppercase()}", style = NexusType.Label, color = NexusColors.ElectricCyan)
                Spacer(Modifier.height(3.dp))
                EnergyBar(p.level.progress, Modifier.width(110.dp), height = 4.dp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x9910283F)).border(1.dp, NexusColors.PanelBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("%,d".format(p.energyBalance), style = NexusType.Numeric, color = Color.White)
        }
    }
}

@Composable
private fun PlanetStatusPanel(stage: String, next: String?, progress: Float, energy: Int, nextReq: Int?) {
    GlassPanel(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Nexus Prime · Headquarters", color = NexusColors.TextSecondary)
                Text(stage.uppercase(), style = NexusType.Title.copy(fontSize = 22.sp), color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (nextReq != null) "$energy / $nextReq" else "$energy", style = NexusType.Numeric.copy(fontSize = 15.sp), color = NexusColors.ElectricCyan)
                Text("ENERGY CHANNELLED", style = NexusType.Label.copy(fontSize = 10.sp), color = NexusColors.TextMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        EnergyBar(progress)
        Spacer(Modifier.height(6.dp))
        Text(if (next != null) "Next: $next" else "Fully restored. New worlds await.", style = NexusType.Small, color = NexusColors.TextSecondary)
    }
}

@Composable
private fun OrionTip(sessionActive: Boolean, doneToday: Int, goal: Int, streak: Int) {
    val msg = when {
        sessionActive -> "Session in progress. Energy is accumulating. Stay with it, Explorer."
        doneToday == 0 && streak > 0 -> "Your streak is $streak days. One session today keeps it alive."
        doneToday == 0 -> "Daily target: $goal sessions. Start with one. Every minute of focus is one unit of energy."
        doneToday < goal -> "$doneToday of $goal sessions done today. One more and Nexus Prime hits today's target."
        else -> "Daily target reached ($doneToday/$goal). Great work. Rest is part of the mission too."
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CharacterPortrait(Character.ORION, size = 54.dp)
        Spacer(Modifier.width(6.dp))
        GlassPanel(Modifier.weight(1f), accent = Character.ORION.accent, padding = PaddingValues(horizontal = 12.dp, vertical = 9.dp), brackets = false, corner = 14.dp) {
            Text(msg, style = NexusType.Small, color = NexusColors.TextPrimary)
        }
    }
}

@Composable
private fun ActiveSessionBanner(vm: NexusViewModel, onOpen: () -> Unit) {
    val s = vm.openSession.collectAsStateWithLifecycle().value ?: return
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(s) { while (true) { now++; delay(1000) } }
    val ev = remember(s, now) { vm.engine.evaluate(s, vm.clock) }
    val (label, remaining) = when (ev) {
        is SessionEvaluation.Running -> "FOCUS ACTIVE" to ev.remainingMs
        is SessionEvaluation.Paused -> "PAUSED" to ev.remainingMs
        else -> "COMPLETING" to 0L
    }
    NexusPrimaryButton("$label · ${formatMs(remaining)} — return", onOpen, Modifier.fillMaxWidth(), leading = Glyph.CLOCK)
}

@Composable
private fun BottomDock(go: (Route) -> Unit) {
    val items = listOf(Triple(Glyph.MISSIONS, "Missions", Route.MISSIONS), Triple(Glyph.PLANETS, "Planets", Route.PLANETS),
        Triple(Glyph.RANKINGS, "Rankings", Route.RANKINGS), Triple(Glyph.PROFILE, "Profile", Route.PROFILE))
    GlassPanel(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 8.dp, horizontal = 4.dp), brackets = false, corner = 20.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { (g, label, r) ->
                Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { go(r) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    NexusIcon(g, NexusColors.TextSecondary, size = 22.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(label.uppercase(), style = NexusType.Label.copy(fontSize = 11.sp), color = NexusColors.TextSecondary)
                }
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
