package com.nexus.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType
import kotlinx.coroutines.delay

private data class Line(val speaker: Character, val text: String, val showPlanets: Boolean = false)

private val script = listOf(
    Line(Character.AIRA, "Welcome, Explorer. Your universe is waiting to be rebuilt."),
    Line(Character.AIRA, "I am AIRA, your guide. The galaxy's civilizations have lost their energy, and the Nexus Core has chosen you to restore them."),
    Line(Character.VYON, "I'm Vyon. Five worlds went dark: Nexus Prime, Orion, Aether, Titan and Eclipse.", showPlanets = true),
    Line(Character.VYON, "We start at Nexus Prime, our headquarters. Right now it's an abandoned outpost. We change that together."),
    Line(Character.AIRA, "Here is the rule of this universe: real-world focus generates energy. Study, read, code, create, and the Nexus Core powers up."),
    Line(Character.ORION, "ORION online. I track your sessions and energy. One focus minute = one energy unit. Complete the session to keep it all."),
    Line(Character.AIRA, "Complete your first focus session and watch your civilization respond. Ready, Explorer?"),
)

/** Screen 03 — Character introduction (conversation-style onboarding). Tap to advance. */
@Composable
fun CharacterIntroScreen(onDone: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val line = script[index]
    val reduce = LocalNexusPrefs.current.reduceMotion
    var shown by remember(index) { mutableIntStateOf(if (reduce) line.text.length else 0) }
    LaunchedEffect(index) { while (shown < line.text.length) { delay(18); shown++ } }
    val advance = {
        if (shown < line.text.length) shown = line.text.length
        else if (index < script.lastIndex) index++ else onDone()
    }
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { advance() }) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("SKIP", style = NexusType.Label, color = NexusColors.TextSecondary, modifier = Modifier.clickable { onDone() }.padding(10.dp))
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(line.speaker, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "speaker") { sp ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterPortrait(sp, size = 230.dp)
                    Text(sp.displayName, style = NexusType.Title, color = sp.accent)
                    Text(sp.role.uppercase(), style = NexusType.Label, color = NexusColors.TextSecondary)
                }
            }
            if (line.showPlanets) {
                Spacer(Modifier.height(16.dp))
                Canvas(Modifier.fillMaxWidth().height(70.dp)) {
                    val n = PlanetIdentity.entries.size
                    PlanetIdentity.entries.forEachIndexed { i, p ->
                        drawPlanetOrb(p, Offset(size.width * (i + 0.5f) / n, size.height / 2), size.height * (if (i == 0) 0.36f else 0.26f), locked = i != 0)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            GlassPanel(Modifier.fillMaxWidth().heightIn(min = 150.dp), accent = line.speaker.accent) {
                SectionLabel(line.speaker.displayName, color = line.speaker.accent)
                Spacer(Modifier.height(8.dp))
                Text(line.text.take(shown), style = NexusType.Body.copy(fontSize = NexusType.Body.fontSize * 1.08f), color = NexusColors.TextPrimary)
                Spacer(Modifier.weight(1f, fill = false))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1} / ${script.size}", style = NexusType.Small, color = NexusColors.TextMuted)
                    Text(if (index == script.lastIndex) "TAP TO BEGIN ›" else "TAP TO CONTINUE ›", style = NexusType.Label, color = line.speaker.accent)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
