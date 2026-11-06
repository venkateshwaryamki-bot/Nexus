package com.nexus.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.*

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

enum class ButtonVariant { Primary, Secondary, Ghost, Danger }
enum class ButtonSize(val height: Dp, val hPad: Dp) { Large(56.dp, 24.dp), Medium(48.dp, 18.dp), Small(36.dp, 12.dp) }

/**
 * The one button. Primary = cyan→turquoise fill with a soft glow (one per screen),
 * Secondary = card fill + cyan outline, Ghost = text only, Danger = outlined red.
 * Always ≥ 48dp touch target (Small pads its touch area).
 */
@Composable
fun NexusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    leading: Glyph? = null,
    trailing: Glyph? = null,
) {
    val haptic = LocalHapticFeedback.current
    val prefs = LocalNexusPrefs.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(Motion.fast), label = "press")
    val shape = RoundedCornerShape(Radius.md)
    val (fill, border, fg) = when {
        !enabled -> Triple(Brush.horizontalGradient(listOf(NexusColors.Card, NexusColors.Card)), NexusColors.Divider, NexusColors.TextMuted)
        variant == ButtonVariant.Primary -> Triple(Brush.horizontalGradient(listOf(NexusColors.ElectricCyan, NexusColors.Turquoise)), Color.Transparent, NexusColors.DeepSpaceNavy)
        variant == ButtonVariant.Secondary -> Triple(Brush.verticalGradient(listOf(NexusColors.CardHigh, NexusColors.Card)), NexusColors.ElectricCyan.copy(alpha = 0.55f), NexusColors.ElectricCyan)
        variant == ButtonVariant.Danger -> Triple(Brush.verticalGradient(listOf(NexusColors.Danger.copy(alpha = 0.12f), NexusColors.Danger.copy(alpha = 0.06f))), NexusColors.Danger.copy(alpha = 0.6f), NexusColors.Danger)
        else -> Triple(Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)), Color.Transparent, NexusColors.ElectricCyan)
    }
    Box(
        modifier
            .heightIn(min = maxOf(size.height, 48.dp))
            .scale(scale)
            .drawBehind {
                if (enabled && variant == ButtonVariant.Primary) {
                    // Soft outer glow, drawn once (no blur pass; cheap on mid-range GPUs).
                    val area = this.size
                    drawRoundRect(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = if (pressed) 0.45f else 0.28f), Color.Transparent),
                        center, area.maxDimension * 0.7f), topLeft = Offset(-12f, -10f),
                        size = androidx.compose.ui.geometry.Size(area.width + 24f, area.height + 20f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(Radius.md.toPx() * 1.6f))
                }
            }
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(source, null, enabled = enabled, role = Role.Button) {
                if (prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
            }
            .heightIn(min = size.height)
            .padding(horizontal = size.hPad),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val iconSize = if (size == ButtonSize.Small) 16.dp else 20.dp
            if (leading != null) { NexusIcon(leading, fg, size = iconSize); Spacer(Modifier.width(Space.xs)) }
            if (text.isNotEmpty()) Text(text, style = if (size == ButtonSize.Small) NexusType.Label.copy(letterSpacing = NexusType.Button.letterSpacing) else NexusType.Button,
                color = fg, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailing != null) { Spacer(Modifier.width(Space.xs)); NexusIcon(trailing, fg, size = iconSize) }
        }
    }
}

/** Circular icon button (timer controls, top-bar actions). [highlighted] adds the cyan ring + glow. */
@Composable
fun NexusIconButton(
    glyph: Glyph, onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier,
    size: Dp = 48.dp, highlighted: Boolean = false, tint: Color = if (highlighted) NexusColors.ElectricCyan else NexusColors.TextPrimary,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(Motion.fast), label = "iconPress")
    Box(
        modifier.size(size).scale(scale)
            .drawBehind { if (highlighted) drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = 0.35f), Color.Transparent), center, this.size.minDimension * 0.8f), this.size.minDimension * 0.8f) }
            .clip(CircleShape)
            .background(if (highlighted) NexusColors.CardHigh else NexusColors.Card)
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) NexusColors.ElectricCyan else NexusColors.PanelBorder, CircleShape)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { NexusIcon(glyph, if (enabled) tint else NexusColors.TextMuted, size = size * 0.45f) }
}

// ---------------------------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------------------------

enum class CardTone { Default, Highlight, Success, Premium }

private fun CardTone.accent() = when (this) {
    CardTone.Default -> NexusColors.PanelBorder
    CardTone.Highlight -> NexusColors.ElectricCyan.copy(alpha = 0.7f)
    CardTone.Success -> NexusColors.NeonGreen.copy(alpha = 0.6f)
    CardTone.Premium -> NexusColors.CosmicGold.copy(alpha = 0.6f)
}

/** Standard surface. Solid (no translucency stacking) so it stays cheap and legible over any background. */
@Composable
fun NexusCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Default,
    padding: PaddingValues = PaddingValues(Space.md),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(NexusColors.Card, NexusColors.MidnightBlue)))
            .border(1.dp, Brush.verticalGradient(listOf(tone.accent(), tone.accent().copy(alpha = 0.08f))), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

// ---------------------------------------------------------------------------------------------
// Progress, chips, headers
// ---------------------------------------------------------------------------------------------

/** Labelled progress bar ("Progress Bar … 40%"). */
@Composable
fun LabeledProgress(label: String, progress: Float, modifier: Modifier = Modifier, valueText: String = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    colors: List<Color> = listOf(NexusColors.ElectricCyan, NexusColors.Turquoise)) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = NexusType.Small, color = NexusColors.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(valueText, style = NexusType.NumericSmall, color = colors.first())
        }
        Spacer(Modifier.height(6.dp))
        EnergyBar(progress, Modifier.fillMaxWidth(), colors = colors)
    }
}

/** Currency/resource pill (energy, XP, credits). */
@Composable
fun CurrencyChip(glyph: Glyph, value: String, tint: Color, modifier: Modifier = Modifier, contentDescription: String = value) {
    Row(
        modifier.clip(RoundedCornerShape(Radius.pill)).background(NexusColors.Card).border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(Radius.pill))
            .padding(horizontal = 10.dp, vertical = 6.dp).semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NexusIcon(glyph, tint, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(value, style = NexusType.NumericSmall, color = NexusColors.TextPrimary)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = NexusType.Heading, color = NexusColors.TextPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, style = NexusType.Label, color = NexusColors.ElectricCyan,
            modifier = Modifier.clip(RoundedCornerShape(Radius.sm)).clickable(onClick = onAction).padding(Space.xs))
    }
}

/** Small status pill ("Unlocks at Lv. 5", "Home", "New"). */
@Composable
fun StatusPill(text: String, tint: Color = NexusColors.ElectricCyan, glyph: Glyph? = null, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(Radius.pill)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) { NexusIcon(glyph, tint, size = 11.dp); Spacer(Modifier.width(4.dp)) }
        Text(text, style = NexusType.Caption, color = tint)
    }
}

// ---------------------------------------------------------------------------------------------
// Composite components
// ---------------------------------------------------------------------------------------------

/** Mission card: icon, objective, progress x/y, reward. */
@Composable
fun MissionCard(glyph: Glyph, title: String, objective: String, current: Int, goal: Int, reward: String, modifier: Modifier = Modifier, accent: Color = NexusColors.Purple) {
    val done = current >= goal
    NexusCard(modifier, tone = if (done) CardTone.Success else CardTone.Default, padding = PaddingValues(Space.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                NexusIcon(if (done) Glyph.CHECK else glyph, if (done) NexusColors.NeonGreen else accent, size = 20.dp)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = NexusType.BodyStrong, color = NexusColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(objective, style = NexusType.Small, color = NexusColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(Space.xs))
            Column(horizontalAlignment = Alignment.End) {
                Text("${current.coerceAtMost(goal)}/$goal", style = NexusType.NumericSmall, color = if (done) NexusColors.NeonGreen else NexusColors.TextPrimary)
                Text(reward, style = NexusType.Caption, color = NexusColors.CosmicGold)
            }
        }
        Spacer(Modifier.height(Space.xs))
        EnergyBar(current.toFloat() / goal.coerceAtLeast(1), Modifier.fillMaxWidth(), height = 4.dp,
            colors = if (done) listOf(NexusColors.NeonGreen, NexusColors.NeonGreen) else listOf(accent, NexusColors.ElectricCyan))
    }
}

/** Square quick-action tile used on the home dashboard (Build / Missions / Planets). */
@Composable
fun QuickActionTile(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = NexusColors.ElectricCyan, badge: String? = null) {
    NexusCard(modifier, padding = PaddingValues(vertical = Space.sm, horizontal = Space.xs), onClick = onClick) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.3f), Color.Transparent), center, size.minDimension * 0.6f)) },
                    contentAlignment = Alignment.Center) { NexusIcon(glyph, tint, size = 24.dp) }
                Spacer(Modifier.height(4.dp))
                Text(label, style = NexusType.Caption, color = NexusColors.TextPrimary)
            }
            if (badge != null) StatusPill(badge, NexusColors.CosmicGold, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** Galaxy list row: planet art, name, unlock state. */
@Composable
fun PlanetRow(name: String, subtitle: String, locked: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
              art: DrawScope.() -> Unit) {
    NexusCard(modifier.semantics { this.selected = selected }, tone = if (selected) CardTone.Highlight else CardTone.Default,
        padding = PaddingValues(Space.sm), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(52.dp)) { art() }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(name, style = NexusType.Heading.copy(fontSize = NexusType.BodyStrong.fontSize), color = if (locked) NexusColors.TextSecondary else NexusColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (locked) { NexusIcon(Glyph.LOCK, NexusColors.TextMuted, size = 12.dp); Spacer(Modifier.width(4.dp)) }
                    Text(subtitle, style = NexusType.Small, color = if (locked) NexusColors.TextMuted else NexusColors.ElectricCyan)
                }
            }
            if (!locked) NexusIcon(Glyph.ARROW_RIGHT, NexusColors.ElectricCyan, size = 18.dp)
        }
    }
}

/** Top bar: avatar, name, level/rank line, trailing actions (e.g. notifications). */
@Composable
fun PlayerTopBar(avatarId: Int, name: String, subtitle: String, onProfile: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(Radius.pill)).clickable(role = Role.Button, onClick = onProfile).padding(end = Space.xs),
            verticalAlignment = Alignment.CenterVertically) {
            AvatarEmblem(avatarId, size = 44.dp)
            Spacer(Modifier.width(Space.sm))
            Column {
                Text(name, style = NexusType.Heading, color = NexusColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = NexusType.Small, color = NexusColors.TextSecondary, maxLines = 1)
            }
        }
        trailing()
    }
}

enum class NavDestination(val label: String, val glyph: Glyph) {
    UNIVERSE("Universe", Glyph.UNIVERSE), MISSIONS("Missions", Glyph.MISSIONS), FOCUS("Focus", Glyph.PLAY),
    ARENA("Arena", Glyph.ARENA), PROFILE("Profile", Glyph.PROFILE),
}

/** Bottom navigation with the raised central Focus button (blueprint "App Flow"). */
@Composable
fun NexusBottomBar(selected: NavDestination, onSelect: (NavDestination) -> Unit, modifier: Modifier = Modifier, focusActive: Boolean = false) {
    val prefs = LocalNexusPrefs.current
    val pulse = if (focusActive && !prefs.reduceMotion) rememberInfiniteTransition(label = "fab")
        .animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow").value else 0.35f
    Box(modifier.fillMaxWidth().height(76.dp)) {
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp)
                .clip(RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl, bottomStart = Radius.lg, bottomEnd = Radius.lg))
                .background(Brush.verticalGradient(listOf(NexusColors.Card, NexusColors.MidnightBlue)))
                .border(1.dp, NexusColors.PanelBorder, RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl, bottomStart = Radius.lg, bottomEnd = Radius.lg)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavDestination.entries.forEach { d ->
                if (d == NavDestination.FOCUS) { Spacer(Modifier.weight(1f)); return@forEach }
                val on = d == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onSelect(d) }.semantics { this.selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    NexusIcon(d.glyph, if (on) NexusColors.ElectricCyan else NexusColors.TextSecondary, size = 22.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(d.label, style = NexusType.Caption, color = if (on) NexusColors.ElectricCyan else NexusColors.TextSecondary)
                }
            }
        }
        // Raised centre Focus button
        Box(
            Modifier.align(Alignment.TopCenter).size(62.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = pulse), Color.Transparent), center, size.minDimension * 0.85f), size.minDimension * 0.85f) }
                .clip(CircleShape).background(Brush.verticalGradient(listOf(NexusColors.CardHigh, NexusColors.DeepSpaceNavy)))
                .border(2.dp, NexusColors.ElectricCyan, CircleShape)
                .clickable(role = Role.Button) { onSelect(NavDestination.FOCUS) }
                .semantics { contentDescription = if (focusActive) "Return to focus session" else "Start focus" },
            contentAlignment = Alignment.Center,
        ) { NexusIcon(if (focusActive) Glyph.CLOCK else Glyph.PLAY, NexusColors.ElectricCyan, size = 26.dp) }
    }
}
