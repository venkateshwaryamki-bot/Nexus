package com.nexus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.*

/** Living reference for the design system (Settings → Design system). Every token and component, in one place. */
@Composable
fun DesignSystemScreen(onBack: () -> Unit) {
    var nav by remember { mutableStateOf(NavDestination.UNIVERSE) }
    Box(Modifier.fillMaxSize().background(NexusColors.DeepSpaceNavy)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            ScreenHeader("Design system", "Tokens & components", onBack)

            SectionHeader("Colour")
            val swatches = listOf(
                "Background" to NexusColors.DeepSpaceNavy, "Surface" to NexusColors.MidnightBlue, "Card" to NexusColors.Card, "Primary" to NexusColors.ElectricCyan,
                "Turquoise" to NexusColors.Turquoise, "Success" to NexusColors.NeonGreen, "Secondary" to NexusColors.Purple, "Premium" to NexusColors.CosmicGold,
                "Danger" to NexusColors.Danger, "Text" to NexusColors.TextPrimary, "Text 2" to NexusColors.TextSecondary, "Muted" to NexusColors.TextMuted,
            )
            swatches.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    row.forEach { (name, c) ->
                        Column(Modifier.weight(1f)) {
                            Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(Radius.sm)).background(c).border(1.dp, NexusColors.Divider, RoundedCornerShape(Radius.sm)))
                            Text(name, style = NexusType.Caption, color = NexusColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            SectionHeader("Typography")
            NexusCard(Modifier.fillMaxWidth()) {
                Text("Title · Space Grotesk", style = NexusType.Title, color = NexusColors.TextPrimary)
                Text("Heading · Space Grotesk", style = NexusType.Heading, color = NexusColors.TextPrimary)
                Text("Body · Inter. Stay focused, earn energy and build your own universe.", style = NexusType.Body, color = NexusColors.TextSecondary)
                Text("LABEL · INTER SEMIBOLD", style = NexusType.Label, color = NexusColors.ElectricCyan)
                Text("25:00", style = NexusType.Timer, color = NexusColors.TextPrimary)
                Text("Tabular numbers: 1,280 · 0/60 · 40%", style = NexusType.NumericSmall, color = NexusColors.TextPrimary)
            }

            SectionHeader("Buttons")
            NexusButton("Start Focus", {}, Modifier.fillMaxWidth(), leading = Glyph.PLAY)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                NexusButton("Build", {}, Modifier.weight(1f), ButtonVariant.Secondary, ButtonSize.Medium, leading = Glyph.BUILD)
                NexusButton("End", {}, Modifier.weight(1f), ButtonVariant.Danger, ButtonSize.Medium, leading = Glyph.STOP)
                NexusButton("Skip", {}, Modifier.weight(1f), ButtonVariant.Ghost, ButtonSize.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                NexusIconButton(Glyph.AMBIENT, {}, "Ambient")
                NexusIconButton(Glyph.PLAY, {}, "Play", size = 64.dp, highlighted = true)
                NexusIconButton(Glyph.SOUND, {}, "Sound")
                NexusButton("Next", {}, size = ButtonSize.Small, trailing = Glyph.ARROW_RIGHT)
                NexusButton("Locked", {}, size = ButtonSize.Small, enabled = false)
            }

            SectionHeader("Chips & progress")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                CurrencyChip(Glyph.ENERGY, "2,450", NexusColors.NeonGreen)
                CurrencyChip(Glyph.STAR, "1,280 XP", NexusColors.CosmicGold)
                CurrencyChip(Glyph.CREDIT, "320", NexusColors.Purple)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                StatusPill("Home"); StatusPill("Unlocks at Lv. 5", NexusColors.TextSecondary, Glyph.LOCK); StatusPill("New", NexusColors.CosmicGold)
            }
            LabeledProgress("Nexus Prime development", 0.4f)

            SectionHeader("Cards")
            MissionCard(Glyph.FOCUS, "Deep Worker", "Focus for 60 minutes", 0, 60, "+40 XP")
            MissionCard(Glyph.CHECK, "First Contact", "Complete one focus session", 1, 1, "+20 XP")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                QuickActionTile(Glyph.BUILD, "Build", {}, Modifier.weight(1f))
                QuickActionTile(Glyph.MISSIONS, "Missions", {}, Modifier.weight(1f), NexusColors.Purple, badge = "2")
                QuickActionTile(Glyph.PLANETS, "Planets", {}, Modifier.weight(1f), NexusColors.CosmicGold)
            }
            PlanetRow("NEXUS PRIME", "Lv. 3 · Home", locked = false, selected = true, onClick = {}) {
                drawPlanetOrb(PlanetIdentity.NEXUS_PRIME, center, size.minDimension * 0.4f, false)
            }
            PlanetRow("ORION", "Unlocks at Lv. 5", locked = true, selected = false, onClick = {}) {
                drawPlanetOrb(PlanetIdentity.ORION, center, size.minDimension * 0.4f, true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                StatTile("Focus hours", "12.5", Modifier.weight(1f), glyph = Glyph.CLOCK)
                StatTile("Streak", "7 d", Modifier.weight(1f), NexusColors.CosmicGold, Glyph.FLAME)
            }

            SectionHeader("Navigation")
            PlayerTopBar(2, "Nova", "Lv. 12 · Explorer", onProfile = {}) { NexusIconButton(Glyph.BELL, {}, "Notifications", size = 44.dp) }
            NexusBottomBar(nav, { nav = it })
            Spacer(Modifier.height(Space.xl))
        }
    }
}
