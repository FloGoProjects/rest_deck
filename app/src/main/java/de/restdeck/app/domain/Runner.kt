package de.restdeck.app.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class PreparedRequest(
    val method: HttpMethod,
    val url: String,
    val headers: List<Header>,
    val body: String?,
    val timeoutMs: Int,
)

data class HttpResult(val status: Int, val body: String)

/** Echte Implementierung in data/HttpClient.kt; in Tests ein Fake. Wirft bei Netzwerkfehlern. */
fun interface HttpExecutor {
    suspend fun execute(req: PreparedRequest): HttpResult
}

fun prepare(c: RestCall): PreparedRequest {
    val headers = c.headers
        .map { Header(it.name.trim(), it.value) }
        .filter { it.name.isNotEmpty() }
        .toMutableList()
    val body = c.body.takeIf { it.isNotEmpty() && c.method != HttpMethod.GET && c.method != HttpMethod.HEAD }
    if (body != null && headers.none { it.name.equals("Content-Type", ignoreCase = true) }) {
        val t = body.trimStart()
        headers += Header("Content-Type", if (t.startsWith("{") || t.startsWith("[")) "application/json" else "text/plain; charset=utf-8")
    }
    return PreparedRequest(c.method, c.url.trim(), headers, body, c.timeoutMs.coerceIn(500, 120_000))
}

object LogPolicy {
    const val MAX_ENTRIES = 300
    const val MAX_BODY = 4000

    fun truncate(s: String): String = if (s.length <= MAX_BODY) s else s.take(MAX_BODY) + "…"

    /** Neueste zuerst, gedeckelt. [new] in Call-Reihenfolge. */
    fun append(log: List<LogEntry>, new: List<LogEntry>): List<LogEntry> = (new.reversed() + log).take(MAX_ENTRIES)
}

fun isSuccess(status: Int): Boolean = status in 200..399

/** R4: Alle Calls eines Buttons parallel; jeder Call liefert genau einen Log-Eintrag. */
class ButtonRunner(private val http: HttpExecutor, private val clock: () -> Long = System::currentTimeMillis) {

    suspend fun run(button: ActionButton): List<LogEntry> = coroutineScope {
        button.calls.map { c -> async { runOne(button, c) } }.awaitAll()
    }

    private suspend fun runOne(button: ActionButton, c: RestCall): LogEntry {
        val start = clock()
        val base = LogEntry(
            time = start, buttonId = button.id, buttonName = button.name,
            callName = c.name.ifBlank { c.url }, method = c.method, url = c.url, success = false,
        )
        LocalNetwork.urlProblem(c.url)?.let { return base.copy(error = it) }
        return try {
            val r = http.execute(prepare(c))
            base.copy(success = isSuccess(r.status), status = r.status, body = LogPolicy.truncate(r.body), durationMs = clock() - start)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            base.copy(error = e.message ?: e.javaClass.simpleName, durationMs = clock() - start)
        }
    }
}

fun summary(buttonName: String, entries: List<LogEntry>): String {
    if (entries.isEmpty()) return "$buttonName: keine Calls"
    val ok = entries.count { it.success }
    val failed = entries.size - ok
    return "$buttonName: $ok/${entries.size} OK" + if (failed > 0) " – $failed Fehler" else ""
}
