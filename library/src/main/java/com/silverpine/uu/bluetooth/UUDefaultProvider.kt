package com.silverpine.uu.bluetooth

import android.content.Context

class UUDefaultProvider(applicationContext: Context): UUBluetoothProvider
{
    override fun initialize()
    {
        // Do something to prompt for BLE permissions
    }

    override var scanner: UUPeripheralScanner = UUBlePeripheralScanner(applicationContext)

    /*override fun createSession(peripheral: UUPeripheral): UUPeripheralSession
    {
        return UUBluetoothDevicePeripheralSession(peripheral)
    }*/
}
