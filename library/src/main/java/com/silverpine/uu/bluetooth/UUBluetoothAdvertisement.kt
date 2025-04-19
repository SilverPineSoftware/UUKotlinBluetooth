package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid

internal class UUBluetoothAdvertisement(
    private val scanResult: ScanResult,
) : UUAdvertisement
{
    val device: BluetoothDevice
        get() = scanResult.device

    override val address: String
        get() = scanResult.device.address ?: ""

    override val rssi: Int
        get() = scanResult.rssi

    override val localName: String?
        get() = scanResult.scanRecord?.deviceName

    override val isConnectable: Boolean
        get() = scanResult.isConnectable

    override val manufacturingData: ByteArray?
        get() = scanResult.scanRecord?.bytes

    override val transmitPower: Int
        get() = scanResult.txPower

    override val primaryPhy: Int
        get() = scanResult.primaryPhy

    override val secondaryPhy: Int
        get() = scanResult.secondaryPhy

    override val timestamp: Long
        get() = scanResult.timestampNanos

    override val services: Array<ParcelUuid>?
        get() = scanResult.scanRecord?.serviceUuids?.toTypedArray()

    override val serviceData: Map<ParcelUuid,ByteArray>?
        get() = scanResult.scanRecord?.serviceData
}