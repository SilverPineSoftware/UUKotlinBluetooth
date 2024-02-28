package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError

class UUBluetoothResult<T>
{
    var error: UUError? = null
    var success: T? = null
}