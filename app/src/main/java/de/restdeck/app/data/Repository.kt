package de.restdeck.app.data

import de.restdeck.app.domain.ActionButton
import de.restdeck.app.domain.ButtonRunner
import de.restdeck.app.domain.Deck
import de.restdeck.app.domain.DeckJson
import de.restdeck.app.domain.JsonFormat
import de.restdeck.app.domain.LogEntry
import de.restdeck.app.domain.LogPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Einzige Datenquelle: Buttons + Log als JSON in filesDir (R9). */
class Repository(
    private val dir: File,
    private val runner: ButtonRunner,
    private val scope: CoroutineScope,
) {
    private val deckFile = File(dir, "deck.json")
    private val logFile = File(dir, "log.json")
    /** Seriell, damit nie zwei Saves dieselbe .tmp-Datei beschreiben. */
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val logSerializer = ListSerializer(LogEntry.serializer())

    private val _deck = MutableStateFlow(load(deckFile) { DeckJson.decode(it) } ?: Deck())
    val deck: StateFlow<Deck> = _deck.asStateFlow()

    private val _log = MutableStateFlow(load(logFile) { JsonFormat.decodeFromString(logSerializer, it) } ?: emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    /** IDs der Buttons, deren Calls gerade laufen. */
    private val _running = MutableStateFlow<Set<String>>(emptySet())
    val running: StateFlow<Set<String>> = _running.asStateFlow()

    /** Ergebnis des letzten Auslösens je Button (nur im Speicher, für die Kachel-Anzeige). */
    private val _last = MutableStateFlow<Map<String, List<LogEntry>>>(emptyMap())
    val last: StateFlow<Map<String, List<LogEntry>>> = _last.asStateFlow()

    private fun <T> load(f: File, parse: (String) -> T): T? =
        runCatching { if (f.exists()) parse(f.readText()) else null }.getOrNull()

    private fun save(f: File, text: String) {
        val tmp = File(dir, f.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(f)
    }

    fun edit(f: (Deck) -> Deck) {
        _deck.update(f)
        // Immer den aktuellsten Stand schreiben, dann ist die Reihenfolge der Saves egal
        scope.launch(io) { save(deckFile, DeckJson.encode(_deck.value)) }
    }

    fun clearLog() = setLog(emptyList())

    private fun setLog(l: List<LogEntry>) {
        _log.value = l
        scope.launch(io) { save(logFile, JsonFormat.encodeToString(logSerializer, _log.value)) }
    }

    /** Feuert alle Calls des Buttons parallel und loggt; liefert die neuen Einträge. */
    suspend fun fire(button: ActionButton): List<LogEntry> {
        _running.update { it + button.id }
        try {
            val entries = withContext(Dispatchers.IO) { runner.run(button) }
            _log.update { LogPolicy.append(it, entries) }
            setLog(_log.value)
            _last.update { it + (button.id to entries) }
            return entries
        } finally {
            _running.update { it - button.id }
        }
    }
}
