package com.silverpine.uu.bluetooth.extensions

import com.silverpine.uu.bluetooth.UUBluetoothStrings

val Int.uuCharacteristicPropertiesString: String
    get() = UUBluetoothStrings.CharacteristicProperties.fromBitmask(this)

val Int.uuGattStatusString: String
    get() = UUBluetoothStrings.GattStatuses.from(this)

val Int.uuConnectionStateString: String
    get() = UUBluetoothStrings.ConnectionStates.from(this)

val Int.uuDeviceTypeString: String
    get() = UUBluetoothStrings.DeviceTypes.from(this)

val Int.uuBondStateString: String
    get() = UUBluetoothStrings.BondStates.from(this)

val Int.uuPowerStateString: String
    get() = UUBluetoothStrings.PowerStates.from(this)