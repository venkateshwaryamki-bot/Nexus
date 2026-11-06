package com.nexus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexus.app.R

/** NEXUS design tokens. The single source of truth for colour in the UI (see ART_DIRECTION.md). */
object NexusColors {
    val DeepSpaceNavy = Color(0xFF061321)
    val MidnightBlue = Color(0xFF0B2035)
    val ElectricCyan = Color(0xFF27DFFF)
    val NeonGreen = Color(0xFF43F58A)
    val CosmicGold = Color(0xFFF5C85B)
    val Purple = Color(0xFF9275FF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA9BCCF)
    val TextMuted = Color(0xFF6B8399)
    val PanelTop = Color(0xCC10283F)
    val PanelBottom = Color(0xB3071828)
    val PanelBorder = Color(0x4027DFFF)
    val Danger = Color(0xFFFF6B7A)
}

val RajdhaniFamily = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

object NexusType {
    val Hero = TextStyle(fontFamily = RajdhaniFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 8.sp)
    val Title = TextStyle(fontFamily = RajdhaniFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 2.sp)
    val Heading = TextStyle(fontFamily = RajdhaniFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = 1.5.sp)
    val Label = TextStyle(fontFamily = RajdhaniFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 1.6.sp)
    val Body = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val BodyStrong = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp)
    val Small = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp)
    val Numeric = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val Timer = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Medium, fontSize = 64.sp, letterSpacing = 1.sp)
}

/** User accessibility preferences that components read. */
data class NexusPrefs(val reduceMotion: Boolean = false, val haptics: Boolean = true)
val LocalNexusPrefs = staticCompositionLocalOf { NexusPrefs() }

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NexusColors.ElectricCyan, onPrimary = NexusColors.DeepSpaceNavy,
            secondary = NexusColors.NeonGreen, tertiary = NexusColors.CosmicGold,
            background = NexusColors.DeepSpaceNavy, surface = NexusColors.MidnightBlue,
            onBackground = NexusColors.TextPrimary, onSurface = NexusColors.TextPrimary,
            surfaceVariant = NexusColors.MidnightBlue, onSurfaceVariant = NexusColors.TextSecondary,
            error = NexusColors.Danger,
        ),
        content = content,
    )
}
