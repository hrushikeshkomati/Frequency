# Frequency transport status

Frequency routes packets through a shared Java `PacketTransport` boundary.
Bluetooth LE remains the required fallback; LAN is preferred for direct delivery
when a peer is reachable on the local network. Broadcasts are sent over both
available transports.

## LAN

Devices on the same multicast-capable local network announce their 8-byte peer
ID and temporary TCP port over `239.255.42.99:45871`. Packets are sent over
TCP with a 4-byte length prefix and decoded by the existing packet protocol.
Discovery entries expire after 30 seconds. Incoming data is size-limited and
still passes through the existing signature, replay, and routing checks.

LAN discovery uses Android's Wi-Fi multicast lock and therefore may be
unavailable on networks that block multicast or isolate clients. Bluetooth
continues to operate if LAN startup fails.

## Cellular and other internet networks

Private text messages can travel over NIP-17 encrypted gift wraps on the
configured Nostr relays. This path is selected when a mesh Noise session is not
available and a mutual-favorite Nostr identity mapping exists. The receiver's
account gift-wrap subscription is established by the chat view model, and relay
events are deduplicated by event ID. Pending events are bounded to 500 entries,
expire after 48 hours, and are removed after a relay accepts them.

The relay client retries failed connections with backoff and resets its relay
sockets when Android changes the default internet network (for example, from
Wi-Fi to cellular). This path is relay-assisted store-and-forward, not a direct
phone-to-phone cellular connection. Carrier NAT and firewalls prevent reliable
inbound connections, so direct cellular discovery and NAT traversal are not
implemented.

Relays are third-party services: they can observe connection metadata and
delivery timing even though NIP-17 encrypts message contents. Delivery depends
on relay availability and the recipient returning online before relay
retention expires. This does not provide guaranteed background push delivery,
and does not currently relay public mesh-channel messages or files over
cellular. Do not rely on it for sensitive communications until the app and its
relay configuration receive an independent security review.
