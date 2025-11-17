package com.silverpine.uu.bluetooth

interface UUBluetoothProvider
{
    val info: UUBluetoothInfo
    val scanner: UUPeripheralScanner
}