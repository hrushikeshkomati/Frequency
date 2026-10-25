package com.frequency.android.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class LanPacketTransportTest {
    @Test
    public void discoveryAnnouncementRoundTripsPeerAndPort() {
        byte[] announcement =
                LanPacketTransport.createDiscoveryAnnouncement("01aB23cd45EF6789", 45_871);

        assertEquals("01ab23cd45ef6789",
                LanPacketTransport.parsePeerId(announcement, announcement.length));
        assertEquals(45_871, LanPacketTransport.parsePort(announcement, announcement.length));
    }

    @Test
    public void discoveryAnnouncementRejectsMalformedPackets() {
        byte[] announcement =
                LanPacketTransport.createDiscoveryAnnouncement("01ab23cd45ef6789", 45_871);
        announcement[0] = 0;

        assertNull(LanPacketTransport.parsePeerId(announcement, announcement.length));
        assertEquals(0, LanPacketTransport.parsePort(announcement, announcement.length));
    }

    @Test
    public void discoveryAnnouncementRejectsInvalidIdentityAndPort() {
        assertThrows(IllegalArgumentException.class,
                () -> LanPacketTransport.createDiscoveryAnnouncement("not-a-peer", 45_871));
        assertThrows(IllegalArgumentException.class,
                () -> LanPacketTransport.createDiscoveryAnnouncement("01ab23cd45ef6789", 0));
    }
}
