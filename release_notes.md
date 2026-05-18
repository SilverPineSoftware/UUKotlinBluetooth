# UUKotlinBluetooth

Bluetooth Low Energy utilities for Silverpine UU Android apps: scanning, peripherals, GATT, L2CAP, advertising, and structured Bluetooth errors.

## Maven coordinates

| Artifact | Coordinates |
|----------|-------------|
| Bluetooth | `com.silverpine.uu:uu-bluetooth-ktx` |

Published to [Maven Central](https://central.sonatype.com/search?q=com.silverpine.uu) under the `com.silverpine.uu` group.

## What's included

### Core BLE

- **`UUBluetooth`** — shared constants, defaults (timeouts, MTU), and framework helpers.
- **`UUPeripheral`**, **`UUPeripheralSession`** — connect, discover services, read/write characteristics.
- **`UUBluetoothGattCallback`**, **`UUBluetoothGattCache`** — GATT event handling and caching.
- **`UUPeripheralScanner`**, **`UUBlePeripheralScanner`** — scan configuration, filters, and results.
- **`UUAdvertisement`**, **`UUBluetoothAdvertiser`** — peripheral advertising.

### State and diagnostics

- **`UUBluetoothState`**, **`UUBluetoothStateWatcher`** — adapter on/off and permission-aware state.
- **`UUBluetoothSniffer`** — debug/trace support for BLE traffic.
- **`UUBluetoothError`** / **`UUBluetoothErrorCode`** — typed errors integrated with `UUError`.

### Advanced

- **`UUL2CapClient`**, **`UUL2CapServer`**, **`UUL2CapChannel`** — L2CAP channel support where available.
- **Representation models** — export/import peripheral structure (`UUPeripheralRepresentation`, services, characteristics).
- **Operations** — `UUExportPeripheralOperation` and related operation types.

Depends on **`uu-core-ktx`** for errors, dates, hex encoding, and shared utilities.

## Gradle dependency

```kotlin
dependencies {
    implementation("com.silverpine.uu:uu-bluetooth-ktx:<version>")
    implementation("com.silverpine.uu:uu-core-ktx:<version>")
}
```

## Requirements

- Android Bluetooth permissions (`BLUETOOTH`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` as required by target SDK)
- Physical device or emulator with BLE support for instrumented tests
- UU Kotlin build catalog (`uu_build`) and GitHub Packages credentials for `UUKotlinBuild`

## Changes in this release

- Structured **`UUBluetoothError`** factory and code mapping aligned with UU Core error patterns.
- Peripheral session and GATT callback coverage in unit tests.
- Managed-device instrumented test workflows in CI.
- Dokka-generated API documentation published to Maven Central.

---

For prior versions and snapshots, see [GitHub Releases](https://github.com/SilverpineSoftware/UUKotlinBluetooth/releases).
