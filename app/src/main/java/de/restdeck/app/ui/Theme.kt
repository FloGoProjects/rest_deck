package de.restdeck.app.ui

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.domain.Contrast
import de.restdeck.app.domain.FontKind
import de.restdeck.app.domain.HttpMethod
import de.restdeck.app.domain.StyleSpec
import de.restdeck.app.domain.Styles
import de.restdeck.app.domain.Swatch

/** Aktiver Stil (R11) und Foto-Hintergrund (R12/R13). */
val LocalStyle = staticCompositionLocalOf { Styles.modern }

data class Backdrop(val photo: ImageBitmap? = null, val dim: Float = 0.6f, val blur: Float = 0f)
val LocalBackdrop = staticCompositionLocalOf { Backdrop() }

fun Long.toColor(): Color = Color(toInt())

/** Design-Tokens des aktiven Stils. Nur in Composables lesbar. */
object Ink {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.bg.toColor()
    val Card: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.card.toColor()
    val CardStrong: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.stroke.toColor()
    val Stroke: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.stroke.toColor()
    val Text: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.text.toColor()
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.muted.toColor()
    val Ok: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.ok.toColor()
    val Bad: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.bad.toColor()
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.accent.toColor()
    val Accent2: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.accent2.toColor()
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalStyle.current.onAccent.toColor()
    /** Hinterlegung für Code/Antworten: dunkler Kasten auf dunklen, zarter auf hellen Stilen. */
    val CodeBg: Color @Composable @ReadOnlyComposable get() = Color.Black.copy(alpha = if (LocalStyle.current.dark) 0.35f else 0.05f)
    val StrokeWidth @Composable @ReadOnlyComposable get() = LocalStyle.current.strokeDp.dp
}

/** Farbfelder für Buttons; Index wird in ActionButton.color gespeichert. */
object Palette {
    fun swatch(style: StyleSpec, i: Int): Swatch = style.swatches[Math.floorMod(i, style.swatches.size)]
    fun gradient(style: StyleSpec, i: Int): Pair<Color, Color> = swatch(style, i).let { it.from.toColor() to it.to.toColor() }
    fun brush(style: StyleSpec, i: Int): Brush = gradient(style, i).let { (a, b) -> Brush.linearGradient(listOf(a, b)) }

    val size: Int @Composable @ReadOnlyComposable get() = LocalStyle.current.swatches.size
    @Composable @ReadOnlyComposable fun brush(i: Int): Brush = brush(LocalStyle.current, i)
    @Composable @ReadOnlyComposable fun on(i: Int): Color = swatch(LocalStyle.current, i).on.toColor()
}

@Composable @ReadOnlyComposable
fun HttpMethod.color(): Color = LocalStyle.current.methodColors[ordinal].toColor()

fun StyleSpec.shape(corner: Int): Shape = if (cutCorners) CutCornerShape(corner.dp) else RoundedCornerShape(corner.dp)

val CardShape: Shape @Composable @ReadOnlyComposable get() = LocalStyle.current.let { it.shape(it.cornerDp) }
val FieldShape: Shape @Composable @ReadOnlyComposable get() = LocalStyle.current.let { it.shape((it.cornerDp * 2 / 3).coerceAtLeast(2)) }
val TileShape: Shape @Composable @ReadOnlyComposable get() = LocalStyle.current.let { it.shape(it.tileCornerDp) }
val PillShape: Shape @Composable @ReadOnlyComposable get() = LocalStyle.current.let { if (it.cutCorners) CutCornerShape(6.dp) else RoundedCornerShape(50) }

fun FontKind.family(): FontFamily = when (this) {
    FontKind.SANS -> FontFamily.SansSerif
    FontKind.SERIF -> FontFamily.Serif
    FontKind.MONO -> FontFamily.Monospace
}

val Mono = FontFamily.Monospace

val CodeStyle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = Mono, fontSize = 13.sp, color = Ink.Text)

@Composable
fun RestDeckTheme(style: StyleSpec, backdrop: Backdrop, content: @Composable () -> Unit) {
    val base = Typography()
    val fam = style.font.family()
    fun TextStyle.f() = copy(fontFamily = fam)
    val typo = Typography(
        displaySmall = base.displaySmall.f().copy(
            fontWeight = FontWeight.Black,
            letterSpacing = if (style.uppercaseTitles) 2.sp else (-1).sp,
        ),
        titleLarge = base.titleLarge.f().copy(fontWeight = FontWeight.Bold, letterSpacing = if (style.uppercaseTitles) 1.5.sp else (-0.5).sp),
        titleMedium = base.titleMedium.f(), titleSmall = base.titleSmall.f(),
        headlineSmall = base.headlineSmall.f(), headlineMedium = base.headlineMedium.f(),
        bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f(),
    )
    val c = style
    val scheme = if (c.dark) darkColorScheme(
        primary = c.accent.toColor(), secondary = c.accent2.toColor(), background = c.bg.toColor(),
        surface = solid(c.card, c.bg), surfaceContainerHigh = solid(c.card, c.bg),
        onSurface = c.text.toColor(), onBackground = c.text.toColor(), onSurfaceVariant = c.muted.toColor(),
        error = c.bad.toColor(), outline = c.stroke.toColor(),
    ) else lightColorScheme(
        primary = c.accent.toColor(), secondary = c.accent2.toColor(), background = c.bg.toColor(),
        surface = solid(c.card, c.bg), surfaceContainerHigh = solid(c.card, c.bg),
        onSurface = c.text.toColor(), onBackground = c.text.toColor(), onSurfaceVariant = c.muted.toColor(),
        error = c.bad.toColor(), outline = c.stroke.toColor(),
    )
    CompositionLocalProvider(LocalStyle provides style, LocalBackdrop provides backdrop) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}

private fun solid(top: Long, bottom: Long): Color = Contrast.over(top, bottom).toColor()

/** Bildschirm-Hintergrund: eigenes Foto (abgedunkelt/weich) oder die Verzierung des Stils. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val style = LocalStyle.current
    val bd = LocalBackdrop.current
    Box(Modifier.fillMaxSize().background(style.bg.toColor())) {
        if (bd.photo != null) {
            val blurOk = Build.VERSION.SDK_INT >= 31 && bd.blur > 0f
            Image(
                bd.photo, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().let { if (blurOk) it.blur(bd.blur.dp) else it },
            )
            val scrim = style.bg.toColor().copy(alpha = bd.dim)
            Box(Modifier.fillMaxSize().drawBehind { drawRect(scrim) })
        } else {
            Box(Modifier.fillMaxSize().styleDecor(style))
        }
        content()
    }
}
