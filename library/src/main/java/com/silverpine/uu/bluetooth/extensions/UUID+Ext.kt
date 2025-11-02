package com.silverpine.uu.bluetooth.extensions

import com.silverpine.uu.bluetooth.UUBluetooth
import java.util.UUID

val UUID.uuCommonName: String
    get() = UUBluetooth.bluetoothSpecName(this)

/**
 * Returns true if this UUID matches the Bluetooth 16-bit short code pattern:
 * 0000????-0000-1000-8000-00805F9B34FB
 */
val UUID.uuIsBluetoothShortCode: Boolean
    get()
    {
        val regex = Regex("^0000[0-9A-Fa-f]{4}-0000-1000-8000-00805F9B34FB$")
        return regex.matches(this.toString().uppercase())
    }