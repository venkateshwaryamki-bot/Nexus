package com.nexus.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import com.nexus.world.scene.GraphicsQuality

/** Screen 13 — Settings. Everything here is functional; planned items are labelled as such. */
@Composable
fun SettingsScreen(vm: NexusViewModel, onBack: () -> Unit, onReplayIntro: () -> Unit, onDesignSystem: () -> Unit = {}) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val notifOk = remember(refresh) { vm.platform.canPostNotifications() }
    val exactOk = remember(refresh) { vm.platform.canScheduleExactAlarms() }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { refresh++; onPauseOrDispose { } }

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScreenHeader("Settings", "Systems configuration", onBack)
            Spacer(Modifier.height(16.dp))
            Section("Audio & feel") {
                Toggle("Sound effects", s.audio) { vm.putSetting("audio", it) }
                Toggle("Haptic feedback", s.haptics) { vm.putSetting("haptics", it) }
            }
            Section("Graphics") {
                Text("3D world quality", style = NexusType.BodyStrong, color = Color.White)
                Text("Low renders at 60% resolution and 30 FPS for older or battery-saving devices.", style = NexusType.Small, color = NexusColors.TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsQuality.entries.forEach { q ->
                        if (q == s.quality) NexusPrimaryButton(q.name, {}, Modifier.weight(1f))
                        else NexusSecondaryButton(q.name, { vm.putSetting("quality", q.name) }, Modifier.weight(1f))
                    }
                }
            }
            Section("Home planet") {
                Toggle("Live 3D planet (experimental)", s.liveWorld) { vm.putSetting("live_world", it) }
                Text("Off: the illustrated planet (recommended, lighter on battery). On: the real-time 3D renderer using the quality setting above.",
                    style = NexusType.Small, color = NexusColors.TextMuted)
            }
            Section("Focus") {
                Text("Default duration: ${s.defaultMinutes} min", style = NexusType.BodyStrong, color = Color.White)
                Slider(s.defaultMinutes.toFloat(), { vm.putSetting("default_minutes", (it / 5).toInt() * 5) }, valueRange = 5f..180f,
                    colors = SliderDefaults.colors(thumbColor = NexusColors.ElectricCyan, activeTrackColor = NexusColors.ElectricCyan))
                Text("Daily target: ${s.dailyGoalSessions} sessions", style = NexusType.BodyStrong, color = Color.White)
                Slider(s.dailyGoalSessions.toFloat(), { vm.putSetting("daily_goal", it.toInt()) }, valueRange = 1f..8f, steps = 6,
                    colors = SliderDefaults.colors(thumbColor = NexusColors.ElectricCyan, activeTrackColor = NexusColors.ElectricCyan))
                Spacer(Modifier.height(6.dp))
                SectionLabel("About distraction blocking", color = NexusColors.CosmicGold)
                Text("NEXUS does not block other apps. Android only allows that through special permissions (such as Accessibility or Device Admin) that NEXUS does not use. Instead, NEXUS records when you leave the app during a session and shows it in your debrief.",
                    style = NexusType.Small, color = NexusColors.TextSecondary)
            }
            Section("Notifications & timing") {
                Status("Session notifications", notifOk, "Open settings") {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
                Status("Exact completion alarm", exactOk, "Allow") {
                    if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                }
                Text("Without exact alarms, Android may send the completion notification a few minutes late. Your session time and rewards are always exact.",
                    style = NexusType.Small, color = NexusColors.TextMuted)
            }
            Section("Accessibility") {
                Toggle("Reduce motion", s.reduceMotion) { vm.putSetting("reduce_motion", it) }
                Text("Freezes background animation and shortens the intro.", style = NexusType.Small, color = NexusColors.TextMuted)
            }
            Section("Privacy & account") {
                Text("Account: Guest (local). All data stays on this device. NEXUS has no internet permission in this version.", style = NexusType.Small, color = NexusColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                NexusSecondaryButton("Data export (coming soon)", {}, Modifier.fillMaxWidth(), enabled = false)
            }
            Section("Universe") {
                NexusSecondaryButton("Replay intro", onReplayIntro, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                NexusSecondaryButton("Design system", onDesignSystem, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                NexusSecondaryButton("Reset all progress", { confirmReset = true }, Modifier.fillMaxWidth(), accent = NexusColors.Danger)
            }
            Text("NEXUS 0.1.0 · Milestone 1", style = NexusType.Small, color = NexusColors.TextMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false }, containerColor = NexusColors.MidnightBlue,
        title = { Text("Reset all progress?", style = NexusType.Title, color = Color.White) },
        text = { Text("This permanently deletes your energy, XP, sessions, statistics and planet progress. It cannot be undone.", style = NexusType.Body, color = NexusColors.TextSecondary) },
        confirmButton = { TextButton({ confirmReset = false; vm.resetProgress() }) { Text("RESET", style = NexusType.Label, color = NexusColors.Danger) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("CANCEL", style = NexusType.Label, color = NexusColors.ElectricCyan) } },
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().padding(bottom = 12.dp), brackets = false) {
        SectionLabel(title); Spacer(Modifier.height(10.dp)); content()
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = NexusType.BodyStrong, color = Color.White, modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedThumbColor = NexusColors.DeepSpaceNavy, checkedTrackColor = NexusColors.ElectricCyan))
    }
}

@Composable
private fun Status(label: String, ok: Boolean, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = NexusType.BodyStrong, color = Color.White)
            Text(if (ok) "Enabled" else "Not allowed", style = NexusType.Small, color = if (ok) NexusColors.NeonGreen else NexusColors.CosmicGold)
        }
        if (!ok) NexusSecondaryButton(action, onAction)
    }
}
