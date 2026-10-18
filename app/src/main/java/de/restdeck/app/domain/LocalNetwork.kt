package de.restdeck.app.domain

import java.net.URI

/** R6: Calls dürfen nur Ziele im lokalen Netz erreichen. */
object LocalNetwork {

    /** Rohe Adresse (4 oder 16 Bytes) liegt in einem privaten/lokalen Bereich. */
    fun isLocal(addr: ByteArray): Boolean {
        val b = addr.map { it.toInt() and 0xff }
        return when (b.size) {
            4 -> isLocalV4(b)
            16 -> {
                val mappedV4 = (0..9).all { b[it] == 0 } && b[10] == 0xff && b[11] == 0xff
                when {
                    mappedV4 -> isLocalV4(b.subList(12, 16))
                    (0..14).all { b[it] == 0 } && b[15] == 1 -> true // ::1
                    (b[0] and 0xfe) == 0xfc -> true // fc00::/7 ULA
                    b[0] == 0xfe && (b[1] and 0xc0) == 0x80 -> true // fe80::/10
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun isLocalV4(b: List<Int>): Boolean = when {
        b[0] == 10 -> true
        b[0] == 127 -> true
        b[0] == 172 && b[1] in 16..31 -> true
        b[0] == 192 && b[1] == 168 -> true
        b[0] == 169 && b[1] == 254 -> true
        else -> false
    }

    fun host(url: String): String? = runCatching { URI(url.trim()).host }.getOrNull()?.removeSurrounding("[", "]")

    /** Formaler URL-Check vor dem DNS-Lookup; null = in Ordnung. */
    fun urlProblem(url: String): String? {
        val u = url.trim()
        if (u.isEmpty()) return "URL fehlt"
        val uri = runCatching { URI(u) }.getOrElse { return "URL ungültig: ${it.message}" }
        val scheme = uri.scheme?.lowercase() ?: return "URL braucht http:// oder https://"
        if (scheme != "http" && scheme != "https") return "Nur http/https, nicht $scheme"
        if (uri.host.isNullOrBlank()) return "Host fehlt in der URL"
        return null
    }
}
