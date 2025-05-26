package com.silverpine.uu.bluetooth

interface UUBluetoothProvider
{
    val scanner: UUPeripheralScanner

    fun createSession(peripheral: UUPeripheral): UUPeripheralSession
}
