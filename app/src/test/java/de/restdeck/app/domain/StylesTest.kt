package de.restdeck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StylesTest {

    @Test fun contrastOfBlackOnWhiteIs21() {
        assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF777777, 0xFF777777), 0.01)
    }

    @Test fun overCompositesAlpha() {
        // 50 % Weiß über Schwarz ≈ mittleres Grau
        val c = Contrast.over(0x80FFFFFF, 0xFF000000)
        assertEquals(0xFF, (c shr 24) and 0xFF)
        assertTrue(((c shr 16) and 0xFF) in 0x7E..0x81)
    }

    @Test fun atLeastFourStylesWithUniqueIds() {
        assertTrue(Styles.all.size >= 4)
        assertEquals(Styles.all.size, Styles.all.map { it.id }.toSet().size)
        assertTrue(Styles.all.map { it.id }.containsAll(listOf("modern", "steampunk", "plain")))
    }

    @Test fun unknownIdFallsBackToModern() {
        assertEquals("modern", Styles.byId("gibtsnicht").id)
        assertEquals("steampunk", Styles.byId("steampunk").id)
    }

    @Test fun allStylesHaveSamePaletteSize() {
        val sizes = Styles.all.map { it.swatches.size }.toSet()
        assertEquals(setOf(Styles.SWATCHES), sizes)
    }

    @Test fun textContrastOnBackgroundAndCards() {
        for (s in Styles.all) {
            val card = Contrast.over(s.card, s.bg)
            assertTrue("${s.id} Text/Hintergrund", Contrast.ratio(s.text, s.bg) >= 4.5)
            assertTrue("${s.id} Text/Karte", Contrast.ratio(s.text, card) >= 4.5)
            assertTrue("${s.id} Nebentext/Hintergrund", Contrast.ratio(s.muted, s.bg) >= 3.0)
            assertTrue("${s.id} Nebentext/Karte", Contrast.ratio(s.muted, card) >= 3.0)
            assertTrue("${s.id} OK-Farbe", Contrast.ratio(s.ok, card) >= 3.0)
            assertTrue("${s.id} Fehler-Farbe", Contrast.ratio(s.bad, card) >= 3.0)
            assertEquals(HttpMethod.entries.size, s.methodColors.size)
            s.methodColors.forEachIndexed { i, m -> assertTrue("${s.id} Methode $i", Contrast.ratio(m, card) >= 3.0) }
        }
    }

    @Test fun tileTextReadableOnBothGradientEnds() {
        for (s in Styles.all) for ((i, w) in s.swatches.withIndex()) {
            assertTrue("${s.id} Kachel $i Anfang", Contrast.ratio(w.on, w.from) >= 3.0)
            assertTrue("${s.id} Kachel $i Ende", Contrast.ratio(w.on, w.to) >= 3.0)
        }
    }

    @Test fun accentButtonTextReadable() {
        for (s in Styles.all) {
            assertTrue("${s.id} Akzent-Button", Contrast.ratio(s.onAccent, s.accent) >= 3.0)
            assertTrue("${s.id} Akzent-Button Ende", Contrast.ratio(s.onAccent, s.accent2) >= 3.0)
        }
    }
}
