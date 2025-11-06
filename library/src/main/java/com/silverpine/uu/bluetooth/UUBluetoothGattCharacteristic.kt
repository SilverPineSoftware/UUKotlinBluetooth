package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import com.silverpine.uu.core.uuIsBitSet

val Int.uuCanRead: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_READ)

val Int.uuCanWrite: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_WRITE)

val Int.uuCanWriteWithoutResponse: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

val Int.uuCanToggleNotify: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

val Int.uuCanToggleIndicate: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_INDICATE)

val Int.uuCanBroadcast: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_BROADCAST)

val Int.uuCanWriteSigned: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)

val Int.uuHasExtendedProperties: Boolean
    get() = uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS)
