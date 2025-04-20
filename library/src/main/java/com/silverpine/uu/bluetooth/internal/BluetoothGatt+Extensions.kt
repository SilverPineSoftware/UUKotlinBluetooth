package com.silverpine.uu.bluetooth.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.silverpine.uu.logging.UULog

@SuppressLint("MissingPermission")
internal fun BluetoothGatt.uuSafeClose()
{
    try
    {
        close()

//        val gatt = bluetoothGatt
//        if (gatt != null)
//        {
//            ++__GATT_CLOSE_CALLS
//            gatt.close()
//        }
    }
    catch (ex: Exception)
    {
        // logException("closeGatt", ex)
        UULog.d(javaClass, "uuSafeClose", "", ex)
    }
//    finally
//    {
//        bluetoothGatt = null
//        logGattInfo("closeGatt-finally")
//    }
}