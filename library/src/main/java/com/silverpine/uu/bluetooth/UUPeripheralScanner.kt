package com.silverpine.uu.bluetooth

import com.silverpine.uu.bluetooth.UUBluetooth.requireApplicationContext
import com.silverpine.uu.bluetooth.internal.UUBlePeripheralScanner

interface UUPeripheralScanner
{
    val isScanning: Boolean

    fun startScan(settings: UUBluetoothScanSettings, callback: (List<UUPeripheral>)->Unit)
    fun stopScan()


    fun getPeripheral(identifier: String): UUPeripheral?
}

internal object UUBluetoothObjects
{
    val defaultScanner: UUPeripheralScanner by lazy { UUBlePeripheralScanner(requireApplicationContext()) }
}

val UUBluetooth.defaultScanner: UUPeripheralScanner
    get()
    {
        return UUBluetoothObjects.defaultScanner
    }
