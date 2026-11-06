package com.nexus.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.nexus.app.ui.theme.*
import com.nexus.domain.focus.SessionEvaluation
import com.nexus.world.scene.WorldHost
import com.nexus.world.scene.WorldState
import kotlinx.coroutines.delay

/** Screen 04 — Home (Universe): the player's headquarters, resources and next action (blueprint v2). */
@Composable
fun HomeScreen(vm: NexusViewModel, go: (Route) -> Unit) {
    val player by vm.player.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val open by vm.openSession.collectAsStateWithLifecycle()
    val p = player ?: return
    val civ = p.civilization
    var pulseKey by remember { mutableIntStateOf(0) }
    var gain by remember { mutableStateOf<Int?>(null) }
    var showOrion by remember { mutableStateOf(false) }

    // Reward feedback when arriving from a completed session.
    LaunchedEffect(Unit) {
        delay(600)
        val amount = vm.consumePulse()
        if (amount > 0) { pulseKey++; vm.audio.play(AudioController.Sfx.ENERGY); gain = amount; delay(2800); gain = null }
    }

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.padding(horizontal = Space.gutter).padding(top = Space.xs)) {
                PlayerTopBar(p.avatarId, p.username, "Lv. ${p.level.level} · ${p.level.rank.title}", onProfile = { go(Route.PROFILE) }) {
                    NexusIconButton(Glyph.BELL, { showOrion = true }, "ORION briefing", size = 44.dp)
                }
                Spacer(Modifier.height(Space.sm))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    CurrencyChip(Glyph.ENERGY, "%,d".format(p.energyBalance), NexusColors.NeonGreen, contentDescription = "${p.energyBalance} energy")
                    CurrencyChip(Glyph.CREDIT, "%,d".format(p.credits), NexusColors.Purple, contentDescription = "${p.credits} credits")
                    CurrencyChip(Glyph.STAR, "%,d XP".format(p.totalXp), NexusColors.CosmicGold, contentDescription = "${p.totalXp} experience points")
                    CurrencyChip(Glyph.FLAME, "${stats?.streak?.current ?: 0} day streak", NexusColors.Danger)
                }
            }

            // Hero: the headquarters (illustrated by default; the live 3D world is an option in Settings).
            Box(Modifier.weight(1f).fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) { go(Route.PLANETS) }, contentAlignment = Alignment.Center) {
                if (settings.liveWorld) LiveWorld(civ.overallDevelopment, civ.stage.ordinal, settings, pulseKey)
                else NexusIsland(civ.stage.ordinal, civ.progressToNext, civ.stage.title, Modifier.fillMaxSize().padding(horizontal = Space.xs), pulseKey)
                androidx.compose.animation.AnimatedVisibility(gain != null, Modifier.align(Alignment.BottomCenter).padding(bottom = Space.xs),
                    enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 24.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("+${gain ?: 0} energy channelled", style = NexusType.Heading, color = NexusColors.NeonGreen)
                    }
                }
            }

            Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                NexusCard(Modifier.fillMaxWidth(), onClick = { go(Route.PLANETS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("NEXUS PRIME", style = NexusType.Heading, color = NexusColors.TextPrimary)
                            Text(civ.stage.title, style = NexusType.Small, color = NexusColors.ElectricCyan)
                        }
                        Text("Lv. ${civ.stage.ordinal + 1}", style = NexusType.Numeric, color = NexusColors.CosmicGold)
                    }
                    Spacer(Modifier.height(Space.xs))
                    LabeledProgress(
                        if (civ.nextStage != null) "${civ.developmentEnergy} / ${civ.nextStage!!.requiredEnergy} energy to ${civ.nextStage!!.title}" else "Fully restored",
                        civ.progressToNext,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    QuickActionTile(Glyph.BUILD, "Build", { go(Route.BUILD) }, Modifier.weight(1f))
                    QuickActionTile(Glyph.MISSIONS, "Missions", { go(Route.MISSIONS) }, Modifier.weight(1f), NexusColors.Purple)
                    QuickActionTile(Glyph.PLANETS, "Planets", { go(Route.PLANETS) }, Modifier.weight(1f), NexusColors.CosmicGold)
                }
                if (open != null) ActiveSessionButton(vm, onOpen = { go(Route.FOCUS) })
                else NexusButton("Start Focus", { go(Route.FOCUS) }, Modifier.fillMaxWidth(), leading = Glyph.PLAY)
            }
            Spacer(Modifier.height(Space.xs))
            NexusBottomBar(NavDestination.UNIVERSE, { d ->
                when (d) {
                    NavDestination.UNIVERSE -> {}
                    NavDestination.MISSIONS -> go(Route.MISSIONS)
                    NavDestination.FOCUS -> go(Route.FOCUS)
                    NavDestination.ARENA -> go(Route.RANKINGS)
                    NavDestination.PROFILE -> go(Route.PROFILE)
                }
            }, Modifier.padding(horizontal = Space.xs), focusActive = open != null)
        }

        if (showOrion) OrionBriefing(vm, onDismiss = { showOrion = false })
    }
}

/** ORION's daily briefing, opened from the bell: real targets from today's data. */
@Composable
private fun OrionBriefing(vm: NexusViewModel, onDismiss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val open by vm.openSession.collectAsStateWithLifecycle()
    val today = stats?.recentDays?.firstOrNull { it.dayKey == java.time.LocalDate.now().toString() }
    val done = today?.completedSessions ?: 0; val goal = settings.dailyGoalSessions; val streak = stats?.streak?.current ?: 0
    val msg = when {
        open != null -> "A session is in progress. Energy is accumulating. Stay with it, Explorer."
        done == 0 && streak > 0 -> "Your streak is $streak days. One session today keeps it alive."
        done == 0 -> "Daily target: $goal sessions. Start with one. Every minute of focus is one unit of energy."
        done < goal -> "$done of $goal sessions done today. One more and Nexus Prime hits today's target."
        else -> "Daily target reached ($done/$goal). Great work. Rest is part of the mission too."
    }
    Box(Modifier.fillMaxSize().background(Color(0x99030A12)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)) {
        NexusCard(Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(Space.gutter).padding(top = 52.dp).fillMaxWidth(), tone = CardTone.Highlight) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CharacterPortrait(Character.ORION, size = 64.dp)
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text("ORION · Operations", style = NexusType.Label, color = NexusColors.ElectricCyan)
                    Spacer(Modifier.height(4.dp))
                    Text(msg, style = NexusType.Body, color = NexusColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(Space.sm))
            LabeledProgress("Today's sessions", done.toFloat() / goal.coerceAtLeast(1), valueText = "$done / $goal",
                colors = listOf(NexusColors.NeonGreen, NexusColors.Turquoise))
        }
    }
}

@Composable
private fun ActiveSessionButton(vm: NexusViewModel, onOpen: () -> Unit) {
    val s = vm.openSession.collectAsStateWithLifecycle().value ?: return
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(s) { while (true) { tick++; delay(1000) } }
    val ev = remember(s, tick) { vm.engine.evaluate(s, vm.clock) }
    val (label, remaining) = when (ev) {
        is SessionEvaluation.Running -> "Focusing" to ev.remainingMs
        is SessionEvaluation.Paused -> "Paused" to ev.remainingMs
        else -> "Completing" to 0L
    }
    NexusButton("$label · ${formatMs(remaining)}", onOpen, Modifier.fillMaxWidth(), leading = Glyph.CLOCK, trailing = Glyph.ARROW_RIGHT)
}

/** Optional live 3D world (Settings → Home planet → Live 3D), via the engine-agnostic WorldHost. */
@Composable
private fun LiveWorld(development: Float, stage: Int, settings: com.nexus.app.AppSettings, pulseKey: Int) {
    val factory = LocalContext.current.nexus.worldHostFactory
    var host by remember { mutableStateOf<WorldHost?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(factory = { ctx -> factory(ctx).also { host = it as WorldHost } }, modifier = Modifier.fillMaxSize(), update = { v ->
        val h = v as WorldHost; h.setQuality(settings.quality); h.setWorldState(WorldState(development = development, stage = stage))
    })
    DisposableEffect(lifecycle, host) {
        val h = host
        val obs = LifecycleEventObserver { _, e -> when (e) { Lifecycle.Event.ON_RESUME -> h?.onHostResume(); Lifecycle.Event.ON_PAUSE -> h?.onHostPause(); else -> {} } }
        if (h != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) h.onHostResume()
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); h?.onHostPause() }
    }
    LaunchedEffect(pulseKey, host) { if (pulseKey > 0) host?.playEnergyPulse(60) }
}

fun formatMs(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
