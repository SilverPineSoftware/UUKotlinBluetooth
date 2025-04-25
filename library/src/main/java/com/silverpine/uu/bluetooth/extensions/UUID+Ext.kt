package com.silverpine.uu.bluetooth.extensions

import com.silverpine.uu.bluetooth.UUBluetooth
import java.util.UUID

val UUID.uuCommonName: String
    get() = UUBluetooth.bluetoothSpecName(this)