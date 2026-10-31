package com.frequency.android.contacts

import com.frequency.android.nostr.Bech32

object FrequencyKeyResolver {
    fun normalize(input: String): String? {
        val normalized = input.trim().lowercase()
        if (normalized.matches(Regex("^[0-9a-f]{64}$"))) return normalized
        if (!normalized.startsWith("npub1")) return null
        return runCatching {
            val (hrp, bytes) = Bech32.decode(normalized)
            if (hrp != "npub" || bytes.size != 32) null
            else bytes.joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    fun conversationPeerId(key: String): String? = normalize(key)?.let { "nostr_${it.take(16)}" }
}
