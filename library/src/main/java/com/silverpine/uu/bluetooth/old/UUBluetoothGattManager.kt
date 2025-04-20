package com.silverpine.uu.bluetooth.old

/*
import com.silverpine.uu.core.uuIsNotEmpty
import com.silverpine.uu.logging.UULog

internal object UUBluetoothGattManager
{
    private val gattHashMap = HashMap<String?, UUBluetoothGatt>()

    fun gattForPeripheral(peripheral: UUPeripheral): UUBluetoothGatt?
    {
        val ctx = UUBluetooth.requireApplicationContext()
        var gatt: UUBluetoothGatt? = null
        val address = peripheral.address

        if (address.uuIsNotEmpty())
        {
            if (gattHashMap.containsKey(address))
            {
                gatt = gattHashMap[address]
                //UULog.d(javaClass, "gattForPeripheral", "Found existing gatt for $address")
            }

            if (gatt == null)
            {
                gatt = UUBluetoothGatt(ctx, peripheral)
                //UULog.d(javaClass, "gattForPeripheral", "Creating new gatt for $address")
                gattHashMap[address] = gatt
            }
        }

        return gatt
    }
}

*/