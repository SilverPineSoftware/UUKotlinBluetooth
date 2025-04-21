package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGattCharacteristic
import java.util.Locale
import java.util.UUID

internal enum class BluetoothGattTimerBucket
{
    Connect,
    ServiceDiscovery,
    CharacteristicNotifyState,
    ReadCharacteristic,
    WriteCharacteristic,
    ReadDescriptor,
    WriteDescriptor,
    ReadRssi,
    PollRssi,
    Disconnect,
    RequestMtu,
}

internal fun UUBluetoothGatt.timerId(bucket: BluetoothGattTimerBucket): String
{
    return String.format(Locale.US, "%s__%s", rootTimerId, bucket.name)
}

internal fun UUBluetoothGatt.timerId(uuid: UUID, bucket: BluetoothGattTimerBucket): String
{
    return String.format(Locale.US, "%s__%s__%s", rootTimerId, uuid.uuToLowercaseString(), bucket.name)
}
/*
internal fun UUBluetoothGatt.characteristicTimerId(characteristic: BluetoothGattCharacteristic, bucket: BluetoothGattTimerBucket): String
{
    return String.format(
        Locale.US,
        "%s__ch_%s__%s",
        rootTimerId,
        characteristic.uuHashLookup(),
        bucket.name
    )
}

internal fun UUBluetoothGatt.descriptorTimerId(descriptor: BluetoothGattDescriptor, bucket: BluetoothGattTimerBucket): String
{
    return String.format(
        Locale.US,
        "%s__de_%s__%s",
        rootTimerId,
        descriptor.uuHashLookup(),
        bucket.name
    )
}*/

internal val UUBluetoothGatt.connectWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.Connect)

internal val UUBluetoothGatt.disconnectWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.Disconnect)

internal val UUBluetoothGatt.serviceDiscoveryWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.ServiceDiscovery)

internal fun UUBluetoothGatt.setNotifyStateWatchdogTimerId(uuid: UUID): String
{
    return timerId(uuid, BluetoothGattTimerBucket.CharacteristicNotifyState)
}

internal fun UUBluetoothGatt.readCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return timerId(characteristic.uuid, BluetoothGattTimerBucket.ReadCharacteristic)
}

internal fun UUBluetoothGatt.readDescriptorWatchdogTimerId(uuid: UUID): String
{
    return timerId(uuid, BluetoothGattTimerBucket.ReadDescriptor)
}

internal fun UUBluetoothGatt.writeCharacteristicWatchdogTimerId(uuid: UUID): String
{
    return timerId(uuid, BluetoothGattTimerBucket.WriteCharacteristic)
}

internal fun UUBluetoothGatt.writeDescriptorWatchdogTimerId(uuid: UUID): String
{
    return timerId(uuid, BluetoothGattTimerBucket.WriteDescriptor)
}

internal val UUBluetoothGatt.readRssiWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.ReadRssi)

internal val UUBluetoothGatt.requestMtuWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.RequestMtu)

internal val UUBluetoothGatt.pollRssiTimerId: String
    get() = timerId(BluetoothGattTimerBucket.PollRssi)
