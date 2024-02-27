package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattService
import com.silverpine.uu.core.UUError

internal typealias UUIntIntCallback = (Int, Int)->Unit
internal typealias UUVoidCallback = ()->Unit
internal typealias UUErrorCallback = (UUError?)->Unit
internal typealias UUServiceListCallback = (List<BluetoothGattService>?, UUError?)->Unit
internal typealias UUDataErrorCallback = (ByteArray?, UUError?)->Unit
internal typealias UUDataCallback = (ByteArray?)->Unit
internal typealias UUIntErrorCallback = (Int?, UUError?)->Unit
internal typealias UUIntIntErrorCallback = (Int?, Int?, UUError?)->Unit
