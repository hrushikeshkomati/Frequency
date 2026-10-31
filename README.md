# Frequency for Android

Frequency is a native Android peer-to-peer messaging app. It currently supports
Bluetooth LE mesh and local-network peer discovery and packet delivery. The UI
is native Android with Jetpack Compose; the transport boundary and LAN transport
are implemented in Java while the existing app and mesh logic are being migrated
incrementally.

> **Development status:** Frequency is a work in progress and has not received an
> independent security review. Do not rely on it for sensitive communications.
> LAN discovery depends on multicast being allowed by the local network.
> Direct mobile-data peer connectivity is not implemented.

## Build
Check out the [Releases](https://github.com/hrushikeshkomati/Frequency/releases) tab.

## Transports

- **Bluetooth LE:** mesh discovery and packet relay.
- **LAN/Wi-Fi:** multicast peer discovery and direct TCP packet delivery on the
  same multicast-capable local network; Bluetooth LE remains the fallback.
- **Mobile data / internet:** private text messages can use NIP-17 encrypted
  Nostr relay delivery when both peers have the required mutual-favorite
  identity mapping. This is relay-assisted messaging, not a direct cellular
  socket; delivery needs internet access and an available relay. Cellular
  connectivity is retried when Android changes its default network.

See [transport status](./docs/FREQUENCY_TRANSPORTS.md) for implementation
details, privacy implications, and limitations.

## Project documentation

- [Privacy policy](./PRIVACY_POLICY.md)
- [Source routing](./docs/SOURCE_ROUTING.md)
- [Gossip synchronization](./docs/sync.md)
- [File transfer](./docs/file_transfer.md)
- [Device management](./docs/device_manager.md)

See [LICENSE.md](./LICENSE.md) for the license included with this source tree.
