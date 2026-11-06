package com.nexus.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.R
import com.nexus.app.platform.AmbientSoundEffect
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.*
import com.nexus.domain.focus.FocusRules
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.SessionEvaluation
import com.nexus.domain.reward.RewardPolicy
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Screen 05 — Focus Chamber. Setup when idle; the live chamber while a session is open. */
@Composable
fun FocusScreen(vm: NexusViewModel, onBack: () -> Unit, requestNotifications: () -> Unit) {
    val open by vm.openSession.collectAsStateWithLifecycle()
    val s = open
    if (s == null) FocusSetup(vm, onBack, requestNotifications) else FocusChamber(vm, s, onBack)
}

private fun backdropOf(env: FocusEnvironment) = when (env) {
    FocusEnvironment.COMMAND_CENTRE -> R.drawable.chamber_command_centre
    FocusEnvironment.OBSERVATORY -> R.drawable.chamber_observatory
    FocusEnvironment.AETHER_FOREST -> R.drawable.chamber_aether_forest
    FocusEnvironment.TITAN_STATION -> R.drawable.chamber_titan_station
}
private fun thumbOf(env: FocusEnvironment) = when (env) {
    FocusEnvironment.COMMAND_CENTRE -> R.drawable.chamber_thumb_command_centre
    FocusEnvironment.OBSERVATORY -> R.drawable.chamber_thumb_observatory
    FocusEnvironment.AETHER_FOREST -> R.drawable.chamber_thumb_aether_forest
    FocusEnvironment.TITAN_STATION -> R.drawable.chamber_thumb_titan_station
}

/** Pre-rendered chamber backdrop, scaled to fill height (layout contract: portal ≈ 36%, pedestal ≈ 70% of height). */
@Composable
private fun ChamberBackdrop(env: FocusEnvironment, dim: Float = 0f) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(backdropOf(env)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0xCC06111E), 0.18f to Color(0x3306111E), 0.6f to Color(0x2206111E), 1f to Color(0xE606111E))))
        if (dim > 0f) Box(Modifier.fillMaxSize().background(NexusColors.DeepSpaceNavy.copy(alpha = dim)))
    }
}

// ============================================================================================
// Setup
// ============================================================================================

@Composable
private fun FocusSetup(vm: NexusViewModel, onBack: () -> Unit, requestNotifications: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val recent by vm.recentTasks.collectAsStateWithLifecycle()
    val rules = FocusRules.Default
    var task by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(settings.defaultMinutes) }
    var custom by rememberSaveable { mutableStateOf(minutes !in rules.presetMinutes) }
    var envId by rememberSaveable { mutableStateOf(FocusEnvironment.COMMAND_CENTRE.id) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    val level = player?.level?.level ?: 1
    val policy = RewardPolicy.Default
    val env = FocusEnvironment.byId(envId)

    Box(Modifier.fillMaxSize()) {
        ChamberBackdrop(env, dim = 0.55f)
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Spacer(Modifier.height(Space.xs))
                ScreenHeader("Focus Chamber", "Prepare your session", onBack)
                NexusCard(Modifier.fillMaxWidth()) {
                    SectionLabel("What will you focus on?")
                    Spacer(Modifier.height(Space.xs))
                    OutlinedTextField(task, { if (it.length <= 60) task = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Read chapter 4", style = NexusType.Body) },
                        leadingIcon = { NexusIcon(Glyph.BOOK, NexusColors.ElectricCyan, size = 20.dp) },
                        textStyle = NexusType.BodyStrong.copy(color = NexusColors.TextPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), shape = RoundedCornerShape(Radius.md),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexusColors.ElectricCyan, unfocusedBorderColor = NexusColors.PanelBorder, cursorColor = NexusColors.ElectricCyan))
                    if (recent.isNotEmpty()) {
                        Spacer(Modifier.height(Space.xs))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            recent.forEach { r -> Chip(r, task == r) { task = r } }
                        }
                    }
                }
                NexusCard(Modifier.fillMaxWidth()) {
                    SectionLabel("Duration")
                    Spacer(Modifier.height(Space.xs))
                    val options = rules.presetMinutes.map { it.toString() } + "Custom"
                    options.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            row.forEach { o ->
                                val sel = if (o == "Custom") custom else !custom && minutes == o.toInt()
                                Chip(if (o == "Custom") o else "$o min", sel, Modifier.weight(1f)) {
                                    if (o == "Custom") custom = true else { custom = false; minutes = o.toInt() }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(custom) {
                        Column {
                            Text("$minutes minutes", style = NexusType.Numeric, color = NexusColors.TextPrimary)
                            Slider(minutes.toFloat(), { minutes = it.toInt() }, valueRange = rules.minDurationMinutes.toFloat()..rules.maxDurationMinutes.toFloat(),
                                steps = (rules.maxDurationMinutes - rules.minDurationMinutes) / 5 - 1,
                                colors = SliderDefaults.colors(thumbColor = NexusColors.ElectricCyan, activeTrackColor = NexusColors.ElectricCyan, inactiveTrackColor = NexusColors.PanelBorder))
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                        Text("On completion", style = NexusType.Small, color = NexusColors.TextSecondary)
                        CurrencyChip(Glyph.ENERGY, "+${minutes * policy.energyPerMinute} energy", NexusColors.NeonGreen)
                        CurrencyChip(Glyph.STAR, "+${minutes * policy.xpPerMinute + policy.completionBonusXp} XP", NexusColors.CosmicGold)
                    }
                }
                NexusCard(Modifier.fillMaxWidth()) {
                    SectionLabel("Environment")
                    Spacer(Modifier.height(Space.xs))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        FocusEnvironment.entries.forEach { e ->
                            val unlocked = level >= e.requiredLevel
                            val sel = envId == e.id
                            Column(Modifier.width(104.dp).clip(RoundedCornerShape(Radius.md))
                                .border(if (sel) 2.dp else 1.dp, if (sel) e.accent else NexusColors.PanelBorder, RoundedCornerShape(Radius.md))
                                .clickable(enabled = unlocked) { envId = e.id }
                                .semantics { contentDescription = e.title + if (unlocked) "" else ", locked until level ${e.requiredLevel}" }) {
                                Box {
                                    Image(painterResource(thumbOf(e)), null, Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop,
                                        alpha = if (unlocked) 1f else 0.35f)
                                    if (!unlocked) Box(Modifier.align(Alignment.Center).size(34.dp).clip(RoundedCornerShape(Radius.pill)).background(Color(0xCC06111E)),
                                        contentAlignment = Alignment.Center) { NexusIcon(Glyph.LOCK, NexusColors.TextSecondary, size = 16.dp) }
                                }
                                Column(Modifier.background(NexusColors.Card).fillMaxWidth().padding(8.dp)) {
                                    Text(e.title, style = NexusType.Caption, color = if (unlocked) NexusColors.TextPrimary else NexusColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (unlocked) (if (sel) "Selected" else "Unlocked") else "Lv. ${e.requiredLevel}", style = NexusType.Caption,
                                        color = if (unlocked) e.accent else NexusColors.TextMuted)
                                }
                            }
                        }
                    }
                }
                NexusCard(Modifier.fillMaxWidth(), tone = CardTone.Premium, onClick = { showRules = !showRules }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Session rules", color = NexusColors.CosmicGold, modifier = Modifier.weight(1f))
                        NexusIcon(Glyph.CHEVRON_DOWN, NexusColors.CosmicGold, size = 18.dp, modifier = Modifier.graphicsLayer { rotationZ = if (showRules) 180f else 0f })
                    }
                    AnimatedVisibility(showRules) { Column(Modifier.padding(top = Space.xs)) { RulesText(rules, policy) } }
                }
                val error by vm.error.collectAsStateWithLifecycle()
                if (error != null) Text(error!!, style = NexusType.Small, color = NexusColors.Danger)
                Spacer(Modifier.height(Space.xs))
            }
            NexusButton("Begin focus · $minutes min", {
                requestNotifications(); vm.clearError(); vm.startSession(task, minutes, envId)
            }, Modifier.fillMaxWidth().padding(Space.gutter), leading = Glyph.PLAY)
        }
    }
}

@Composable
fun RulesText(rules: FocusRules, policy: RewardPolicy) {
    val items = listOf(
        "Complete the full session: 1 energy per minute, ${policy.xpPerMinute} XP per minute + ${policy.completionBonusXp} bonus XP.",
        "Up to ${rules.maxPauses} pauses, ${rules.maxTotalPauseMs / 60_000} minutes in total. Paused time doesn't count as focus. Go over the limit and the session ends.",
        "End early after at least ${rules.partialRewardMinFocusMs / 60_000} focused minutes and you keep half the energy. Before that, no energy.",
        "The timer runs on device time, so it keeps going if you close the app or the phone sleeps. Changing the clock doesn't speed it up.",
        "Leaving NEXUS for other apps is recorded as an interruption, and you'll see it in your debrief. Turning the screen off is not.",
        "Healthy-play limit: after ${policy.fullRateDailyMinutes / 60} h of focus in a day, energy is earned at half rate. After ${policy.dailyEnergyCapMinutes / 60} h, no energy. Rest is part of the mission.",
    )
    items.forEach { Row(Modifier.padding(vertical = 3.dp)) { Text("›  ", style = NexusType.Small, color = NexusColors.CosmicGold); Text(it, style = NexusType.Small, color = NexusColors.TextSecondary) } }
}

// ============================================================================================
// Live chamber
// ============================================================================================

@Composable
private fun FocusChamber(vm: NexusViewModel, s: FocusSession, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val reduce = LocalNexusPrefs.current.reduceMotion
    var env by remember(s.id) { mutableStateOf(FocusEnvironment.byId(s.environmentId)) }
    val level = vm.player.collectAsStateWithLifecycle().value?.level?.level ?: 1

    // Text-level state recomputed 4×/s from persisted timestamps — the UI never owns timer state.
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(250); tick++ } }
    val ev = remember(s, tick) { vm.engine.evaluate(s, vm.clock) }
    // Frame clock for the ring/core (draw phase only, no recomposition per frame).
    val frameMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(reduce) {
        if (reduce) while (true) { frameMs.longValue = System.nanoTime() / 1_000_000; delay(1000) }
        else while (true) withFrameMillis { frameMs.longValue = it }
    }

    // Completion moment: play the burst, then settle (completion time is back-dated by the engine).
    val burst = remember { Animatable(0f) }
    var completing by remember(s.id) { mutableStateOf(false) }
    LaunchedEffect(ev) {
        when (ev) {
            is SessionEvaluation.DueForCompletion -> if (!completing) {
                completing = true
                burst.animateTo(1f, tween(if (reduce) 300 else 1700, easing = FastOutSlowInEasing)); vm.reconcile()
            }
            is SessionEvaluation.DueForTermination -> vm.reconcile()
            else -> {}
        }
    }

    val focused = when (ev) { is SessionEvaluation.Running -> ev.focusedMs; is SessionEvaluation.Paused -> ev.focusedMs; else -> s.plannedMs }
    val remaining = when (ev) { is SessionEvaluation.Running -> ev.remainingMs; is SessionEvaluation.Paused -> ev.remainingMs; else -> 0L }
    val paused = ev is SessionEvaluation.Paused
    var confirmEnd by remember { mutableStateOf(false) }
    AmbientSoundEffect(settings.ambientSound && !paused)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        val backdropW = maxOf(w, h * 9f / 16f)
        val ringSize: Dp = minOf(backdropW * 0.6f, w * 0.86f)
        val coreSize: Dp = backdropW * 0.2f

        ChamberBackdrop(env, dim = if (paused) 0.35f else 0f)

        // Energy core floating above the pedestal; grows brighter and larger as energy charges.
        val chargeAnim by animateFloatAsState((focused.toFloat() / s.plannedMs).coerceIn(0f, 1f), tween(800), label = "charge")
        Box(Modifier.offset(x = (w - coreSize) / 2, y = h * 0.645f - coreSize / 2).size(coreSize)) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = 1.8f; scaleY = 1.8f }) {
                val t = frameMs.longValue / 1000f
                val b = burst.value
                val a = (0.18f + 0.35f * chargeAnim) * (if (paused) 0.4f else 0.85f + 0.15f * sin(t * 2.2f)) + b * 0.6f
                drawCircle(Brush.radialGradient(listOf(env.accent.copy(alpha = a.coerceAtMost(1f)), Color.Transparent), center, size.minDimension / 2), size.minDimension / 2)
            }
            Image(painterResource(R.drawable.energy_core), "Energy core, ${(chargeAnim * 100).toInt()}% charged", Modifier.fillMaxSize().graphicsLayer {
                val t = frameMs.longValue / 1000f
                val b = burst.value
                translationY = if (reduce || paused) 0f else sin(t * 1.4f) * 7f * density
                val sc = 0.78f + 0.22f * chargeAnim + sin(b * PI.toFloat()) * 0.35f
                scaleX = sc; scaleY = sc
                alpha = (if (paused) 0.55f else 1f) * (1f - (b - 0.75f).coerceAtLeast(0f) * 2f)
            })
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Space.gutter)) {
            Spacer(Modifier.height(Space.xs))
            ScreenHeader("Focus Chamber", env.title, onBack, accent = env.accent)
            Spacer(Modifier.height(Space.sm))
            Row(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(Radius.pill)).background(Color(0xB30A1B2B))
                .border(1.dp, NexusColors.PanelBorder, RoundedCornerShape(Radius.pill)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                NexusIcon(Glyph.BOOK, env.accent, size = 18.dp); Spacer(Modifier.width(8.dp))
                Text(s.task.ifBlank { "Deep focus" }, style = NexusType.BodyStrong, color = NexusColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Timer ring centred on the portal (≈36% of height).
        Box(Modifier.offset(x = (w - ringSize) / 2, y = h * 0.36f - ringSize / 2).size(ringSize), contentAlignment = Alignment.Center) {
            TimerRing(s, vm, frameMs, paused, env.accent, burst, Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMs(remaining), style = NexusType.Timer, color = NexusColors.TextPrimary,
                    modifier = Modifier.semantics { contentDescription = "${remaining / 60_000} minutes ${(remaining / 1000) % 60} seconds remaining" })
                Text(when { completing -> "Session complete"; paused -> "Paused"; else -> "Focus Session" }, style = NexusType.BodyStrong,
                    color = when { completing -> NexusColors.NeonGreen; paused -> NexusColors.CosmicGold; else -> env.accent })
            }
        }

        // Controls + energy charging (bottom).
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = Space.gutter, vertical = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                LabeledIconButton(Glyph.AMBIENT, "Ambient", env.accent) {
                    val unlocked = FocusEnvironment.entries.filter { level >= it.requiredLevel }
                    env = unlocked[(unlocked.indexOf(env) + 1) % unlocked.size]
                }
                if (paused) NexusIconButton(Glyph.PLAY, { vm.resume() }, "Resume", size = 76.dp, highlighted = true)
                else NexusIconButton(Glyph.PAUSE, { vm.pause() }, if (vm.engine.canPause(s)) "Pause" else "No pauses left", size = 76.dp, highlighted = true,
                    enabled = vm.engine.canPause(s) && !completing)
                LabeledIconButton(Glyph.SOUND, if (settings.ambientSound) "Sound on" else "Sound off",
                    if (settings.ambientSound) env.accent else NexusColors.TextSecondary) { vm.putSetting("ambient_sound", !settings.ambientSound) }
            }
            NexusCard(Modifier.fillMaxWidth(), tone = CardTone.Success, padding = PaddingValues(horizontal = Space.md, vertical = Space.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 18.dp); Spacer(Modifier.width(6.dp))
                    Text("+${s.plannedMinutes} Energy", style = NexusType.BodyStrong, color = NexusColors.NeonGreen)
                    Spacer(Modifier.width(Space.sm))
                    NexusIcon(Glyph.STAR, NexusColors.CosmicGold, size = 16.dp); Spacer(Modifier.width(4.dp))
                    Text("+${s.plannedMinutes * RewardPolicy.Default.xpPerMinute + RewardPolicy.Default.completionBonusXp} XP", style = NexusType.BodyStrong, color = NexusColors.CosmicGold)
                    Spacer(Modifier.weight(1f))
                    Text("${focused / 60_000} / ${s.plannedMinutes} min", style = NexusType.NumericSmall, color = NexusColors.TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                EnergyBar(chargeAnim, Modifier.fillMaxWidth(), colors = listOf(NexusColors.NeonGreen, env.accent))
                val note = when {
                    ev is SessionEvaluation.Paused -> "Pause allowance left ${formatMs(ev.pauseRemainingMs)} · pauses used ${s.pauseCount}/${FocusRules.Default.maxPauses}"
                    s.interruptions > 0 -> "Interruptions recorded: ${s.interruptions}"
                    else -> null
                }
                if (note != null) { Spacer(Modifier.height(6.dp)); Text(note, style = NexusType.Small, color = if (paused) NexusColors.CosmicGold else NexusColors.TextSecondary) }
            }
            NexusButton("End session", { confirmEnd = true }, variant = ButtonVariant.Ghost, size = ButtonSize.Small, leading = Glyph.STOP, enabled = !completing)
        }

        // Completion flash.
        if (burst.value > 0f) Canvas(Modifier.fillMaxSize()) {
            val b = burst.value
            drawRect(NexusColors.NeonGreen.copy(alpha = sin(b * PI.toFloat()) * 0.18f))
            val c = Offset(size.width / 2, size.height * 0.645f)
            for (i in 0 until 28) {
                val a = i * (2 * PI.toFloat() / 28)
                val r = size.minDimension * 0.08f + b * size.minDimension * 0.55f
                drawCircle((if (i % 3 == 0) NexusColors.NeonGreen else env.accent).copy(alpha = (1f - b)), (3f + (i % 3)) * density, Offset(c.x + cos(a) * r, c.y + sin(a) * r * 0.8f))
            }
        }
    }

    if (confirmEnd) {
        val minEarly = FocusRules.Default.partialRewardMinFocusMs
        val partial = (focused / 60_000 * RewardPolicy.Default.partialEnergyFactor).toInt()
        AlertDialog(
            onDismissRequest = { confirmEnd = false }, containerColor = NexusColors.MidnightBlue,
            title = { Text("End session early?", style = NexusType.Title, color = NexusColors.TextPrimary) },
            text = {
                Text(if (focused >= minEarly) "You've focused ${focused / 60_000} minutes. Ending now earns about $partial energy (half rate). Completing the session earns ${s.plannedMinutes}."
                    else "You've focused less than ${minEarly / 60_000} minutes, so ending now earns no energy. Your time is still recorded.",
                    style = NexusType.Body, color = NexusColors.TextSecondary)
            },
            confirmButton = { TextButton({ confirmEnd = false; vm.endEarly() }) { Text("End session", style = NexusType.Button, color = NexusColors.Danger) } },
            dismissButton = { TextButton({ confirmEnd = false }) { Text("Keep focusing", style = NexusType.Button, color = NexusColors.ElectricCyan) } },
        )
    }
}

/** Progress ring: recomputed every frame from the session's timestamps for perfectly smooth motion. */
@Composable
private fun TimerRing(s: FocusSession, vm: NexusViewModel, frameMs: State<Long>, paused: Boolean, accent: Color, burst: Animatable<Float, AnimationVector1D>, modifier: Modifier) {
    val density = LocalDensity.current
    Canvas(modifier) {
        frameMs.value // subscribe to frames
        val progress = (vm.engine.focusedMs(s, vm.clock).toFloat() / s.plannedMs).coerceIn(0f, 1f).let { if (burst.value > 0f) maxOf(it, burst.value) else it }
        val sw = with(density) { 9.dp.toPx() }
        val inset = sw * 2.2f
        val arc = Size(size.width - inset * 2, size.height - inset * 2)
        val tl = Offset(inset, inset)
        val col = if (paused) NexusColors.CosmicGold else accent
        // Backing track + 60 ticks.
        drawArc(Color(0x5506111E), 0f, 360f, false, tl, arc, style = Stroke(sw * 1.6f))
        drawArc(col.copy(alpha = 0.16f), 0f, 360f, false, tl, arc, style = Stroke(sw))
        val r1 = size.width / 2 - inset - sw * 1.6f
        for (k in 0 until 60) {
            val a = Math.toRadians(k * 6.0 - 90).toFloat(); val major = k % 5 == 0
            val on = k / 60f < progress
            val r2 = r1 - if (major) sw * 1.2f else sw * 0.6f
            drawLine(if (on) col.copy(alpha = 0.9f) else Color(0x5592A6B8), Offset(center.x + cos(a) * r1, center.y + sin(a) * r1),
                Offset(center.x + cos(a) * r2, center.y + sin(a) * r2), if (major) 3f else 2f, StrokeCap.Round)
        }
        // Glow halo + progress arc.
        drawArc(col.copy(alpha = 0.22f), -90f, 360f * progress, false, tl, arc, style = Stroke(sw * 3f, cap = StrokeCap.Round))
        drawArc(Brush.sweepGradient(listOf(col.copy(alpha = 0.7f), col, NexusColors.Turquoise, col.copy(alpha = 0.7f))), -90f, 360f * progress, false, tl, arc,
            style = Stroke(sw, cap = StrokeCap.Round))
        // Leading head with glow.
        if (progress > 0f) {
            val a = Math.toRadians(360.0 * progress - 90).toFloat(); val rr = arc.width / 2
            val head = Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.8f), Color.Transparent), head, sw * 3f), sw * 3f, head)
            drawCircle(Color.White, sw * 0.55f, head)
        }
    }
}

@Composable
private fun LabeledIconButton(glyph: Glyph, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NexusIconButton(glyph, onClick, label, size = 52.dp, tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(label, style = NexusType.Caption, color = NexusColors.TextSecondary)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.md)
    Box(modifier.clip(shape).background(if (selected) NexusColors.ElectricCyan.copy(alpha = 0.18f) else NexusColors.MidnightBlue)
        .border(1.dp, if (selected) NexusColors.ElectricCyan else NexusColors.PanelBorder, shape)
        .clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = NexusType.BodyStrong, color = if (selected) NexusColors.TextPrimary else NexusColors.TextSecondary, maxLines = 1)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String?, onBack: () -> Unit, accent: Color = NexusColors.ElectricCyan, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NexusIconButton(Glyph.BACK, onBack, "Back", size = 44.dp)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = NexusType.Title.copy(fontSize = 22.sp), color = NexusColors.TextPrimary, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = NexusType.Small, color = accent)
        }
        trailing()
    }
}
