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

/** Back-compat wrappers: all buttons route through [NexusButton]. */
@Composable
fun NexusPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                       leading: Glyph? = null, trailing: Glyph? = null) =
    NexusButton(text, onClick, modifier, ButtonVariant.Primary, enabled = enabled, leading = leading, trailing = trailing)

@Composable
fun NexusSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                         accent: Color = NexusColors.ElectricCyan, leading: Glyph? = null) =
    NexusButton(text, onClick, modifier, if (accent == NexusColors.Danger) ButtonVariant.Danger else ButtonVariant.Secondary,
        size = ButtonSize.Medium, enabled = enabled, leading = leading)

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
    NexusCard(modifier, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) { NexusIcon(glyph, accent, size = 16.dp); Spacer(Modifier.width(6.dp)) }
            Text(label.uppercase(), style = NexusType.Label, color = NexusColors.TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = NexusType.Numeric, color = NexusColors.TextPrimary)
    }
}
