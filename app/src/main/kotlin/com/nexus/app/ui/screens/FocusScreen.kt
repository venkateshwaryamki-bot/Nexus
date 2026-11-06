package com.nexus.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import com.nexus.domain.focus.FocusRules
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.SessionEvaluation
import com.nexus.domain.reward.RewardPolicy
import kotlinx.coroutines.delay

/** Screen 05 — Focus Chamber. Setup when idle; the live chamber while a session is open. */
@Composable
fun FocusScreen(vm: NexusViewModel, onBack: () -> Unit, requestNotifications: () -> Unit) {
    val open by vm.openSession.collectAsStateWithLifecycle()
    val s = open
    if (s == null) FocusSetup(vm, onBack, requestNotifications) else FocusChamber(vm, s, onBack)
}

@Composable
private fun FocusSetup(vm: NexusViewModel, onBack: () -> Unit, requestNotifications: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val rules = FocusRules.Default
    var task by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(settings.defaultMinutes) }
    var custom by rememberSaveable { mutableStateOf(minutes !in rules.presetMinutes) }
    var envId by rememberSaveable { mutableStateOf(FocusEnvironment.COMMAND_CENTRE.id) }
    var showRules by rememberSaveable { mutableStateOf(false) }
    val level = player?.level?.level ?: 1
    val policy = RewardPolicy.Default

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScreenHeader("Focus Chamber", "Prepare your session", onBack)
            Spacer(Modifier.height(16.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionLabel("Mission objective")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(task, { if (it.length <= 60) task = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What will you focus on?", style = NexusType.Body) },
                    textStyle = NexusType.BodyStrong.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexusColors.ElectricCyan, unfocusedBorderColor = NexusColors.PanelBorder, cursorColor = NexusColors.ElectricCyan))
                Spacer(Modifier.height(18.dp))
                SectionLabel("Duration")
                Spacer(Modifier.height(10.dp))
                val options = rules.presetMinutes.map { it.toString() } + "Custom"
                for (row in options.chunked(3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { o ->
                            val sel = if (o == "Custom") custom else !custom && minutes == o.toInt()
                            Chip(if (o == "Custom") o else "$o min", sel, Modifier.weight(1f)) {
                                if (o == "Custom") custom = true else { custom = false; minutes = o.toInt() }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                AnimatedVisibility(custom) {
                    Column {
                        Text("$minutes minutes", style = NexusType.Numeric, color = Color.White)
                        Slider(minutes.toFloat(), { minutes = it.toInt() }, valueRange = rules.minDurationMinutes.toFloat()..rules.maxDurationMinutes.toFloat(),
                            steps = (rules.maxDurationMinutes - rules.minDurationMinutes) / 5 - 1,
                            colors = SliderDefaults.colors(thumbColor = NexusColors.ElectricCyan, activeTrackColor = NexusColors.ElectricCyan, inactiveTrackColor = NexusColors.PanelBorder))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("+${minutes * policy.energyPerMinute} energy · +${minutes * policy.xpPerMinute + policy.completionBonusXp} XP on completion",
                        style = NexusType.BodyStrong, color = NexusColors.NeonGreen)
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionLabel("Environment")
                Spacer(Modifier.height(10.dp))
                FocusEnvironment.entries.forEach { env ->
                    val unlocked = level >= env.requiredLevel
                    val sel = envId == env.id
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                        .border(1.dp, if (sel) env.accent else NexusColors.PanelBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable(enabled = unlocked) { envId = env.id }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val t by rememberSceneTime()
                        Canvas(Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(8.dp))) { drawEnvironment(env, t, 0.5f) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(env.title, style = NexusType.BodyStrong, color = if (unlocked) Color.White else NexusColors.TextMuted)
                            Text(if (unlocked) "Unlocked" else "Unlocks at level ${env.requiredLevel}", style = NexusType.Small,
                                color = if (unlocked) env.accent else NexusColors.TextMuted)
                        }
                        if (!unlocked) NexusIcon(Glyph.LOCK, NexusColors.TextMuted, size = 18.dp)
                        else if (sel) NexusIcon(Glyph.CHECK, env.accent, size = 20.dp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassPanel(Modifier.fillMaxWidth().clickable { showRules = !showRules }, accent = NexusColors.CosmicGold, brackets = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Session rules", color = NexusColors.CosmicGold, modifier = Modifier.weight(1f))
                    Text(if (showRules) "HIDE" else "SHOW", style = NexusType.Label, color = NexusColors.CosmicGold)
                }
                AnimatedVisibility(showRules) { Column(Modifier.padding(top = 8.dp)) { RulesText(rules, policy) } }
            }
            Spacer(Modifier.height(18.dp))
            val error by vm.error.collectAsStateWithLifecycle()
            if (error != null) { Text(error!!, style = NexusType.Small, color = NexusColors.Danger); Spacer(Modifier.height(8.dp)) }
            NexusPrimaryButton("Begin focus · $minutes min", {
                requestNotifications(); vm.clearError(); vm.startSession(task, minutes, envId)
            }, Modifier.fillMaxWidth(), leading = Glyph.PLAY)
            Spacer(Modifier.height(24.dp))
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

@Composable
private fun FocusChamber(vm: NexusViewModel, s: FocusSession, onBack: () -> Unit) {
    val env = FocusEnvironment.byId(s.environmentId)
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(250); tick++ } }
    // Recompute from persisted timestamps every tick — the UI never owns timer state.
    val ev = remember(s, tick) { vm.engine.evaluate(s, vm.clock) }
    LaunchedEffect(ev) { if (ev is SessionEvaluation.DueForCompletion || ev is SessionEvaluation.DueForTermination) vm.reconcile() }
    var confirmEnd by remember { mutableStateOf(false) }

    val focused = when (ev) { is SessionEvaluation.Running -> ev.focusedMs; is SessionEvaluation.Paused -> ev.focusedMs; else -> s.plannedMs }
    val remaining = when (ev) { is SessionEvaluation.Running -> ev.remainingMs; is SessionEvaluation.Paused -> ev.remainingMs; else -> 0L }
    val progress = focused.toFloat() / s.plannedMs
    val paused = ev is SessionEvaluation.Paused

    Box(Modifier.fillMaxSize()) {
        val t by rememberSceneTime()
        Canvas(Modifier.fillMaxSize()) { drawEnvironment(env, t, progress) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66061321), Color(0x22061321), Color(0xCC061321)))))
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ScreenHeader(env.title, if (paused) "Session paused" else "Focus state active", onBack, accent = env.accent)
            Spacer(Modifier.height(10.dp))
            Text(s.task.ifBlank { "Deep focus" }, style = NexusType.Heading, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(280.dp)) {
                    val sw = 10.dp.toPx(); val inset = sw
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(Color(0x3327DFFF), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
                    drawArc(Brush.sweepGradient(listOf(env.accent, NexusColors.NeonGreen, env.accent)), -90f, 360f * progress, false,
                        Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                    for (k in 0 until 60) {
                        val a = Math.toRadians(k * 6.0 - 90).toFloat(); val r1 = size.width / 2 - sw * 2.2f; val r2 = r1 - if (k % 5 == 0) 12f else 6f
                        val on = k / 60f <= progress
                        drawLine((if (on) env.accent else Color(0x4427DFFF)), Offset(center.x + kotlin.math.cos(a) * r1, center.y + kotlin.math.sin(a) * r1),
                            Offset(center.x + kotlin.math.cos(a) * r2, center.y + kotlin.math.sin(a) * r2), 2f)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatMs(remaining), style = NexusType.Timer, color = Color.White)
                    Text(if (paused) "PAUSED" else "REMAINING", style = NexusType.Label, color = if (paused) NexusColors.CosmicGold else env.accent)
                }
            }
            Spacer(Modifier.height(20.dp))
            // Energy accumulation indicator: charges toward the completion reward.
            GlassPanel(Modifier.fillMaxWidth(), accent = NexusColors.NeonGreen, padding = PaddingValues(14.dp), brackets = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexusIcon(Glyph.ENERGY, NexusColors.NeonGreen, size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("ENERGY CHARGING", style = NexusType.Label, color = NexusColors.NeonGreen, modifier = Modifier.weight(1f))
                    Text("${(focused / 60_000)} / ${s.plannedMinutes}", style = NexusType.Numeric.copy(fontSize = 15.sp), color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                EnergyBar(progress, colors = listOf(NexusColors.NeonGreen, env.accent))
                if (ev is SessionEvaluation.Paused) {
                    Spacer(Modifier.height(8.dp))
                    Text("Pause allowance left: ${formatMs(ev.pauseRemainingMs)} · pauses used ${s.pauseCount}/${FocusRules.Default.maxPauses}",
                        style = NexusType.Small, color = NexusColors.CosmicGold)
                } else if (s.interruptions > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Interruptions recorded: ${s.interruptions}", style = NexusType.Small, color = NexusColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (paused) NexusPrimaryButton("Resume", { vm.resume() }, Modifier.weight(1f), leading = Glyph.PLAY)
                else NexusSecondaryButton(if (vm.engine.canPause(s)) "Pause (${FocusRules.Default.maxPauses - s.pauseCount} left)" else "No pauses left",
                    { vm.pause() }, Modifier.weight(1f), enabled = vm.engine.canPause(s), leading = Glyph.PAUSE)
                NexusSecondaryButton("End", { confirmEnd = true }, Modifier.weight(0.6f), accent = NexusColors.Danger, leading = Glyph.STOP)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmEnd) {
        val minEarly = FocusRules.Default.partialRewardMinFocusMs
        val partial = (focused / 60_000 * RewardPolicy.Default.partialEnergyFactor).toInt()
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = NexusColors.MidnightBlue,
            title = { Text("End session early?", style = NexusType.Title, color = Color.White) },
            text = {
                Text(if (focused >= minEarly) "You've focused ${focused / 60_000} minutes. Ending now earns about $partial energy (half rate). Completing the session earns ${s.plannedMinutes}."
                    else "You've focused less than ${minEarly / 60_000} minutes, so ending now earns no energy. Your time is still recorded.",
                    style = NexusType.Body, color = NexusColors.TextSecondary)
            },
            confirmButton = { TextButton({ confirmEnd = false; vm.endEarly() }) { Text("END SESSION", style = NexusType.Label, color = NexusColors.Danger) } },
            dismissButton = { TextButton({ confirmEnd = false }) { Text("KEEP FOCUSING", style = NexusType.Label, color = NexusColors.ElectricCyan) } },
        )
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.clip(shape).background(if (selected) NexusColors.ElectricCyan.copy(alpha = 0.18f) else Color(0x1427DFFF))
        .border(1.dp, if (selected) NexusColors.ElectricCyan else NexusColors.PanelBorder.copy(alpha = 0.2f), shape)
        .clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text.uppercase(), style = NexusType.Label, color = if (selected) Color.White else NexusColors.TextSecondary)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String?, onBack: () -> Unit, accent: Color = NexusColors.ElectricCyan, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x3310283F)).border(1.dp, NexusColors.PanelBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onBack), contentAlignment = Alignment.Center) { NexusIcon(Glyph.BACK, Color.White, size = 20.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = NexusType.Title.copy(fontSize = 22.sp), color = Color.White, maxLines = 1)
            if (subtitle != null) Text(subtitle.uppercase(), style = NexusType.Label, color = accent)
        }
        trailing()
    }
}
