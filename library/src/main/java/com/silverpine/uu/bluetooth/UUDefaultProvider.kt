package com.silverpine.uu.bluetooth

class UUDefaultProvider: UUBluetoothProvider
{
    override val info: UUBluetoothInfo = UUBuildConfigBluetoothInfo()
    override val scanner: UUPeripheralScanner = UUBlePeripheralScanner()
}