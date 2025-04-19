package com.silverpine.uu.bluetooth

interface UUPeripheralScanner
{
    val isScanning: Boolean

    fun startScan(settings: UUBluetoothScanSettings, callback: (List<UUPeripheral>)->Unit)
    fun stopScan()
}


val UUBluetooth.defaultScanner: UUPeripheralScanner
    get() = UUBlePeripheralScanner(requireApplicationContext())


/*
public extension UUCoreBluetooth
{
    static var defaultScanner: UUPeripheralScanner
    {
        return UUCoreBluetoothBleScanner()
    }
}*/