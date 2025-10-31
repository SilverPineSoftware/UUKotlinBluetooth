package com.silverpine.uu.bluetooth

class UUDefaultProvider: UUBluetoothProvider
{
//    override fun initialize()
//    {
//        // Do something to prompt for BLE permissions
//    }

    override var scanner: UUPeripheralScanner = UUBlePeripheralScanner()

    /*override fun createSession(peripheral: UUPeripheral): UUPeripheralSession
    {
        return UUBluetoothDevicePeripheralSession(peripheral)
    }*/
}
