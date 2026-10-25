package com.frequency.android.transport;

import android.util.Log;

import com.frequency.android.model.RoutedPacket;
import com.frequency.android.protocol.FrequencyPacket;

/**
 * Uses a required primary transport and a best-effort secondary transport.
 */
public final class MultiPacketTransport implements PacketTransport {
    private static final String TAG = "MultiPacketTransport";

    private final PacketTransport requiredTransport;
    private final PacketTransport preferredTransport;
    private volatile boolean preferredStarted;

    public MultiPacketTransport(PacketTransport requiredTransport, PacketTransport preferredTransport) {
        if (requiredTransport == null || preferredTransport == null) {
            throw new IllegalArgumentException("Both transports are required");
        }
        this.requiredTransport = requiredTransport;
        this.preferredTransport = preferredTransport;
    }

    @Override
    public synchronized boolean startServices() {
        preferredStarted = false;
        if (!requiredTransport.startServices()) {
            return false;
        }
        try {
            preferredStarted = preferredTransport.startServices();
        } catch (RuntimeException exception) {
            Log.w(TAG, "Preferred transport failed to start; continuing with required transport", exception);
            preferredStarted = false;
        }
        if (!preferredStarted) {
            Log.w(TAG, "Preferred transport did not start; continuing with required transport");
        }
        return true;
    }

    @Override
    public synchronized void stopServices() {
        try {
            preferredTransport.stopServices();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to stop preferred transport", exception);
        } finally {
            preferredStarted = false;
            requiredTransport.stopServices();
        }
    }

    @Override
    public void broadcastPacket(RoutedPacket packet) {
        try {
            requiredTransport.broadcastPacket(packet);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Required transport failed to broadcast packet", exception);
        }
        if (preferredStarted) {
            try {
                preferredTransport.broadcastPacket(packet);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Preferred transport failed to broadcast packet", exception);
            }
        }
    }

    @Override
    public boolean sendPacketToPeer(String peerId, FrequencyPacket packet) {
        if (preferredStarted) {
            try {
                if (preferredTransport.sendPacketToPeer(peerId, packet)) {
                    return true;
                }
            } catch (RuntimeException exception) {
                Log.w(TAG, "Preferred transport failed to send packet to peer", exception);
            }
        }
        return requiredTransport.sendPacketToPeer(peerId, packet);
    }

    @Override
    public boolean sendToPeer(String peerId, RoutedPacket packet) {
        if (preferredStarted) {
            try {
                if (preferredTransport.sendToPeer(peerId, packet)) {
                    return true;
                }
            } catch (RuntimeException exception) {
                Log.w(TAG, "Preferred transport failed to route packet to peer", exception);
            }
        }
        return requiredTransport.sendToPeer(peerId, packet);
    }
}
