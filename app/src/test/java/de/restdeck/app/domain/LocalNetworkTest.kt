package de.restdeck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {
    private fun v4(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    @Test fun privateIpv4RangesAreLocal() {
        assertTrue(LocalNetwork.isLocal(v4(192, 168, 1, 20)))
        assertTrue(LocalNetwork.isLocal(v4(10, 0, 0, 1)))
        assertTrue(LocalNetwork.isLocal(v4(172, 16, 0, 1)))
        assertTrue(LocalNetwork.isLocal(v4(172, 31, 255, 255)))
        assertTrue(LocalNetwork.isLocal(v4(127, 0, 0, 1)))
        assertTrue(LocalNetwork.isLocal(v4(169, 254, 3, 4)))
    }

    @Test fun publicIpv4IsNotLocal() {
        assertFalse(LocalNetwork.isLocal(v4(8, 8, 8, 8)))
        assertFalse(LocalNetwork.isLocal(v4(172, 32, 0, 1)))
        assertFalse(LocalNetwork.isLocal(v4(172, 15, 0, 1)))
        assertFalse(LocalNetwork.isLocal(v4(192, 169, 0, 1)))
    }

    @Test fun ipv6LocalRanges() {
        val loopback = ByteArray(16).also { it[15] = 1 }
        val ula = ByteArray(16).also { it[0] = 0xfd.toByte() }
        val linkLocal = ByteArray(16).also { it[0] = 0xfe.toByte(); it[1] = 0x80.toByte() }
        val global = ByteArray(16).also { it[0] = 0x20; it[1] = 0x01 }
        assertTrue(LocalNetwork.isLocal(loopback))
        assertTrue(LocalNetwork.isLocal(ula))
        assertTrue(LocalNetwork.isLocal(linkLocal))
        assertFalse(LocalNetwork.isLocal(global))
    }

    @Test fun ipv4MappedIpv6UsesEmbeddedAddress() {
        val mapped = ByteArray(16).also { it[10] = -1; it[11] = -1; it[12] = 192.toByte(); it[13] = 168.toByte(); it[14] = 0; it[15] = 5 }
        assertTrue(LocalNetwork.isLocal(mapped))
        val mappedPublic = ByteArray(16).also { it[10] = -1; it[11] = -1; it[12] = 8; it[13] = 8; it[14] = 8; it[15] = 8 }
        assertFalse(LocalNetwork.isLocal(mappedPublic))
    }

    @Test fun urlValidation() {
        assertNull(LocalNetwork.urlProblem("http://192.168.1.2/api"))
        assertNull(LocalNetwork.urlProblem("https://homeassistant.local:8123/api/services"))
        assertNotNull(LocalNetwork.urlProblem(""))
        assertNotNull(LocalNetwork.urlProblem("ftp://192.168.1.2"))
        assertNotNull(LocalNetwork.urlProblem("192.168.1.2/api"))
        assertNotNull(LocalNetwork.urlProblem("http://"))
    }

    @Test fun hostOfUrl() {
        assertEquals("192.168.1.2", LocalNetwork.host("http://192.168.1.2:8080/x"))
        assertEquals("nas.local", LocalNetwork.host("http://nas.local/x"))
    }
}
