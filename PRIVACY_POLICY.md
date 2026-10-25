# Frequency privacy notice (draft)

This draft describes the current development build and must be reviewed before
public release. Frequency has not received an independent privacy or security
audit.

## Information used by the app

- Frequency creates a peer identity and may store identity material, app
  settings, favorites, and message data on the device.
- When messaging nearby peers, the app sends protocol packets, which can
  include a nickname, peer identity, and message content, over Bluetooth LE or
  a local network.
- Optional internet-backed features use third-party Nostr relays. Those
  services may observe network metadata such as IP address and connection
  times, and receive the events routed through them. Review the relay
  provider's own privacy terms.
- Android permissions for Bluetooth scanning may require location access.
  Location-based channels, when used, calculate and share a coarse geohash
  with the selected relay network.
- Camera and microphone access is used only when the user invokes QR scanning
  or voice recording features.

## Controls

You can deny optional permissions and choose whether to use internet-backed
features. Removing the app clears its private app storage according to Android
platform behavior. Data already sent to peers or third-party relays may not be
recallable.

## Before public release

The maintainers should verify this notice against the final data flows, add a
privacy contact and effective date, and update it whenever the app's storage,
permissions, relay providers, or telemetry behavior changes.
