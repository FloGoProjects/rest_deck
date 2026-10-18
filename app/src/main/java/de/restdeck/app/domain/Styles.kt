package de.restdeck.app.domain

import kotlin.math.pow

/** R11: Grafikstile als reine Daten (ARGB als Long), damit Kontraste testbar sind (R14). */

enum class FontKind { SANS, SERIF, MONO }

/** Hintergrund-Verzierung des ganzen Bildschirms. */
enum class BgDecor { AURORA, GEARS, NONE, SCANLINES }

/** Verzierung der Button-Kacheln. */
enum class TileDecor { GLOSS, RIVETS, NONE, SCANLINES }

/** Farbfeld einer Kachel: Verlauf from→to, Schrift [on]. */
data class Swatch(val from: Long, val to: Long, val on: Long) {
    constructor(flat: Long, on: Long) : this(flat, flat, on)
}

data class StyleSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val dark: Boolean,
    val bg: Long,
    /** Zweitfarbe für Verzierungen (Blobs, Zahnräder, Scanlines). */
    val decorColor: Long,
    /** Kartenfläche, darf transparent sein (wird über [bg] gelegt). */
    val card: Long,
    val stroke: Long,
    val text: Long,
    val muted: Long,
    val ok: Long,
    val bad: Long,
    val accent: Long,
    val accent2: Long,
    val onAccent: Long,
    /** Farben je HttpMethod in Enum-Reihenfolge. */
    val methodColors: List<Long>,
    val swatches: List<Swatch>,
    val font: FontKind,
    val cornerDp: Int,
    val tileCornerDp: Int,
    /** Abgeschrägte statt runder Ecken (viktorianischer Rahmen). */
    val cutCorners: Boolean,
    val strokeDp: Int,
    val bgDecor: BgDecor,
    val tileDecor: TileDecor,
    /** Symbol oben links auf der Kachel; leer = keins. */
    val glyph: String,
    val uppercaseTitles: Boolean,
)

object Styles {
    const val SWATCHES = 8

    val modern = StyleSpec(
        id = "modern", name = "Modern", tagline = "Dunkel, Verläufe, Glas",
        dark = true, bg = 0xFF07070D, decorColor = 0xFF8B5CF6,
        card = 0x14FFFFFF, stroke = 0x1FFFFFFF, text = 0xFFF1F5F9, muted = 0xFF94A3B8,
        ok = 0xFF34D399, bad = 0xFFFB7185, accent = 0xFF8B5CF6, accent2 = 0xFF0891B2, onAccent = 0xFFFFFFFF,
        methodColors = listOf(0xFF34D399, 0xFF60A5FA, 0xFFFBBF24, 0xFFA78BFA, 0xFFFB7185, 0xFF94A3B8),
        swatches = listOf(
            Swatch(0xFF7C3AED, 0xFF0E7490, 0xFFFFFFFF),
            Swatch(0xFFDB2777, 0xFFC2410C, 0xFFFFFFFF),
            Swatch(0xFF047857, 0xFF0369A1, 0xFFFFFFFF),
            Swatch(0xFF2563EB, 0xFF4F46E5, 0xFFFFFFFF),
            Swatch(0xFFB45309, 0xFFDC2626, 0xFFFFFFFF),
            Swatch(0xFF4D7C0F, 0xFF047857, 0xFFFFFFFF),
            Swatch(0xFFC026D3, 0xFF4F46E5, 0xFFFFFFFF),
            Swatch(0xFF475569, 0xFF1E293B, 0xFFFFFFFF),
        ),
        font = FontKind.SANS, cornerDp = 24, tileCornerDp = 28, cutCorners = false, strokeDp = 1,
        bgDecor = BgDecor.AURORA, tileDecor = TileDecor.GLOSS, glyph = "⚡", uppercaseTitles = false,
    )

    val steampunk = StyleSpec(
        id = "steampunk", name = "Viktorianisch", tagline = "Messing, Kupfer, Zahnräder",
        dark = true, bg = 0xFF15100A, decorColor = 0xFFB8860B,
        card = 0x1FD4A55A, stroke = 0x99B8862B, text = 0xFFF3E3C3, muted = 0xFFC9AE7C,
        ok = 0xFF9CCB6B, bad = 0xFFE58A6E, accent = 0xFFD4A84A, accent2 = 0xFFA87132, onAccent = 0xFF1A120B,
        methodColors = listOf(0xFF9CCB6B, 0xFF8FB8D8, 0xFFE3C16F, 0xFFC9A0DC, 0xFFE58A6E, 0xFFC9AE7C),
        swatches = listOf(
            Swatch(0xFFE3C16F, 0xFFB8860B, 0xFF1A120B), // Messing
            Swatch(0xFF9A5A31, 0xFF5E2C14, 0xFFFBEFD5), // Kupfer
            Swatch(0xFF3F7365, 0xFF234640, 0xFFF3E3C3), // Patina
            Swatch(0xFF7E2A2A, 0xFF431212, 0xFFF3E3C3), // Ochsenblut
            Swatch(0xFF5E5E5E, 0xFF2B2B2B, 0xFFF3E3C3), // Eisen
            Swatch(0xFF6E3826, 0xFF381A10, 0xFFF3E3C3), // Mahagoni
            Swatch(0xFFF1DCA7, 0xFFC9A55A, 0xFF2A1C10), // Blattgold
            Swatch(0xFF2F3E5C, 0xFF141B2D, 0xFFE3C16F), // Tinte
        ),
        font = FontKind.SERIF, cornerDp = 10, tileCornerDp = 18, cutCorners = true, strokeDp = 2,
        bgDecor = BgDecor.GEARS, tileDecor = TileDecor.RIVETS, glyph = "⚙", uppercaseTitles = true,
    )

    val plain = StyleSpec(
        id = "plain", name = "Schlicht", tagline = "Hell, flach, ruhig",
        dark = false, bg = 0xFFF6F7F9, decorColor = 0xFFE5E7EB,
        card = 0xFFFFFFFF, stroke = 0x1F000000, text = 0xFF111827, muted = 0xFF4B5563,
        ok = 0xFF15803D, bad = 0xFFB91C1C, accent = 0xFF2563EB, accent2 = 0xFF2563EB, onAccent = 0xFFFFFFFF,
        methodColors = listOf(0xFF15803D, 0xFF1D4ED8, 0xFFB45309, 0xFF6D28D9, 0xFFB91C1C, 0xFF4B5563),
        swatches = listOf(
            Swatch(0xFF2563EB, 0xFFFFFFFF),
            Swatch(0xFFDB2777, 0xFFFFFFFF),
            Swatch(0xFF047857, 0xFFFFFFFF),
            Swatch(0xFF4F46E5, 0xFFFFFFFF),
            Swatch(0xFFB45309, 0xFFFFFFFF),
            Swatch(0xFF4D7C0F, 0xFFFFFFFF),
            Swatch(0xFF7C3AED, 0xFFFFFFFF),
            Swatch(0xFF374151, 0xFFFFFFFF),
        ),
        font = FontKind.SANS, cornerDp = 16, tileCornerDp = 16, cutCorners = false, strokeDp = 1,
        bgDecor = BgDecor.NONE, tileDecor = TileDecor.NONE, glyph = "", uppercaseTitles = false,
    )

    val terminal = StyleSpec(
        id = "terminal", name = "Terminal", tagline = "Phosphor-Grün, Scanlines",
        dark = true, bg = 0xFF030A04, decorColor = 0xFF33FF66,
        card = 0x1433FF66, stroke = 0x6633FF66, text = 0xFFA6FFB5, muted = 0xFF45B862,
        ok = 0xFF33FF66, bad = 0xFFFF5F56, accent = 0xFF1F8F3A, accent2 = 0xFF0F5A22, onAccent = 0xFFD2FFDA,
        methodColors = listOf(0xFF33FF66, 0xFF6CA0FF, 0xFFFFB000, 0xFFFF4FD8, 0xFFFF5F56, 0xFFB0B0B0),
        swatches = listOf(
            Swatch(0xFF0B2A12, 0xFF33FF66),
            Swatch(0xFF2A1E00, 0xFFFFB000),
            Swatch(0xFF002A30, 0xFF00E5FF),
            Swatch(0xFF2A0A26, 0xFFFF4FD8),
            Swatch(0xFF2A0B0A, 0xFFFF5F56),
            Swatch(0xFF1A1A1A, 0xFFE6E6E6),
            Swatch(0xFF262A05, 0xFFF4FF52),
            Swatch(0xFF0A1430, 0xFF6CA0FF),
        ),
        font = FontKind.MONO, cornerDp = 4, tileCornerDp = 4, cutCorners = false, strokeDp = 1,
        bgDecor = BgDecor.SCANLINES, tileDecor = TileDecor.SCANLINES, glyph = ">_", uppercaseTitles = true,
    )

    val all = listOf(modern, steampunk, plain, terminal)

    fun byId(id: String): StyleSpec = all.firstOrNull { it.id == id } ?: modern
}

/** WCAG-Kontrast für ARGB-Farben. */
object Contrast {
    private fun channel(c: Long, shift: Int): Double {
        val v = ((c shr shift) and 0xFF) / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(c: Long): Double = 0.2126 * channel(c, 16) + 0.7152 * channel(c, 8) + 0.0722 * channel(c, 0)

    fun ratio(a: Long, b: Long): Double {
        val la = luminance(a); val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Legt [top] (mit Alpha) über die deckende Farbe [bottom]. */
    fun over(top: Long, bottom: Long): Long {
        val a = ((top shr 24) and 0xFF) / 255.0
        fun mix(shift: Int): Long {
            val t = (top shr shift) and 0xFF; val b = (bottom shr shift) and 0xFF
            return Math.round(t * a + b * (1 - a)) and 0xFF
        }
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
