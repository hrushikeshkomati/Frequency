package com.frequency.android.contacts

import com.frequency.android.nostr.Bech32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrequencyKeyResolverTest {
    @Test
    fun acceptsHexKey() {
        val hex = "ab".repeat(32)
        assertEquals(hex, FrequencyKeyResolver.normalize(hex.uppercase()))
        assertEquals("nostr_${"ab".repeat(8)}", FrequencyKeyResolver.conversationPeerId(hex))
    }

    @Test
    fun decodesNpubKey() {
        val bytes = ByteArray(32) { (it + 1).toByte() }
        val npub = Bech32.encode("npub", bytes)
        val expected = bytes.joinToString("") { "%02x".format(it) }
        assertEquals(expected, FrequencyKeyResolver.normalize(npub.uppercase()))
        assertNull(FrequencyKeyResolver.normalize("npub1invalid"))
    }

    @Test
    fun rejectsInvalidKey() {
        assertNull(FrequencyKeyResolver.normalize("not-a-key"))
        assertNull(FrequencyKeyResolver.normalize("aa".repeat(31)))
    }
}
