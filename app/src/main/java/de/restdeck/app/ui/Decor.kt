package de.restdeck.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.restdeck.app.domain.BgDecor
import de.restdeck.app.domain.StyleSpec
import de.restdeck.app.domain.TileDecor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Statische Hintergrund-Verzierung je Stil (bewusst ohne Dauer-Animation: Akku). */
fun Modifier.styleDecor(style: StyleSpec): Modifier = drawBehind {
    val deco = style.decorColor.toColor()
    when (style.bgDecor) {
        BgDecor.AURORA -> {
            fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent), Offset(x, y), r),
                radius = r, center = Offset(x, y),
            )
            blob(deco, size.width * 0.1f, size.height * 0.05f, size.width * 0.8f)
            blob(Color(0xFF22D3EE), size.width * 1.0f, size.height * 0.35f, size.width * 0.7f)
            blob(Color(0xFFEC4899), size.width * 0.2f, size.height * 1.0f, size.width * 0.8f)
        }
        BgDecor.GEARS -> {
            // Warmes Licht oben, Zahnräder als Prägung, dunkle Vignette
            drawCircle(
                Brush.radialGradient(listOf(deco.copy(alpha = 0.22f), Color.Transparent), Offset(size.width * 0.5f, 0f), size.width),
                radius = size.width, center = Offset(size.width * 0.5f, 0f),
            )
            val w = size.width
            gear(Offset(w * 0.92f, size.height * 0.22f), w * 0.30f, 14, deco.copy(alpha = 0.10f))
            gear(Offset(w * 0.55f, size.height * 0.30f), w * 0.16f, 10, deco.copy(alpha = 0.08f))
            gear(Offset(w * 0.05f, size.height * 0.82f), w * 0.38f, 18, deco.copy(alpha = 0.09f))
            gear(Offset(w * 0.48f, size.height * 0.93f), w * 0.14f, 9, deco.copy(alpha = 0.07f))
            vignette(0.65f)
        }
        BgDecor.SCANLINES -> {
            scanlines(deco.copy(alpha = 0.05f), 3.dp.toPx())
            drawCircle(
                Brush.radialGradient(listOf(deco.copy(alpha = 0.10f), Color.Transparent), center, size.maxDimension * 0.7f),
                radius = size.maxDimension * 0.7f, center = center,
            )
            vignette(0.7f)
        }
        BgDecor.NONE -> Unit
    }
}

/** Verzierung über einer Kachel (wird nach dem Inhalt-Hintergrund gezeichnet). */
fun Modifier.tileDecor(style: StyleSpec, on: Color): Modifier = drawWithContent {
    when (style.tileDecor) {
        TileDecor.GLOSS -> drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), 0f, size.height * 0.55f))
        TileDecor.RIVETS -> {
            // Innerer Zierrahmen + vier Nieten
            val inset = 7.dp.toPx()
            val cut = 10.dp.toPx()
            val p = Path().apply {
                moveTo(inset + cut, inset); lineTo(size.width - inset - cut, inset); lineTo(size.width - inset, inset + cut)
                lineTo(size.width - inset, size.height - inset - cut); lineTo(size.width - inset - cut, size.height - inset)
                lineTo(inset + cut, size.height - inset); lineTo(inset, size.height - inset - cut); lineTo(inset, inset + cut); close()
            }
            drawPath(p, on.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
            val r = 3.5.dp.toPx(); val d = 15.dp.toPx()
            listOf(Offset(d, d), Offset(size.width - d, d), Offset(d, size.height - d), Offset(size.width - d, size.height - d)).forEach { c ->
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1C1), Color(0xFF8B6914)), c - Offset(r / 3, r / 3), r * 1.6f), r, c)
                drawCircle(Color.Black.copy(alpha = 0.35f), r, c, style = Stroke(0.8.dp.toPx()))
            }
        }
        TileDecor.SCANLINES -> {
            scanlines(Color.Black.copy(alpha = 0.18f), 3.dp.toPx())
            drawRect(on.copy(alpha = 0.08f))
        }
        TileDecor.NONE -> Unit
    }
    drawContent()
}

private fun DrawScope.vignette(strength: Float) = drawRect(
    Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = strength)), center, size.maxDimension * 0.75f),
)

private fun DrawScope.scanlines(c: Color, step: Float) {
    var y = 0f
    while (y < size.height) { drawRect(c, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, step / 2)); y += step }
}

/** Zahnrad-Umriss mit Nabe; [r] = Außenradius. */
private fun DrawScope.gear(c: Offset, r: Float, teeth: Int, color: Color) {
    val inner = r * 0.84f
    val p = Path()
    val steps = teeth * 4
    for (i in 0..steps) {
        val a = 2 * PI * i / steps
        val rr = if (i % 4 == 1 || i % 4 == 2) r else inner
        val x = c.x + (rr * cos(a)).toFloat(); val y = c.y + (rr * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    val w = 2.dp.toPx()
    drawPath(p, color, style = Stroke(w))
    drawCircle(color, inner * 0.62f, c, style = Stroke(w))
    drawCircle(color, inner * 0.18f, c, style = Stroke(w))
    for (k in 0 until 6) {
        val a = 2 * PI * k / 6
        drawLine(color, Offset(c.x + (inner * 0.18f * cos(a)).toFloat(), c.y + (inner * 0.18f * sin(a)).toFloat()),
            Offset(c.x + (inner * 0.62f * cos(a)).toFloat(), c.y + (inner * 0.62f * sin(a)).toFloat()), w)
    }
}
