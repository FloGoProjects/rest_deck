package de.restdeck.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD }

@Serializable
data class Header(val name: String, val value: String)

@Serializable
data class RestCall(
    val id: String = newId(),
    val name: String = "",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "http://",
    val headers: List<Header> = emptyList(),
    val body: String = "",
    val timeoutMs: Int = 10_000,
)

@Serializable
data class ActionButton(
    val id: String = newId(),
    val name: String = "",
    /** Index in die Verlaufs-Palette der UI. */
    val color: Int = 0,
    val calls: List<RestCall> = emptyList(),
)

@Serializable
data class Deck(
    val version: Int = 1,
    val buttons: List<ActionButton> = emptyList(),
) {
    fun button(id: String): ActionButton? = buttons.firstOrNull { it.id == id }

    fun upsertButton(b: ActionButton): Deck =
        if (buttons.any { it.id == b.id }) copy(buttons = buttons.map { if (it.id == b.id) b else it })
        else copy(buttons = buttons + b)

    fun deleteButton(id: String): Deck = copy(buttons = buttons.filterNot { it.id == id })

    fun moveButton(id: String, delta: Int): Deck {
        val i = buttons.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in buttons.indices) return this
        val list = buttons.toMutableList()
        list.add(j, list.removeAt(i))
        return copy(buttons = list)
    }

    private fun editCalls(buttonId: String, f: (List<RestCall>) -> List<RestCall>): Deck =
        copy(buttons = buttons.map { if (it.id == buttonId) it.copy(calls = f(it.calls)) else it })

    fun upsertCall(buttonId: String, c: RestCall): Deck = editCalls(buttonId) { calls ->
        if (calls.any { it.id == c.id }) calls.map { if (it.id == c.id) c else it } else calls + c
    }

    fun deleteCall(buttonId: String, callId: String): Deck = editCalls(buttonId) { it.filterNot { c -> c.id == callId } }

    fun duplicateCall(buttonId: String, callId: String, newId: String = newId()): Deck = editCalls(buttonId) { calls ->
        val i = calls.indexOfFirst { it.id == callId }
        if (i < 0) calls
        else calls.toMutableList().apply { add(i + 1, calls[i].copy(id = newId, name = "${calls[i].name} (Kopie)")) }
    }
}

@Serializable
data class LogEntry(
    val id: String = newId(),
    val time: Long,
    val buttonId: String,
    val buttonName: String,
    val callName: String,
    val method: HttpMethod,
    val url: String,
    val success: Boolean,
    val status: Int? = null,
    val durationMs: Long = 0,
    val body: String = "",
    val error: String? = null,
)

fun newId(): String = UUID.randomUUID().toString()

val JsonFormat = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

object DeckJson {
    fun encode(d: Deck): String = JsonFormat.encodeToString(Deck.serializer(), d)
    fun decode(s: String): Deck = if (s.isBlank()) Deck() else JsonFormat.decodeFromString(Deck.serializer(), s)
}
