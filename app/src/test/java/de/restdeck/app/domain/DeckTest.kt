package de.restdeck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeckTest {
    private val c1 = RestCall(id = "c1", name = "An", url = "http://10.0.0.2/on")
    private val b1 = ActionButton(id = "b1", name = "Licht", calls = listOf(c1))
    private val b2 = ActionButton(id = "b2", name = "Tor")
    private val deck = Deck(buttons = listOf(b1, b2))

    @Test fun jsonRoundTrip() {
        val withHeaders = deck.upsertCall("b1", c1.copy(headers = listOf(Header("A", "1")), body = "{}", method = HttpMethod.PUT))
        assertEquals(withHeaders, DeckJson.decode(DeckJson.encode(withHeaders)))
    }

    @Test fun decodeToleratesUnknownFieldsAndEmpty() {
        assertEquals(Deck(), DeckJson.decode(""))
        assertEquals(1, DeckJson.decode("""{"version":1,"buttons":[{"id":"x","name":"n","future":1}]}""").buttons.size)
    }

    @Test fun upsertButtonReplacesOrAppends() {
        assertEquals("Licht 2", deck.upsertButton(b1.copy(name = "Licht 2")).buttons[0].name)
        assertEquals(3, deck.upsertButton(ActionButton(id = "b3", name = "Neu")).buttons.size)
    }

    @Test fun deleteAndMoveButton() {
        assertEquals(listOf("b2"), deck.deleteButton("b1").buttons.map { it.id })
        assertEquals(listOf("b2", "b1"), deck.moveButton("b1", +1).buttons.map { it.id })
        assertEquals(listOf("b1", "b2"), deck.moveButton("b1", -1).buttons.map { it.id })
    }

    @Test fun callEdits() {
        val c2 = RestCall(id = "c2", name = "Aus")
        val added = deck.upsertCall("b1", c2)
        assertEquals(listOf("c1", "c2"), added.button("b1")!!.calls.map { it.id })
        assertEquals(listOf("c2"), added.deleteCall("b1", "c1").button("b1")!!.calls.map { it.id })
        val dup = deck.duplicateCall("b1", "c1", newId = "c9").button("b1")!!.calls
        assertEquals(listOf("c1", "c9"), dup.map { it.id })
        assertEquals("An (Kopie)", dup[1].name)
        assertNotEquals(dup[0].id, dup[1].id)
    }

    @Test fun logKeepsNewestFirstAndCaps() {
        val e = { i: Int -> LogEntry(id = "$i", time = i.toLong(), buttonId = "b", buttonName = "b", callName = "c", method = HttpMethod.GET, url = "u", success = true) }
        var log = emptyList<LogEntry>()
        repeat(LogPolicy.MAX_ENTRIES + 5) { log = LogPolicy.append(log, listOf(e(it))) }
        assertEquals(LogPolicy.MAX_ENTRIES, log.size)
        assertEquals("${LogPolicy.MAX_ENTRIES + 4}", log.first().id)
    }

    @Test fun summaryText() {
        val ok = LogEntry(id = "1", time = 0, buttonId = "b", buttonName = "Licht", callName = "c", method = HttpMethod.GET, url = "u", success = true)
        assertEquals("Licht: 2/2 OK", summary("Licht", listOf(ok, ok)))
        assertEquals("Licht: 1/2 OK – 1 Fehler", summary("Licht", listOf(ok, ok.copy(success = false))))
        assertEquals("Licht: keine Calls", summary("Licht", emptyList()))
    }
}
