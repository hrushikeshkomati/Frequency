package com.frequency.android.nostr

import android.util.Base64
import com.frequency.android.model.NoisePayload
import com.frequency.android.model.PrivateMessagePacket
import com.frequency.android.protocol.FrequencyPacket
import com.frequency.android.protocol.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NostrCellularMessageTest {
    @Test
    fun `private message is encrypted and survives relay packet round trip`() {
        val sender = NostrIdentity.fromSeed("frequency-cellular-sender")
        val recipient = NostrIdentity.fromSeed("frequency-cellular-recipient")
        val message = "Cellular relay test"
        val messageId = "message-123"

        val embedded = NostrEmbeddedFrequency.encodePMForNostr(
            content = message,
            messageID = messageId,
            recipientPeerID = "0123456789abcdef",
            senderPeerID = "fedcba9876543210"
        )
        assertNotNull(embedded)

        val giftWrap = NostrProtocol.createPrivateMessage(
            content = embedded!!,
            recipientPubkey = recipient.publicKeyHex,
            senderIdentity = sender
        ).single()
        assertEquals(NostrKind.GIFT_WRAP, giftWrap.kind)

        val decrypted = NostrProtocol.decryptPrivateMessage(giftWrap, recipient)
        assertNotNull(decrypted)
        assertEquals(embedded, decrypted!!.first)

        val packetBytes = Base64.decode(
            embedded.removePrefix("frequency1:"),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val packet = FrequencyPacket.fromBinaryData(packetBytes)
        assertNotNull(packet)
        assertEquals(MessageType.NOISE_ENCRYPTED.value, packet!!.type)
        assertTrue(packet.recipientID!!.contentEquals(byteArrayOf(1, 0x23, 0x45, 0x67, 0x89.toByte(), 0xab.toByte(), 0xcd.toByte(), 0xef.toByte())))

        val noisePayload = NoisePayload.decode(packet.payload)
        assertNotNull(noisePayload)
        val privateMessage = PrivateMessagePacket.decode(noisePayload!!.data)
        assertNotNull(privateMessage)
        assertEquals(messageId, privateMessage!!.messageID)
        assertEquals(message, privateMessage.content)
    }
}
