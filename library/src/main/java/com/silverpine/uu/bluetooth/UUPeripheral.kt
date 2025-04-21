package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothSocket
import com.silverpine.uu.bluetooth.internal.UUDataErrorCallback
import com.silverpine.uu.core.UUError

typealias UUPeripheralConnectedBlock = (()->Unit)
typealias UUPeripheralDisconnectedBlock = ((UUError?)->Unit)
typealias UUPeripheralBlock = ((UUPeripheral)->Unit)
typealias UUPeripheralErrorBlock = ((UUPeripheral, UUError?)->Unit)
typealias UUPeripheralCharacteristicErrorBlock = ((UUPeripheral, BluetoothGattCharacteristic, UUError?)->Unit)
typealias UUPeripheralDescriptorErrorBlock = ((UUPeripheral, BluetoothGattDescriptor, UUError?)->Unit)
typealias UUPeripheralIntegerErrorBlock = ((UUPeripheral, Int, UUError?)->Unit)
typealias UUDiscoverServicesCompletionBlock = ((List<BluetoothGattService>?, UUError?)->Unit)
typealias UUDiscoverCharacteristicsCompletionBlock = ((List<BluetoothGattCharacteristic>?, UUError?)->Unit)
typealias UUDiscoverDescriptorsCompletionBlock = ((List<BluetoothGattDescriptor>?, UUError?)->Unit)

interface UUPeripheral
{
    val advertisement: UUAdvertisement
    val rssi: Int
    val firstDiscoveryTime: Long
    val timeSinceLastUpdate: Long
        get() = System.currentTimeMillis() - advertisement.timestamp

    val identifier: String
    val name: String
    val friendlyName: String
    val peripheralState: UUPeripheralConnectionState
    val services: List<BluetoothGattService>?

    fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock)

    fun disconnect(timeout: Long)

    fun discoverServices(
        timeout: Long,
        completion: UUDiscoverServicesCompletionBlock)

//    fun discoverCharacteristics(
//        characteristicUUIDs: List<ParcelUuid>?,
//        service: BluetoothGattService,
//        timeout: Long,
//        completion: UUDiscoverCharacteristicsCompletionBlock)
//
//    fun discoverIncludedServices(
//        includedServiceUUIDs: List<ParcelUuid>?,
//        service: BluetoothGattService,
//        timeout: Long,
//        completion: UUPeripheralErrorBlock)
//
//    fun discoverDescriptorsForCharacteristic(
//        characteristic: BluetoothGattCharacteristic,
//        timeout: Long,
//        completion: UUDiscoverDescriptorsCompletionBlock)
//
//    fun discover(
//        characteristics: List<ParcelUuid>?,
//        serviceUuid: ParcelUuid,
//        timeout: Long,
//        completion: UUDiscoverCharacteristicsCompletionBlock)

    fun setNotifyValue(
        enabled: Boolean,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        notifyHandler: UUPeripheralCharacteristicErrorBlock?,
        completion: UUPeripheralCharacteristicErrorBlock)

    fun read(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUDataErrorCallback)

    fun read(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUDataErrorCallback)

    fun writeValue(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUPeripheralCharacteristicErrorBlock)

    fun writeValueWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUPeripheralCharacteristicErrorBlock)

    fun writeValue(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUPeripheralDescriptorErrorBlock)

    fun readRSSI(
        timeout: Long,
        completion: UUPeripheralIntegerErrorBlock)

    // These need to be internal
    // func openL2CAPChannel(psm: CBL2CAPPSM)

    // func setDidOpenL2ChannelCallback(callback:((CBPeripheral, CBL2CAPChannel?, Error?) -> Void)?)


    fun createL2capChannel(psm: Int): BluetoothSocket?
    fun createInsecureL2capChannel(psm: Int): BluetoothSocket?


//    val bluetoothDevice: BluetoothDevice
//    var bluetoothGatt: BluetoothGatt?
    var negotiatedMtuSize: Int?

    fun updateRssi(updatedRssi: Int)
    {
    }
}
