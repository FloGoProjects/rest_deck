package de.restdeck.app.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class RunnerTest {
    private fun call(name: String, url: String = "http://192.168.1.2/$name", body: String = "", headers: List<Header> = emptyList()) =
        RestCall(id = name, name = name, method = HttpMethod.POST, url = url, headers = headers, body = body, timeoutMs = 5000)

    private val button = ActionButton(id = "b1", name = "Licht", calls = listOf(call("a"), call("b"), call("c")))

    @Test fun firesAllCallsInParallel() = runBlocking {
        val running = AtomicInteger(0)
        var maxParallel = 0
        val http = HttpExecutor { _ ->
            val now = running.incrementAndGet()
            synchronized(this) { maxParallel = maxOf(maxParallel, now) }
            delay(100)
            running.decrementAndGet()
            HttpResult(status = 200, body = "ok")
        }
        val entries = ButtonRunner(http, clock = { 1000L }).run(button)
        assertEquals(3, entries.size)
        assertEquals(3, maxParallel)
        assertEquals(listOf("a", "b", "c"), entries.map { it.callName })
    }

    @Test fun oneFailureDoesNotAffectOthers() = runBlocking {
        val http = HttpExecutor { req ->
            if (req.url.endsWith("/b")) throw IOException("Verbindung abgelehnt")
            HttpResult(status = 204, body = "")
        }
        val entries = ButtonRunner(http, clock = { 0L }).run(button)
        assertEquals(listOf(true, false, true), entries.map { it.success })
        assertEquals("Verbindung abgelehnt", entries[1].error)
        assertNull(entries[1].status)
    }

    @Test fun httpErrorStatusIsFailure() = runBlocking {
        val http = HttpExecutor { HttpResult(status = 500, body = "boom") }
        val entries = ButtonRunner(http, clock = { 0L }).run(button)
        assertTrue(entries.none { it.success })
        assertEquals(500, entries[0].status)
        assertEquals("boom", entries[0].body)
    }

    @Test fun invalidUrlIsLoggedWithoutCallingHttp() = runBlocking {
        var calls = 0
        val http = HttpExecutor { calls++; HttpResult(200, "") }
        val b = ActionButton(id = "b", name = "x", calls = listOf(call("bad", url = "nonsense")))
        val entries = ButtonRunner(http, clock = { 0L }).run(b)
        assertEquals(0, calls)
        assertFalse(entries[0].success)
        assertTrue(entries[0].error!!.isNotBlank())
    }

    @Test fun recordsDurationAndButton() = runBlocking {
        var t = 0L
        val http = HttpExecutor { t += 42; HttpResult(200, "") }
        val b = ActionButton(id = "b9", name = "Tor", calls = listOf(call("x")))
        val e = ButtonRunner(http, clock = { t }).run(b).single()
        assertEquals(42L, e.durationMs)
        assertEquals("b9", e.buttonId)
        assertEquals("Tor", e.buttonName)
    }

    @Test fun longBodyIsTruncated() = runBlocking {
        val http = HttpExecutor { HttpResult(200, "x".repeat(10_000)) }
        val e = ButtonRunner(http, clock = { 0L }).run(ActionButton(id = "b", name = "b", calls = listOf(call("x")))).single()
        assertTrue(e.body.length <= LogPolicy.MAX_BODY + 20)
        assertTrue(e.body.endsWith("…"))
    }

    @Test fun prepareAddsJsonContentTypeForJsonBody() {
        val req = prepare(call("x", body = """ {"on":true}"""))
        assertEquals("application/json", req.headers.single { it.name.equals("Content-Type", true) }.value)
    }

    @Test fun prepareAddsTextContentTypeForOtherBody() {
        val req = prepare(call("x", body = "hello"))
        assertEquals("text/plain; charset=utf-8", req.headers.single { it.name.equals("Content-Type", true) }.value)
    }

    @Test fun prepareKeepsExplicitContentType() {
        val req = prepare(call("x", body = "{}", headers = listOf(Header("content-type", "application/xml"))))
        assertEquals(listOf("application/xml"), req.headers.filter { it.name.equals("Content-Type", true) }.map { it.value })
    }

    @Test fun prepareSkipsBlankHeadersAndNoBodyNoContentType() {
        val req = prepare(call("x", headers = listOf(Header(" ", "v"), Header("X-A", "1"))))
        assertEquals(listOf(Header("X-A", "1")), req.headers)
        assertNull(req.body)
    }
}
