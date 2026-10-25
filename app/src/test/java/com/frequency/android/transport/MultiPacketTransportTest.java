package com.frequency.android.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.frequency.android.model.RoutedPacket;
import com.frequency.android.protocol.FrequencyPacket;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiPacketTransportTest {
    @Test
    public void prefersLanAndFallsBackToBluetooth() {
        RecordingTransport bluetooth = new RecordingTransport("bluetooth");
        RecordingTransport lan = new RecordingTransport("lan");
        bluetooth.sendResult = true;
        lan.sendResult = true;
        MultiPacketTransport transport = new MultiPacketTransport(bluetooth, lan);

        assertTrue(transport.startServices());
        assertTrue(transport.sendPacketToPeer("peer", null));
        assertEquals(Arrays.asList("lan"), lan.sendCalls);
        assertTrue(bluetooth.sendCalls.isEmpty());

        lan.sendResult = false;
        assertTrue(transport.sendPacketToPeer("peer", null));
        assertEquals(Arrays.asList("lan", "lan"), lan.sendCalls);
        assertEquals(Arrays.asList("bluetooth"), bluetooth.sendCalls);
        transport.stopServices();
    }

    @Test
    public void lanFailureDoesNotPreventBluetoothStartup() {
        RecordingTransport bluetooth = new RecordingTransport("bluetooth");
        RecordingTransport lan = new RecordingTransport("lan");
        lan.startResult = false;
        MultiPacketTransport transport = new MultiPacketTransport(bluetooth, lan);

        assertTrue(transport.startServices());
        assertFalse(lan.started);
        assertTrue(bluetooth.started);
        transport.stopServices();
    }

    private static final class RecordingTransport implements PacketTransport {
        final String name;
        final List<String> sendCalls = new ArrayList<>();
        boolean startResult = true;
        boolean sendResult;
        boolean started;

        RecordingTransport(String name) {
            this.name = name;
        }

        @Override
        public boolean startServices() {
            started = startResult;
            return startResult;
        }

        @Override
        public void stopServices() {
            started = false;
        }

        @Override
        public void broadcastPacket(RoutedPacket packet) {
        }

        @Override
        public boolean sendPacketToPeer(String peerId, FrequencyPacket packet) {
            sendCalls.add(name);
            return sendResult;
        }

        @Override
        public boolean sendToPeer(String peerId, RoutedPacket packet) {
            return sendPacketToPeer(peerId, packet == null ? null : packet.getPacket());
        }
    }
}
