# Frequency Android - Agent Guide

## Project

Frequency is a native Android peer-to-peer messaging app. The Android
application uses Kotlin and Jetpack Compose; the Java migration is incremental.
The root app package is `com.frequency.android`, with Gradle configuration in
`app/build.gradle.kts`.

## Architecture

- `ui/`: Compose screens, ViewModels, and presentation state.
- `service/`: Android foreground service and app lifecycle integration.
- `mesh/`: BLE mesh, peer management, packet processing, and routing.
- `protocol/`, `model/`, `crypto/`, `noise/`: packet formats and cryptography.
- `transport/`: Java `PacketTransport` API, transport coordinator, and LAN TCP
  discovery/delivery.
- `nostr/`: optional internet relay functionality.

`BluetoothMeshService` connects packet transports to the existing mesh packet
processor. Received packets must continue through its validation and routing
path. BLE is the required fallback; LAN discovery depends on multicast support.
Direct cellular peer connectivity is not implemented.

## Development

- Keep Android APIs and transport I/O off the UI thread.
- Preserve packet wire compatibility and signature/replay validation.
- Check Android runtime permissions and foreground-service restrictions when
  changing hardware or background behavior.
- Put JVM unit tests in `app/src/test/`; use device tests for BLE and LAN
  behavior that cannot be validated on a regular JVM.
- Windows build: `.\gradlew.bat assembleDebug`
- Unit tests: `.\gradlew.bat testDebugUnitTest`
- Lint: `.\gradlew.bat lintDebug`

The app has not received an independent security review; do not describe it as
audited or production-secure.
