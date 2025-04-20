package com.silverpine.uu.sample.bluetooth.ui

import android.bluetooth.BluetoothGattService
import android.content.Intent
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.ux.uuRequireString

fun Intent.uuRequireService(peripheral: UUPeripheral, key: String): BluetoothGattService
{
    val serviceUUid = uuRequireString("serviceUuid")

    // TODO: Implement this
    throw throw RuntimeException("Unable to get discovered service with UUID $serviceUUid")
//    return peripheral.getDiscoveredService(serviceUUid)
//        ?: throw RuntimeException("Unable to get discovered service with UUID $serviceUUid")
}