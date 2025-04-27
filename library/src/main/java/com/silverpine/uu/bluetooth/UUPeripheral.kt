package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothSocket
import com.silverpine.uu.core.UUError

typealias UUPeripheralConnectedBlock = (()->Unit)
typealias UUPeripheralDisconnectedBlock = ((UUError?)->Unit)
typealias UUDiscoverServicesCompletionBlock = ((List<BluetoothGattService>?, UUError?)->Unit)
typealias UUIntIntCallback = (Int, Int)->Unit
typealias UUErrorCallback = (UUError?)->Unit
typealias UUServiceListCallback = (List<BluetoothGattService>?, UUError?)->Unit
typealias UUDataErrorCallback = (ByteArray?, UUError?)->Unit
typealias UUIntErrorCallback = (Int?, UUError?)->Unit
typealias UUIntIntErrorCallback = (Int?, Int?, UUError?)->Unit
typealias UUCharacteristicDataCallback = ((BluetoothGattCharacteristic, ByteArray?)->Unit)
typealias UUCharacteristicErrorCallback = ((BluetoothGattCharacteristic, UUError?)->Unit)

interface UUPeripheral
{
    val advertisement: UUAdvertisement
    val rssi: Int
    val firstDiscoveryTime: Long
    val timeSinceLastUpdate: Long
        get() = System.currentTimeMillis() - advertisement.timestamp

    val identifier: String
    val name: String
    val peripheralState: UUPeripheralConnectionState
    val services: List<BluetoothGattService>?

    var mtuSize: Int
    var txPhy: Int?
    var rxPhy: Int?

    fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock)

    fun disconnect(timeout: Long)

    fun discoverServices(
        timeout: Long,
        completion: UUDiscoverServicesCompletionBlock)

    fun setNotifyValue(
        enabled: Boolean,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        notifyHandler: UUCharacteristicDataCallback?,
        completion: UUCharacteristicErrorCallback)

    fun read(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUDataErrorCallback)

    fun read(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUDataErrorCallback)

    fun write(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUErrorCallback)

    fun writeWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUErrorCallback)

    fun write(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUErrorCallback)

    fun readRSSI(
        timeout: Long,
        completion: UUIntErrorCallback)

    fun requestMtu(
        mtu: Int,
        timeout: Long,
        completion: UUIntErrorCallback)

    fun readPhy(
        timeout: Long,
        completion: UUIntIntErrorCallback)

    fun updatePhy(
        txPhy: Int,
        rxPhy: Int,
        phyOptions: Int,
        timeout: Long,
        completion: UUIntIntErrorCallback)

    // These need to be internal
    // func openL2CAPChannel(psm: CBL2CAPPSM)

    // func setDidOpenL2ChannelCallback(callback:((CBPeripheral, CBL2CAPChannel?, Error?) -> Void)?)


    fun createL2capChannel(psm: Int): BluetoothSocket?
    fun createInsecureL2capChannel(psm: Int): BluetoothSocket?


}
