package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.core.uuSafeToString
import java.util.Locale
import java.util.UUID


internal fun BluetoothGattCharacteristic?.uuSafeUuidString(): String
{
    return this?.uuid.uuToLowercaseString()
}

internal fun BluetoothGattDescriptor?.uuSafeUuidString(): String
{
    return this?.uuid.uuToLowercaseString()
}

internal fun UUID?.uuToLowercaseString(): String
{
    return this?.uuSafeToString()?.lowercase(Locale.getDefault()) ?: ""
}

internal fun BluetoothGattCharacteristic?.uuHashLookup(): String
{
    return uuSafeUuidString()
}

internal fun BluetoothGattDescriptor?.uuHashLookup(): String
{
    return "${this?.characteristic?.uuSafeUuidString()}-${this?.uuSafeUuidString()}"
}






internal fun BluetoothDevice.uuTimerId(bucket: String): String
{
    return String.format(Locale.US, "%s__%s", address, bucket)
}

internal fun BluetoothDevice.uuCharacteristicTimerId(characteristic: BluetoothGattCharacteristic, bucket: String): String
{
    return String.format(
        Locale.US,
        "%s__ch_%s__%s",
        address,
        characteristic.uuHashLookup(),
        bucket
    )
}

internal fun BluetoothDevice.uuDescriptorTimerId(descriptor: BluetoothGattDescriptor, bucket: String): String
{
    return String.format(
        Locale.US,
        "%s__de_%s__%s",
        address,
        descriptor.uuHashLookup(),
        bucket
    )
}
