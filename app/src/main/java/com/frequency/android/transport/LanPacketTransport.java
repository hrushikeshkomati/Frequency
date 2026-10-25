package com.frequency.android.transport;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.frequency.android.model.RoutedPacket;
import com.frequency.android.protocol.FrequencyPacket;
import com.frequency.android.util.AppConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discovers peers on the same multicast-capable LAN and exchanges framed protocol packets over TCP.
 */
public final class LanPacketTransport implements PacketTransport {
    private static final String TAG = "LanPacketTransport";
    private static final String MULTICAST_ADDRESS = "239.255.42.99";
    private static final int DISCOVERY_PORT = 45871;
    private static final int DISCOVERY_INTERVAL_MS = 5_000;
    private static final long PEER_TIMEOUT_MS = 30_000;
    private static final int MAX_PEERS = 256;
    // The packet envelope can include up to 255 route IDs beyond the payload ceiling.
    private static final int MAX_FRAME_BYTES = AppConstants.Protocol.MAX_PAYLOAD_LENGTH + 4_096;
    private static final int DISCOVERY_PACKET_BYTES = 15;
    private static final byte[] DISCOVERY_MAGIC = "FREQ".getBytes(StandardCharsets.US_ASCII);
    private static final int DISCOVERY_VERSION = 1;

    public interface PacketListener {
        void onPacketReceived(FrequencyPacket packet, String peerId, String remoteAddress);
    }

    public interface PeerListener {
        void onPeerDiscovered(String peerId);
    }

    private static final class PeerEndpoint {
        final InetAddress address;
        final int port;
        volatile long lastSeen;

        PeerEndpoint(InetAddress address, int port, long lastSeen) {
            this.address = address;
            this.port = port;
            this.lastSeen = lastSeen;
        }
    }

    private final Context context;
    private final String peerId;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, PeerEndpoint> peers = new ConcurrentHashMap<>();

    private volatile PacketListener packetListener;
    private volatile PeerListener peerListener;
    private volatile MulticastSocket discoverySocket;
    private volatile ServerSocket serverSocket;
    private volatile WifiManager.MulticastLock multicastLock;
    private volatile ThreadPoolExecutor connectionExecutor;
    private volatile ThreadPoolExecutor sendExecutor;
    private volatile Thread discoveryThread;
    private volatile Thread acceptThread;
    private volatile Thread announceThread;
    private volatile int tcpPort;

    public LanPacketTransport(Context context, String peerId) {
        if (context == null || peerId == null || !peerId.matches("(?i)[0-9a-f]{16}")) {
            throw new IllegalArgumentException("A context and 16-character hexadecimal peer ID are required");
        }
        this.context = context.getApplicationContext();
        this.peerId = peerId.toLowerCase(java.util.Locale.ROOT);
    }

    public void setPacketListener(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    public void setPeerListener(PeerListener peerListener) {
        this.peerListener = peerListener;
    }

    public boolean isPeerAvailable(String targetPeerId) {
        return running.get() && getActivePeer(targetPeerId) != null;
    }

    @Override
    public synchronized boolean startServices() {
        if (running.get()) {
            return true;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(0));
            tcpPort = serverSocket.getLocalPort();

            MulticastSocket socket = new MulticastSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setTimeToLive(1);
            socket.setSoTimeout(1_000);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            discoverySocket = socket;

            WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock(TAG);
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            }

            connectionExecutor = new ThreadPoolExecutor(
                    2, 4, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(32),
                    runnable -> new Thread(runnable, "frequency-lan-connection"),
                    new ThreadPoolExecutor.AbortPolicy());
            sendExecutor = new ThreadPoolExecutor(
                    1, 4, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(256),
                    runnable -> new Thread(runnable, "frequency-lan-send"),
                    new ThreadPoolExecutor.AbortPolicy());
            running.set(true);

            discoveryThread = new Thread(this::receiveAnnouncements, "frequency-lan-discovery");
            acceptThread = new Thread(this::acceptConnections, "frequency-lan-accept");
            announceThread = new Thread(this::announcePresence, "frequency-lan-announce");
            discoveryThread.start();
            acceptThread.start();
            announceThread.start();
            Log.i(TAG, "LAN transport started on TCP port " + tcpPort);
            return true;
        } catch (IOException | RuntimeException exception) {
            Log.w(TAG, "Unable to start LAN transport", exception);
            stopServices();
            return false;
        }
    }

    @Override
    public synchronized void stopServices() {
        running.set(false);
        interrupt(discoveryThread);
        interrupt(acceptThread);
        interrupt(announceThread);
        discoveryThread = null;
        acceptThread = null;
        announceThread = null;
        closeQuietly(discoverySocket);
        discoverySocket = null;
        closeQuietly(serverSocket);
        serverSocket = null;
        if (connectionExecutor != null) {
            connectionExecutor.shutdownNow();
            connectionExecutor = null;
        }
        if (sendExecutor != null) {
            sendExecutor.shutdownNow();
            sendExecutor = null;
        }
        WifiManager.MulticastLock lock = multicastLock;
        multicastLock = null;
        if (lock != null && lock.isHeld()) {
            lock.release();
        }
        peers.clear();
    }

    @Override
    public void broadcastPacket(RoutedPacket routedPacket) {
        if (!running.get()) {
            return;
        }
        byte[] encoded = encodePacket(routedPacket.getPacket());
        if (encoded == null) {
            return;
        }
        for (Map.Entry<String, PeerEndpoint> entry : activePeers().entrySet()) {
            sendAsync(entry.getKey(), entry.getValue(), encoded);
        }
    }

    @Override
    public boolean sendPacketToPeer(String targetPeerId, FrequencyPacket packet) {
        if (targetPeerId == null || targetPeerId.equalsIgnoreCase(peerId) || !running.get()) {
            return false;
        }
        PeerEndpoint endpoint = getActivePeer(targetPeerId);
        byte[] encoded = encodePacket(packet);
        return endpoint != null && encoded != null && sendFrame(endpoint, encoded);
    }

    @Override
    public boolean sendToPeer(String targetPeerId, RoutedPacket routedPacket) {
        return sendPacketToPeer(targetPeerId, routedPacket.getPacket());
    }

    private void announcePresence() {
        while (running.get()) {
            sendDiscoveryAnnouncement();
            try {
                Thread.sleep(DISCOVERY_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void sendDiscoveryAnnouncement() {
        MulticastSocket socket = discoverySocket;
        if (!running.get() || socket == null) {
            return;
        }
        byte[] announcement = createDiscoveryAnnouncement(peerId, tcpPort);
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.send(new DatagramPacket(
                    announcement, announcement.length, group, DISCOVERY_PORT));
        } catch (IOException exception) {
            if (running.get()) {
                Log.w(TAG, "Failed to announce LAN presence", exception);
            }
        }
    }

    private void receiveAnnouncements() {
        byte[] buffer = new byte[DISCOVERY_PACKET_BYTES + 1];
        while (running.get()) {
            MulticastSocket socket = discoverySocket;
            if (socket == null) {
                return;
            }
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(datagram);
                String discoveredPeer = parsePeerId(datagram.getData(), datagram.getLength());
                if (discoveredPeer == null || discoveredPeer.equals(peerId)) {
                    continue;
                }
                int port = parsePort(datagram.getData(), datagram.getLength());
                if (port == 0 || port == tcpPort) {
                    continue;
                }
                prunePeers();
                if (peers.size() < MAX_PEERS || peers.containsKey(discoveredPeer)) {
                    AtomicBoolean newEndpoint = new AtomicBoolean(false);
                    peers.compute(discoveredPeer, (ignored, existing) -> {
                        if (existing != null
                                && existing.address.equals(datagram.getAddress())
                                && existing.port == port) {
                            existing.lastSeen = System.currentTimeMillis();
                            return existing;
                        }
                        newEndpoint.set(true);
                        return new PeerEndpoint(
                                datagram.getAddress(), port, System.currentTimeMillis());
                    });
                    if (newEndpoint.get()) {
                        PeerListener listener = peerListener;
                        if (listener != null) {
                            listener.onPeerDiscovered(discoveredPeer);
                        }
                    }
                }
            } catch (SocketException exception) {
                if (running.get()) {
                    Log.w(TAG, "LAN discovery socket failed", exception);
                }
                return;
            } catch (IOException exception) {
                if (running.get() && !(exception instanceof java.net.SocketTimeoutException)) {
                    Log.w(TAG, "Failed to receive LAN discovery announcement", exception);
                }
            }
        }
    }

    private void acceptConnections() {
        while (running.get()) {
            ServerSocket socket = serverSocket;
            if (socket == null) {
                return;
            }
            try {
                Socket connection = socket.accept();
                connection.setSoTimeout(5_000);
                ThreadPoolExecutor executor = connectionExecutor;
                if (executor == null) {
                    closeQuietly(connection);
                    return;
                }
                try {
                    executor.execute(() -> receivePacket(connection));
                } catch (RejectedExecutionException exception) {
                    Log.w(TAG, "Dropping LAN connection: connection queue is full");
                    closeQuietly(connection);
                }
            } catch (SocketException exception) {
                if (running.get()) {
                    Log.w(TAG, "LAN listener socket failed", exception);
                }
                return;
            } catch (IOException exception) {
                if (running.get()) {
                    Log.w(TAG, "Failed to accept LAN connection", exception);
                }
            }
        }
    }

    private void receivePacket(Socket connection) {
        try (Socket socket = connection;
             DataInputStream input = new DataInputStream(socket.getInputStream())) {
            int frameLength = input.readInt();
            if (frameLength <= 0 || frameLength > MAX_FRAME_BYTES) {
                Log.w(TAG, "Rejected LAN frame with invalid length: " + frameLength);
                return;
            }
            byte[] frame = new byte[frameLength];
            input.readFully(frame);
            FrequencyPacket packet = FrequencyPacket.Companion.fromBinaryData(frame);
            if (packet == null || packet.getSenderID().length != 8) {
                Log.w(TAG, "Rejected malformed LAN packet");
                return;
            }
            String senderPeerId = encodePeerId(packet.getSenderID());
            if (!peerId.equals(senderPeerId)) {
                PacketListener listener = packetListener;
                if (listener != null) {
                    listener.onPacketReceived(
                            packet, senderPeerId,
                            "lan:" + socket.getInetAddress().getHostAddress());
                }
            }
        } catch (EOFException exception) {
            Log.d(TAG, "LAN peer closed before sending a complete packet");
        } catch (IOException | RuntimeException exception) {
            Log.w(TAG, "Failed to receive LAN packet", exception);
        }
    }

    private void sendAsync(String targetPeerId, PeerEndpoint endpoint, byte[] encoded) {
        ThreadPoolExecutor executor = sendExecutor;
        if (executor == null) {
            return;
        }
        try {
            executor.execute(() -> {
                if (!sendFrame(endpoint, encoded)) {
                    peers.remove(targetPeerId, endpoint);
                }
            });
        } catch (RejectedExecutionException exception) {
            Log.w(TAG, "Dropping LAN packet: send queue is full");
        }
    }

    private boolean sendFrame(PeerEndpoint endpoint, byte[] encoded) {
        if (!running.get() || encoded.length > MAX_FRAME_BYTES) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.address, endpoint.port), 2_000);
            socket.setSoTimeout(5_000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(encoded.length);
            output.write(encoded);
            output.flush();
            return true;
        } catch (IOException exception) {
            Log.w(TAG, "Failed to send LAN packet to " + endpoint.address.getHostAddress(), exception);
            return false;
        }
    }

    private byte[] encodePacket(FrequencyPacket packet) {
        if (packet == null) {
            Log.e(TAG, "Cannot send a null packet");
            return null;
        }
        byte[] encoded = packet.toBinaryData();
        if (encoded == null || encoded.length == 0 || encoded.length > MAX_FRAME_BYTES) {
            Log.e(TAG, "Packet could not be encoded or exceeds the LAN frame limit");
            return null;
        }
        return encoded;
    }

    private Map<String, PeerEndpoint> activePeers() {
        prunePeers();
        return new ConcurrentHashMap<>(peers);
    }

    private PeerEndpoint getActivePeer(String targetPeerId) {
        prunePeers();
        return peers.get(targetPeerId.toLowerCase(java.util.Locale.ROOT));
    }

    private void prunePeers() {
        long cutoff = System.currentTimeMillis() - PEER_TIMEOUT_MS;
        peers.entrySet().removeIf(entry -> entry.getValue().lastSeen < cutoff);
    }

    static byte[] createDiscoveryAnnouncement(String peerId, int tcpPort) {
        if (peerId == null || !peerId.matches("(?i)[0-9a-f]{16}")
                || tcpPort < 1 || tcpPort > 65_535) {
            throw new IllegalArgumentException("Invalid LAN discovery identity or port");
        }
        byte[] announcement = new byte[DISCOVERY_PACKET_BYTES];
        System.arraycopy(DISCOVERY_MAGIC, 0, announcement, 0, DISCOVERY_MAGIC.length);
        announcement[4] = (byte) DISCOVERY_VERSION;
        System.arraycopy(decodePeerId(peerId), 0, announcement, 5, 8);
        announcement[13] = (byte) ((tcpPort >>> 8) & 0xff);
        announcement[14] = (byte) (tcpPort & 0xff);
        return announcement;
    }

    static String parsePeerId(byte[] data, int length) {
        if (data == null || length != DISCOVERY_PACKET_BYTES || data.length < length
                || !Arrays.equals(DISCOVERY_MAGIC, Arrays.copyOfRange(data, 0, 4))
                || (data[4] & 0xff) != DISCOVERY_VERSION) {
            return null;
        }
        return encodePeerId(Arrays.copyOfRange(data, 5, 13));
    }

    static int parsePort(byte[] data, int length) {
        if (parsePeerId(data, length) == null) {
            return 0;
        }
        return ((data[13] & 0xff) << 8) | (data[14] & 0xff);
    }

    private static byte[] decodePeerId(String value) {
        byte[] result = new byte[8];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private static String encodePeerId(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte element : value) {
            result.append(String.format(java.util.Locale.ROOT, "%02x", element & 0xff));
        }
        return result.toString();
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            Log.d(TAG, "Socket was already closed");
        }
    }

    private static void interrupt(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }
}
