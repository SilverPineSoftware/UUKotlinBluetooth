package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.bluetooth.internal.uuHashLookup
import java.util.Locale

internal enum class PeripheralTimerBucket
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

internal fun UUPeripheral.timerId(bucket: PeripheralTimerBucket): String
{
    return String.format(Locale.US, "%s__%s", identifier, bucket.name)
}

internal fun BluetoothDevice.uuCharacteristicTimerId(characteristic: BluetoothGattCharacteristic, bucket: PeripheralTimerBucket): String
{
    return String.format(
        Locale.US,
        "%s__ch_%s__%s",
        address,
        characteristic.uuHashLookup(),
        bucket.name
    )
}

internal fun BluetoothDevice.uuDescriptorTimerId(descriptor: BluetoothGattDescriptor, bucket: PeripheralTimerBucket): String
{
    return String.format(
        Locale.US,
        "%s__de_%s__%s",
        address,
        descriptor.uuHashLookup(),
        bucket.name
    )
}

internal val UUPeripheral.connectWatchdogTimerId: String
    get() = timerId(PeripheralTimerBucket.Connect)

internal val UUPeripheral.disconnectWatchdogTimerId: String
    get() = timerId(PeripheralTimerBucket.Disconnect)

internal val UUPeripheral.serviceDiscoveryWatchdogTimerId: String
    get() = timerId(PeripheralTimerBucket.ServiceDiscovery)


/*
private fun setNotifyStateWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return formatCharacteristicTimerId(characteristic,
        UUBluetoothGatt.CHARACTERISTIC_NOTIFY_STATE_WATCHDOG_BUCKET
    )
}

private fun readCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return formatCharacteristicTimerId(characteristic,
        UUBluetoothGatt.READ_CHARACTERISTIC_WATCHDOG_BUCKET
    )
}

private fun readDescritporWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
{
    return formatDescriptorTimerId(descriptor, UUBluetoothGatt.READ_DESCRIPTOR_WATCHDOG_BUCKET)
}

private fun writeCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
{
    return formatCharacteristicTimerId(characteristic,
        UUBluetoothGatt.WRITE_CHARACTERISTIC_WATCHDOG_BUCKET
    )
}

private fun writeDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
{
    return formatDescriptorTimerId(descriptor, UUBluetoothGatt.WRITE_DESCRIPTOR_WATCHDOG_BUCKET)
}

private fun readRssiWatchdogTimerId(): String
{
    return formatPeripheralTimerId(UUBluetoothGatt.READ_RSSI_WATCHDOG_BUCKET)
}

private fun requestMtuWatchdogTimerId(): String
{
    return formatPeripheralTimerId(UUBluetoothGatt.REQUEST_MTU_WATCHDOG_BUCKET)
}

private fun pollRssiTimerId(): String
{
    return formatPeripheralTimerId(UUBluetoothGatt.POLL_RSSI_BUCKET)
}

*/