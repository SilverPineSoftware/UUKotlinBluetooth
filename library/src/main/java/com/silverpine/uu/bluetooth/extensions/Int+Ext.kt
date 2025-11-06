package com.silverpine.uu.bluetooth.extensions

import com.silverpine.uu.bluetooth.UUBluetoothStrings

fun Int.uuCharacteristicPropertiesDescription(): String
{
    return UUBluetoothStrings.CharacteristicProperties.fromBitmask(this)
}