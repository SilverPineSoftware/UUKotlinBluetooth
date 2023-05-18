package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice

interface UUPeripheralFactory<T : UUPeripheral?>
{
    fun createPeripheral(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?): T
}