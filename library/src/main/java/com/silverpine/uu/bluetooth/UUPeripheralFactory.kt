package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice

interface UUPeripheralFactory<T : UUPeripheral>
{
    fun createPeripheral(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?): T
}

class UUDefaultPeripheralFactory: UUPeripheralFactory<UUPeripheral>
{
    override fun createPeripheral(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?): UUPeripheral
    {
        return UUPeripheral(device, rssi, scanRecord)
    }
}