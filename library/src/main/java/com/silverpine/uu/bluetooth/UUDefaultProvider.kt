package com.silverpine.uu.bluetooth

class UUDefaultProvider: UUBluetoothProvider
{
    override val scanner: UUPeripheralScanner = UUBlePeripheralScanner()
}