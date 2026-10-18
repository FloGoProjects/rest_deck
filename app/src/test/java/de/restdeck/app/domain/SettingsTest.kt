package de.restdeck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test fun defaults() {
        val s = AppSettings()
        assertEquals("modern", s.styleId)
        assertEquals(false, s.hasPhoto)
        assertEquals(0.6f, s.dim, 0.001f)
        assertEquals(0f, s.blur, 0.001f)
    }

    @Test fun jsonRoundTrip() {
        val s = AppSettings(styleId = "steampunk", hasPhoto = true, photoVersion = 7, dim = 0.3f, blur = 12f)
        assertEquals(s, SettingsJson.decode(SettingsJson.encode(s)))
    }

    @Test fun decodeEmptyOrBrokenGivesDefaults() {
        assertEquals(AppSettings(), SettingsJson.decode(""))
        assertEquals(AppSettings(), SettingsJson.decode("{kaputt"))
    }

    @Test fun decodeClampsOutOfRangeValues() {
        val s = SettingsJson.decode("""{"styleId":"plain","dim":3.0,"blur":-5}""")
        assertEquals(AppSettings.MAX_DIM, s.dim, 0.001f)
        assertEquals(0f, s.blur, 0.001f)
    }

    @Test fun normalizedClampsAndKeepsStyle() {
        val s = AppSettings(styleId = "terminal", dim = -1f, blur = 99f).normalized()
        assertEquals("terminal", s.styleId)
        assertEquals(0f, s.dim, 0.001f)
        assertEquals(AppSettings.MAX_BLUR, s.blur, 0.001f)
    }
}
