package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattService
import com.silverpine.uu.core.UUError

//interface UUDiscoverServicesDelegate
//{
//    fun onCompleted(services: ArrayList<BluetoothGattService?>?, error: UUError?)
//}

typealias UUDiscoverServicesDelegate = (ArrayList<BluetoothGattService>?,UUError?)->Unit