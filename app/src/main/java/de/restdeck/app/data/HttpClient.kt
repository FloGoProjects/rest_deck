package de.restdeck.app.data

import de.restdeck.app.domain.HttpExecutor
import de.restdeck.app.domain.HttpMethod
import de.restdeck.app.domain.HttpResult
import de.restdeck.app.domain.LocalNetwork
import de.restdeck.app.domain.LogPolicy
import de.restdeck.app.domain.PreparedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * HttpURLConnection-basierter Client. Erzwingt R6: alle aufgelösten Adressen müssen lokal sein.
 * Redirects werden nicht verfolgt (ein Redirect könnte sonst ins Internet führen).
 */
class HttpClient(private val hasLocalNetworkPermission: () -> Boolean) : HttpExecutor {

    override suspend fun execute(req: PreparedRequest): HttpResult = withContext(Dispatchers.IO) {
        if (!hasLocalNetworkPermission()) throw IOException("Berechtigung „Lokales Netzwerk“ fehlt – App öffnen und erlauben")
        val host = LocalNetwork.host(req.url) ?: throw IOException("Host fehlt")
        val addrs = InetAddress.getAllByName(host)
        val foreign = addrs.firstOrNull { !LocalNetwork.isLocal(it.address) }
        if (foreign != null) throw IOException("Kein lokales Ziel: $host → ${foreign.hostAddress}")

        val conn = URL(req.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = req.method.name
            conn.connectTimeout = req.timeoutMs
            conn.readTimeout = req.timeoutMs
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            req.headers.forEach { conn.addRequestProperty(it.name, it.value) }
            if (req.body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(req.body.toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status >= 400) conn.errorStream else if (req.method == HttpMethod.HEAD) null else conn.inputStream
            val body = stream?.use { s ->
                // Nicht mehr lesen als ins Log passt (+1 Zeichen, damit gekürzt wird)
                s.bufferedReader(Charsets.UTF_8).let { r ->
                    val buf = CharArray(LogPolicy.MAX_BODY + 1)
                    var n = 0
                    while (n < buf.size) { val k = r.read(buf, n, buf.size - n); if (k < 0) break; n += k }
                    String(buf, 0, n)
                }
            } ?: ""
            HttpResult(status, body)
        } finally {
            conn.disconnect()
        }
    }
}
