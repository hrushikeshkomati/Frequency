package com.frequency.android.transport;

import com.frequency.android.model.RoutedPacket;
import com.frequency.android.protocol.FrequencyPacket;

/**
 * Java-facing boundary for packet transports used by the mesh backend.
 */
public interface PacketTransport {
    boolean startServices();

    void stopServices();

    void broadcastPacket(RoutedPacket packet);

    boolean sendPacketToPeer(String peerId, FrequencyPacket packet);

    boolean sendToPeer(String peerId, RoutedPacket packet);
}
