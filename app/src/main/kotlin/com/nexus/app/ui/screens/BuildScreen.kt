package com.nexus.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusViewModel
import com.nexus.app.ui.art.IslandLayout
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.*
import com.nexus.domain.civilization.BuildCheck
import com.nexus.domain.civilization.BuildingType
import com.nexus.domain.civilization.Construction
import com.nexus.domain.repository.BuildResult
import kotlinx.coroutines.delay

/** Screen 07 — Civilization development: construct and upgrade structures; every level changes the island. */
@Composable
fun BuildScreen(vm: NexusViewModel, onBack: () -> Unit) {
    val p = vm.player.collectAsStateWithLifecycle().value ?: return
    val levels by vm.buildingLevels.collectAsStateWithLifecycle()
    val lastBuild by vm.lastBuild.collectAsStateWithLifecycle()
    val grove by vm.grove.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(BuildingType.REACTOR) }
    val stage = p.civilization.stage.ordinal
    var justBuilt by remember { mutableStateOf<Pair<BuildingType, Int>?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lastBuild) {
        when (val r = lastBuild) {
            is BuildResult.Built -> { justBuilt = r.type to r.level; banner = "${r.type.title} → ${r.type.levelNames[r.level - 1]} · +${r.xp} XP"; vm.consumeBuild(); delay(2600); banner = null }
            is BuildResult.Rejected -> { banner = "Not enough resources yet"; vm.consumeBuild(); delay(2000); banner = null }
            else -> {}
        }
    }
    val selLevel = levels[selected] ?: 0
    val ghost = if (selLevel < BuildingType.MAX_LEVEL && stage >= selected.unlockStage) selected to selLevel + 1 else null

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.padding(horizontal = Space.gutter).padding(top = Space.xs)) {
                ScreenHeader("Build", "Nexus Prime · ${p.civilization.stage.title}", onBack)
                Spacer(Modifier.height(Space.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    CurrencyChip(Glyph.ENERGY, "%,d".format(p.energyBalance), NexusColors.NeonGreen, contentDescription = "${p.energyBalance} energy available")
                    CurrencyChip(Glyph.CREDIT, "%,d".format(p.credits), NexusColors.Purple, contentDescription = "${p.credits} credits available")
                }
            }
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                NexusIsland(stage, levels, p.civilization.stage.title, Modifier.fillMaxSize(), justBuilt = justBuilt, ghost = ghost, grove = grove)
                androidx.compose.animation.AnimatedVisibility(banner != null, Modifier.align(Alignment.BottomCenter), enter = fadeIn() + slideInVertically { it }, exit = fadeOut()) {
                    StatusPill(banner ?: "", NexusColors.CosmicGold, Glyph.BUILD)
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = Space.gutter, vertical = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                items(BuildingType.entries, key = { it.name }) { t ->
                    BuildingCard(t, levels[t] ?: 0, stage, p.energyBalance, p.credits, selected == t, onSelect = { selected = t },
                        onBuild = { selected = t; vm.build(t, levels[t] ?: 0) })
                }
                item { GroveCard(grove) }
                item {
                    NexusCard(Modifier.fillMaxWidth()) {
                        Text("Nexus Tower", style = NexusType.BodyStrong, color = NexusColors.TextPrimary)
                        Text("The headquarters grows by itself as you channel energy through focus: new energy rings and height at every civilization stage.",
                            style = NexusType.Small, color = NexusColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingCard(t: BuildingType, level: Int, stage: Int, energy: Long, credits: Long, selected: Boolean, onSelect: () -> Unit, onBuild: () -> Unit) {
    val check = Construction.check(t, level, stage, energy, credits)
    val previewLevel = (level + 1).coerceAtMost(BuildingType.MAX_LEVEL)
    NexusCard(Modifier.fillMaxWidth(), tone = if (selected) CardTone.Highlight else CardTone.Default, onClick = onSelect, padding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(Radius.md)).background(NexusColors.DeepSpaceNavy), contentAlignment = Alignment.Center) {
                IslandLayout.layers[t.layerKey(previewLevel)]?.let {
                    Image(painterResource(it.res), null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit,
                        alpha = if (check is BuildCheck.Locked) 0.3f else 1f)
                }
                if (check is BuildCheck.Locked) NexusIcon(Glyph.LOCK, NexusColors.TextSecondary, size = 18.dp)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title, style = NexusType.BodyStrong, color = NexusColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    LevelPips(level)
                }
                Text(if (level > 0) "Lv. $level · ${t.levelNames[level - 1]}" else t.description, style = NexusType.Small, color = NexusColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                when (check) {
                    is BuildCheck.Ok, is BuildCheck.NotEnough -> {
                        val cost = if (check is BuildCheck.Ok) check.cost else (check as BuildCheck.NotEnough).cost
                        val missing = check as? BuildCheck.NotEnough
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CurrencyChip(Glyph.ENERGY, "${cost.energy}", if ((missing?.missingEnergy ?: 0) > 0) NexusColors.Danger else NexusColors.NeonGreen)
                            if (cost.credits > 0) CurrencyChip(Glyph.CREDIT, "${cost.credits}", if ((missing?.missingCredits ?: 0) > 0) NexusColors.Danger else NexusColors.Purple)
                            Spacer(Modifier.weight(1f))
                            NexusButton(if (level == 0) "Build" else "Upgrade", onBuild, variant = if (check is BuildCheck.Ok) ButtonVariant.Primary else ButtonVariant.Secondary,
                                size = ButtonSize.Small, enabled = check is BuildCheck.Ok)
                        }
                        if (missing != null) Text(buildString {
                            append("Need ")
                            if (missing.missingEnergy > 0) append("${missing.missingEnergy} more energy")
                            if (missing.missingEnergy > 0 && missing.missingCredits > 0) append(" and ")
                            if (missing.missingCredits > 0) append("${missing.missingCredits} more credits")
                            append(" · next: ${t.levelNames[previewLevel - 1]}")
                        }, style = NexusType.Caption, color = NexusColors.TextMuted, modifier = Modifier.padding(top = 4.dp))
                        else Text("Next: ${t.levelNames[previewLevel - 1]} · +${Construction.xpFor(previewLevel)} XP", style = NexusType.Caption, color = NexusColors.ElectricCyan, modifier = Modifier.padding(top = 4.dp))
                    }
                    is BuildCheck.Locked -> StatusPill("Unlocks at ${check.requiredStage.title}", NexusColors.TextSecondary, Glyph.LOCK)
                    BuildCheck.MaxLevel -> StatusPill("Max level", NexusColors.NeonGreen, Glyph.CHECK)
                }
            }
        }
    }
}

@Composable
private fun LevelPips(level: Int) {
    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "Level $level of ${BuildingType.MAX_LEVEL}" }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(BuildingType.MAX_LEVEL) { i ->
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < level) NexusColors.CosmicGold else NexusColors.Divider))
        }
    }
}

/** The grove: grows only from completed focus. Shows the five stages and what the youngest tree needs. */
@Composable
private fun GroveCard(grove: List<com.nexus.domain.civilization.GroveTree>) {
    val growing = grove.lastOrNull { it.next != null } ?: grove.last()
    NexusCard(Modifier.fillMaxWidth(), tone = CardTone.Success) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexusIcon(Glyph.TREE, NexusColors.NeonGreen, size = 20.dp); Spacer(Modifier.width(6.dp))
            Text("The Grove", style = NexusType.BodyStrong, color = NexusColors.TextPrimary, modifier = Modifier.weight(1f))
            StatusPill("${grove.size} / ${com.nexus.domain.civilization.Grove.MAX_TREES} trees", NexusColors.NeonGreen)
        }
        Text("Trees grow only from completed focus sessions. A new seed is planted every ${com.nexus.domain.civilization.Grove.PLANT_EVERY / 60.0} hours of focus.".replace(".0 ", " "),
            style = NexusType.Small, color = NexusColors.TextSecondary)
        Spacer(Modifier.height(Space.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            com.nexus.domain.civilization.TreeStage.entries.forEachIndexed { i, st ->
                val reached = growing.stage.ordinal >= i
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    IslandLayout.trees.getOrNull(i)?.let { Image(painterResource(it.res), null, Modifier.height((26 + i * 7).dp), contentScale = ContentScale.Fit, alpha = if (reached) 1f else 0.3f) }
                    Text(st.title.substringBefore(" Tree"), style = NexusType.Caption, color = if (growing.stage.ordinal == i) NexusColors.NeonGreen else NexusColors.TextMuted)
                }
            }
        }
        Spacer(Modifier.height(Space.xs))
        val next = growing.next
        LabeledProgress(if (next != null) "Tree ${growing.index + 1}: ${growing.minutesToNext} more focus minutes to ${next.title}" else "Every tree is ancient",
            growing.progressToNext, colors = listOf(NexusColors.NeonGreen, NexusColors.Turquoise))
    }
}
