package com.silverpine.uu.bluetooth

typealias UUPeripheralListChangedCallback = (UUPeripheralScanner, List<UUPeripheral>) -> Unit
typealias UUPeripheralScannerStartedCallback = (UUPeripheralScanner) -> Unit
typealias UUPeripheralScannerStoppedCallback = (UUPeripheralScanner, Error?) -> Unit

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








//
//
//internal object UUBluetoothObjects
//{
//    val defaultScanner: UUPeripheralScanner by lazy { UUBlePeripheralScanner(requireApplicationContext()) }
//}
//
//val UUBluetooth.defaultScanner: UUPeripheralScanner
//    get()
//    {
//        return UUBluetoothObjects.defaultScanner
//    }
