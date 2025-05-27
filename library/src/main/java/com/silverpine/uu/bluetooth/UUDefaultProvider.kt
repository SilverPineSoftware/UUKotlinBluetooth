package com.silverpine.uu.bluetooth

import android.content.Context
import com.silverpine.uu.bluetooth.UUBlePeripheralScanner
import com.silverpine.uu.bluetooth.internal.UUBluetoothDevicePeripheralSession

class UUDefaultProvider(applicationContext: Context): UUBluetoothProvider
{
    override var scanner: UUPeripheralScanner = UUBlePeripheralScanner(applicationContext)

    override fun createSession(peripheral: UUPeripheral): UUPeripheralSession
    {
        return UUBluetoothDevicePeripheralSession(peripheral)
    }
}
