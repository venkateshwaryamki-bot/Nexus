package com.nexus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimal line icons drawn in code (original, scalable, no icon-font dependency). */
enum class Glyph { ENERGY, FOCUS, MISSIONS, PLANETS, RANKINGS, PROFILE, SETTINGS, BELL, PLAY, PAUSE, STOP, LOCK, BACK, CHECK, STAR, CLOCK, FLAME,
    UNIVERSE, ARENA, BUILD, BOOK, SOUND, AMBIENT, CRYSTAL, CREDIT, CHEVRON_DOWN, ARROW_RIGHT, TREE }

@Composable
fun NexusIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) { drawGlyph(glyph, tint) }
}

fun DrawScope.drawGlyph(glyph: Glyph, tint: Color) {
    val w = size.width; val s = w / 24f
    val stroke = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(vararg pts: Float, close: Boolean = false) = Path().apply {
        moveTo(pts[0] * s, pts[1] * s); var i = 2
        while (i < pts.size) { lineTo(pts[i] * s, pts[i + 1] * s); i += 2 }
        if (close) close()
    }
    fun c(x: Float, y: Float) = Offset(x * s, y * s)
    when (glyph) {
        Glyph.ENERGY -> drawPath(p(13f, 2f, 5f, 13.5f, 11f, 13.5f, 10f, 22f, 19f, 9.5f, 12.5f, 9.5f, close = true), tint)
        Glyph.FOCUS -> { drawCircle(tint, 9f * s, c(12f, 12f), style = stroke); drawCircle(tint, 4.5f * s, c(12f, 12f), style = stroke); drawCircle(tint, 1.4f * s, c(12f, 12f)) }
        Glyph.MISSIONS -> { drawPath(p(5f, 3f, 5f, 21f), tint, style = stroke); drawPath(p(5f, 4f, 18f, 4f, 15f, 8.5f, 18f, 13f, 5f, 13f), tint, style = stroke) }
        Glyph.PLANETS -> { drawCircle(tint, 6f * s, c(12f, 12f), style = stroke)
            drawOval(tint, c(1.5f, 8.8f), Size(21f * s, 6.4f * s), style = stroke) }
        Glyph.RANKINGS -> { drawPath(p(3f, 21f, 3f, 13f, 9f, 13f, 9f, 21f), tint, style = stroke); drawPath(p(9f, 21f, 9f, 6f, 15f, 6f, 15f, 21f), tint, style = stroke)
            drawPath(p(15f, 21f, 15f, 10f, 21f, 10f, 21f, 21f), tint, style = stroke) }
        Glyph.PROFILE -> { drawCircle(tint, 4f * s, c(12f, 8f), style = stroke); drawPath(p(4f, 21f, 6f, 15.5f, 18f, 15.5f, 20f, 21f), tint, style = stroke) }
        Glyph.SETTINGS -> { drawCircle(tint, 3.2f * s, c(12f, 12f), style = stroke)
            for (k in 0 until 8) { val a = k * Math.PI / 4; val o = c(12f + 7.5f * kotlin.math.cos(a).toFloat(), 12f + 7.5f * kotlin.math.sin(a).toFloat())
                val i = c(12f + 5.2f * kotlin.math.cos(a).toFloat(), 12f + 5.2f * kotlin.math.sin(a).toFloat()); drawLine(tint, i, o, 2.2f * s, StrokeCap.Round) }
            drawCircle(tint, 5.6f * s, c(12f, 12f), style = stroke) }
        Glyph.BELL -> { drawPath(p(6f, 17f, 6f, 10f, 8f, 6f, 12f, 4.5f, 16f, 6f, 18f, 10f, 18f, 17f, 20f, 18f, 4f, 18f, 6f, 17f), tint, style = stroke); drawLine(tint, c(10f, 21f), c(14f, 21f), 1.8f * s, StrokeCap.Round) }
        Glyph.PLAY -> drawPath(p(7f, 4f, 20f, 12f, 7f, 20f, close = true), tint)
        Glyph.PAUSE -> { drawLine(tint, c(8f, 5f), c(8f, 19f), 3.2f * s, StrokeCap.Round); drawLine(tint, c(16f, 5f), c(16f, 19f), 3.2f * s, StrokeCap.Round) }
        Glyph.STOP -> drawRoundRect(tint, c(5.5f, 5.5f), Size(13f * s, 13f * s), androidx.compose.ui.geometry.CornerRadius(2.5f * s))
        Glyph.LOCK -> { drawRoundRect(tint, c(5f, 10.5f), Size(14f * s, 10.5f * s), androidx.compose.ui.geometry.CornerRadius(2f * s), style = stroke)
            drawPath(p(8f, 10.5f, 8f, 7.5f, 10f, 4.5f, 14f, 4.5f, 16f, 7.5f, 16f, 10.5f), tint, style = stroke) }
        Glyph.BACK -> drawPath(p(15f, 4f, 7f, 12f, 15f, 20f), tint, style = stroke)
        Glyph.CHECK -> drawPath(p(4.5f, 12.5f, 9.5f, 17.5f, 19.5f, 6.5f), tint, style = stroke)
        Glyph.STAR -> drawPath(p(12f, 2.5f, 14.6f, 9f, 21.5f, 9.4f, 16.2f, 13.8f, 17.9f, 20.6f, 12f, 16.9f, 6.1f, 20.6f, 7.8f, 13.8f, 2.5f, 9.4f, 9.4f, 9f, close = true), tint)
        Glyph.CLOCK -> { drawCircle(tint, 9f * s, c(12f, 12f), style = stroke); drawPath(p(12f, 7f, 12f, 12f, 15.5f, 14f), tint, style = stroke) }
        Glyph.FLAME -> drawPath(Path().apply {
            moveTo(12f * s, 2.5f * s); cubicTo(17f * s, 8f * s, 19.5f * s, 11f * s, 19f * s, 15f * s)
            cubicTo(18.5f * s, 19.5f * s, 15f * s, 21.5f * s, 12f * s, 21.5f * s); cubicTo(8.5f * s, 21.5f * s, 5f * s, 19f * s, 5f * s, 15f * s)
            cubicTo(5f * s, 12f * s, 7f * s, 10.5f * s, 8.5f * s, 8.5f * s); cubicTo(9f * s, 11f * s, 10f * s, 12f * s, 11f * s, 12.5f * s)
            cubicTo(11f * s, 9f * s, 11f * s, 5.5f * s, 12f * s, 2.5f * s); close() }, tint)
        Glyph.UNIVERSE -> { drawPath(p(3f, 11f, 12f, 3.5f, 21f, 11f), tint, style = stroke); drawPath(p(5.5f, 9.5f, 5.5f, 20.5f, 18.5f, 20.5f, 18.5f, 9.5f), tint, style = stroke)
            drawPath(p(10f, 20.5f, 10f, 14.5f, 14f, 14.5f, 14f, 20.5f), tint, style = stroke) }
        Glyph.ARENA -> { drawPath(p(7f, 4f, 17f, 4f, 17f, 9f, 15.5f, 12.5f, 12f, 14f, 8.5f, 12.5f, 7f, 9f, close = true), tint, style = stroke)
            drawPath(p(7f, 6f, 3.5f, 6f, 3.5f, 8f, 7.2f, 10.5f), tint, style = stroke); drawPath(p(17f, 6f, 20.5f, 6f, 20.5f, 8f, 16.8f, 10.5f), tint, style = stroke)
            drawLine(tint, c(12f, 14f), c(12f, 18f), 1.8f * s, StrokeCap.Round); drawLine(tint, c(8f, 20f), c(16f, 20f), 1.8f * s, StrokeCap.Round) }
        Glyph.BUILD -> { drawPath(p(12f, 3f, 20f, 7.5f, 20f, 16.5f, 12f, 21f, 4f, 16.5f, 4f, 7.5f, close = true), tint, style = stroke)
            drawPath(p(4f, 7.5f, 12f, 12f, 20f, 7.5f), tint, style = stroke); drawLine(tint, c(12f, 12f), c(12f, 21f), 1.8f * s, StrokeCap.Round) }
        Glyph.BOOK -> { drawPath(p(12f, 6f, 8f, 4.5f, 3f, 4.5f, 3f, 18.5f, 8f, 18.5f, 12f, 20f, 16f, 18.5f, 21f, 18.5f, 21f, 4.5f, 16f, 4.5f, 12f, 6f, 12f, 20f), tint, style = stroke) }
        Glyph.SOUND -> { drawPath(p(4f, 9.5f, 8f, 9.5f, 13f, 5f, 13f, 19f, 8f, 14.5f, 4f, 14.5f, close = true), tint, style = stroke)
            drawArc(tint, -45f, 90f, false, c(11f, 8f), androidx.compose.ui.geometry.Size(8f * s, 8f * s), style = stroke)
            drawArc(tint, -45f, 90f, false, c(9f, 5f), androidx.compose.ui.geometry.Size(14f * s, 14f * s), style = stroke) }
        Glyph.AMBIENT -> { drawPath(p(2.5f, 19f, 9f, 8f, 13f, 14f, 15.5f, 11f, 21.5f, 19f, close = true), tint, style = stroke); drawCircle(tint, 1.8f * s, c(17f, 6f)) }
        Glyph.CRYSTAL -> { drawPath(p(12f, 2.5f, 19f, 9f, 12f, 21.5f, 5f, 9f, close = true), tint); drawPath(p(5f, 9f, 19f, 9f), tint.copy(alpha = 0.4f), style = stroke) }
        Glyph.CREDIT -> { drawCircle(tint, 8.5f * s, c(12f, 12f), style = stroke); drawPath(p(12f, 7f, 16f, 12f, 12f, 17f, 8f, 12f, close = true), tint) }
        Glyph.CHEVRON_DOWN -> drawPath(p(6f, 9.5f, 12f, 15.5f, 18f, 9.5f), tint, style = stroke)
        Glyph.ARROW_RIGHT -> { drawLine(tint, c(4.5f, 12f), c(19f, 12f), 1.8f * s, StrokeCap.Round); drawPath(p(13.5f, 6.5f, 19f, 12f, 13.5f, 17.5f), tint, style = stroke) }
        Glyph.TREE -> { drawCircle(tint, 6.5f * s, c(12f, 9f), style = stroke); drawLine(tint, c(12f, 15.5f), c(12f, 21f), 1.8f * s, StrokeCap.Round)
            drawLine(tint, c(8f, 21f), c(16f, 21f), 1.8f * s, StrokeCap.Round) }
    }
}
