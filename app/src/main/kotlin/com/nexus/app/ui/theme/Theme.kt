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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.app.R

/**
 * NEXUS design tokens — the single source of truth for the UI (ART_DIRECTION.md, UI/UX blueprint v2).
 * Screens never use raw colours, sizes or durations; they use these.
 */
object NexusColors {
    // Surfaces (blueprint: Background / Surface / Card)
    val DeepSpaceNavy = Color(0xFF06111E)   // Background
    val MidnightBlue = Color(0xFF0A1B2B)    // Surface
    val Card = Color(0xFF102638)            // Card
    val CardHigh = Color(0xFF16324A)        // Raised / pressed card
    // Accents
    val ElectricCyan = Color(0xFF20D6EA)    // Primary
    val Turquoise = Color(0xFF2FE3C8)       // Primary gradient end
    val NeonGreen = Color(0xFF37E6A0)       // Success / energy gained
    val CosmicGold = Color(0xFFF5C85B)      // Premium / XP / level-up
    val Purple = Color(0xFF9275FF)          // Secondary / NEXA / crystals
    val Danger = Color(0xFFF06A7A)
    // Text
    val TextPrimary = Color(0xFFEAF4FF)
    val TextSecondary = Color(0xFF92A6B8)
    val TextMuted = Color(0xFF5E7488)
    // Panel treatments (kept for existing components)
    val PanelTop = Color(0xE6122B42)
    val PanelBottom = Color(0xD90A1B2B)
    val PanelBorder = Color(0x3D20D6EA)
    val Divider = Color(0x1F92A6B8)
}

/** 4-pt spacing scale. */
object Space {
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp; val lg = 20.dp; val xl = 24.dp; val xxl = 32.dp
    /** Horizontal screen gutter. */
    val gutter = 16.dp
}

object Radius {
    val sm = 10.dp; val md = 14.dp; val lg = 18.dp; val xl = 24.dp; val pill = 100.dp
}

/** Motion durations (ms). Long ambient loops are separate and respect reduce-motion. */
object Motion {
    const val fast = 150; const val standard = 300; const val emphasized = 500; const val reward = 1400
}

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Type scale: Space Grotesk for headings/titles, Inter for body/labels, tabular figures for numbers. */
object NexusType {
    private const val TNUM = "tnum"
    val Hero = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 6.sp)
    val Title = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.5.sp)
    val Heading = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.3.sp)
    val Label = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.1.sp)
    val Button = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.4.sp)
    val Body = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val BodyStrong = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
    val Small = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp)
    val Caption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    val Numeric = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFeatureSettings = TNUM)
    val NumericSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFeatureSettings = TNUM)
    val Timer = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Medium, fontSize = 68.sp, letterSpacing = 0.sp, fontFeatureSettings = TNUM)
}

/** User accessibility preferences that components read. */
data class NexusPrefs(val reduceMotion: Boolean = false, val haptics: Boolean = true)
val LocalNexusPrefs = staticCompositionLocalOf { NexusPrefs() }

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NexusColors.ElectricCyan, onPrimary = NexusColors.DeepSpaceNavy,
            secondary = NexusColors.Purple, tertiary = NexusColors.CosmicGold,
            background = NexusColors.DeepSpaceNavy, surface = NexusColors.MidnightBlue,
            onBackground = NexusColors.TextPrimary, onSurface = NexusColors.TextPrimary,
            surfaceVariant = NexusColors.Card, onSurfaceVariant = NexusColors.TextSecondary,
            error = NexusColors.Danger,
        ),
        content = content,
    )
}
