package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError

typealias UUPeripheralListChangedCallback = (UUPeripheralScanner, List<UUPeripheral>) -> Unit
typealias UUPeripheralScannerStartedCallback = (UUPeripheralScanner) -> Unit
typealias UUPeripheralScannerStoppedCallback = (UUPeripheralScanner, UUError?) -> Unit

interface UUPeripheralScanner
{
    val isScanning: Boolean
    var config: UUPeripheralScannerConfig
    val peripherals: List<UUPeripheral>

    var started: UUPeripheralScannerStartedCallback
    var ended: UUPeripheralScannerStoppedCallback
    var listChanged: UUPeripheralListChangedCallback

    fun start()
    fun stop()

    fun getPeripheral(identifier: String): UUPeripheral?
}
