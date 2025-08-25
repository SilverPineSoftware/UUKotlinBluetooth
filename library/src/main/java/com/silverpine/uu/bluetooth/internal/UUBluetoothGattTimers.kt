package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.bluetooth.UUPeripheral
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
    Disconnect,
    RequestMtu,
    ReadPhy,
    UpdatePhy
}

internal fun UUPeripheral.timerId(bucket: BluetoothGattTimerBucket): String
{
    return String.format(Locale.US, "%s__%s", rootTimerId, bucket.name)
}

internal fun UUPeripheral.timerId(uuid: UUID, bucket: BluetoothGattTimerBucket): String
{
    return String.format(Locale.US, "%s__%s__%s", rootTimerId, uuid.uuToLowercaseString(), bucket.name)
}

internal val UUPeripheral.connectWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.Connect)

internal val UUPeripheral.disconnectWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.Disconnect)

internal val UUPeripheral.serviceDiscoveryWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.ServiceDiscovery)

internal fun UUPeripheral.setNotifyStateWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return timerId(characteristic.uuid, BluetoothGattTimerBucket.CharacteristicNotifyState)
}

internal fun UUPeripheral.readCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return timerId(characteristic.uuid, BluetoothGattTimerBucket.ReadCharacteristic)
}

internal fun UUPeripheral.readDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
{
    return timerId(descriptor.uuid, BluetoothGattTimerBucket.ReadDescriptor)
}

internal fun UUPeripheral.writeCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return timerId(characteristic.uuid, BluetoothGattTimerBucket.WriteCharacteristic)
}

internal fun UUPeripheral.writeDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
{
    return timerId(descriptor.uuid, BluetoothGattTimerBucket.WriteDescriptor)
}

internal val UUPeripheral.readRssiWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.ReadRssi)

internal val UUPeripheral.requestMtuWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.RequestMtu)

internal val UUPeripheral.readPhyWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.ReadPhy)

internal val UUPeripheral.updatePhyWatchdogTimerId: String
    get() = timerId(BluetoothGattTimerBucket.UpdatePhy)
