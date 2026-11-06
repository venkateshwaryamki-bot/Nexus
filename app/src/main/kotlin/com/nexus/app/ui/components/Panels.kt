package com.nexus.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType

/** Translucent HUD panel with a hairline border and corner brackets (selective glassmorphism). */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = NexusColors.ElectricCyan,
    corner: Dp = 18.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    brackets: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(NexusColors.PanelTop, NexusColors.PanelBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.08f))), shape)
            .drawBehind {
                if (!brackets) return@drawBehind
                val l = 14.dp.toPx(); val i = 6.dp.toPx(); val sw = 1.6.dp.toPx(); val c = accent.copy(alpha = 0.85f)
                drawLine(c, Offset(i, i), Offset(i + l, i), sw, StrokeCap.Round); drawLine(c, Offset(i, i), Offset(i, i + l), sw, StrokeCap.Round)
                val r = size.width - i; val b = size.height - i
                drawLine(c, Offset(r, b), Offset(r - l, b), sw, StrokeCap.Round); drawLine(c, Offset(r, b), Offset(r, b - l), sw, StrokeCap.Round)
            }
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = NexusColors.ElectricCyan) =
    Text(text.uppercase(), style = NexusType.Label, color = color, modifier = modifier)

/** Primary call to action: energised gradient with a moving sheen and press feedback. */
@Composable
fun NexusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: Glyph? = null,
    colors: List<Color> = listOf(NexusColors.ElectricCyan, Color(0xFF35EBC0), NexusColors.NeonGreen),
) {
    val haptic = LocalHapticFeedback.current
    val prefs = LocalNexusPrefs.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val sheen = if (prefs.reduceMotion) 0.3f else rememberInfiniteTransition(label = "sheen")
        .animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sheenX").value
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .scale(scale)
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (enabled) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(Color(0xFF22384D), Color(0xFF1A2D40))))
            .drawWithContent {
                drawContent()
                if (enabled) {
                    val x = size.width * sheen
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(x - 120f, 0f), end = Offset(x + 120f, size.height)))
                }
            }
            .clickable(source, null, enabled = enabled, role = Role.Button) {
                if (prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val fg = if (enabled) NexusColors.DeepSpaceNavy else NexusColors.TextMuted
            if (leading != null) { NexusIcon(leading, fg, size = 20.dp); Spacer(Modifier.width(10.dp)) }
            Text(text.uppercase(), style = NexusType.Heading, color = fg, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun NexusSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                         accent: Color = NexusColors.ElectricCyan, leading: Glyph? = null) {
    val shape = RoundedCornerShape(14.dp)
    val c = if (enabled) accent else NexusColors.TextMuted
    Row(
        modifier.heightIn(min = 50.dp).clip(shape).background(accent.copy(alpha = if (enabled) 0.08f else 0.03f))
            .border(1.dp, c.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) { NexusIcon(leading, c, size = 18.dp); Spacer(Modifier.width(8.dp)) }
        Text(text.uppercase(), style = NexusType.Label, color = c)
    }
}

/** Thin glowing progress bar. */
@Composable
fun EnergyBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp,
              colors: List<Color> = listOf(NexusColors.ElectricCyan, NexusColors.NeonGreen)) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(height)).background(Color(0x3327DFFF))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(height)).background(Brush.horizontalGradient(colors)))
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = NexusColors.ElectricCyan, glyph: Glyph? = null) {
    GlassPanel(modifier, accent = accent, padding = PaddingValues(14.dp), brackets = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) { NexusIcon(glyph, accent, size = 16.dp); Spacer(Modifier.width(6.dp)) }
            Text(label.uppercase(), style = NexusType.Label, color = NexusColors.TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = NexusType.Numeric, color = NexusColors.TextPrimary)
    }
}
