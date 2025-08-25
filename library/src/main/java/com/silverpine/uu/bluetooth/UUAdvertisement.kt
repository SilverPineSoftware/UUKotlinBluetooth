package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.os.Build
import android.util.SparseArray
import java.util.UUID

class UUAdvertisement(
    val address: String = "",
    val rssi: Int = 0,
    val localName: String = "",
    val isConnectable: Boolean = false,
    val manufacturingData: SparseArray<ByteArray>? = null,
    val transmitPower: Int = 0,
    val primaryPhy: Int = 0,
    val secondaryPhy: Int = 0,
    val timestamp: Long = 0,
    val services: List<UUID>? = null,
    val serviceData: Map<UUID,ByteArray>? = null,
    val solicitedServices: List<UUID>? = null
)
{
    @SuppressLint("MissingPermission")
    constructor(scanResult: ScanResult): this(
        address = scanResult.device.address ?: "",
        rssi = scanResult.rssi,
        localName = scanResult.scanRecord?.deviceName ?: "",
        isConnectable = scanResult.isConnectable,
        manufacturingData = scanResult.scanRecord?.manufacturerSpecificData,
        transmitPower = scanResult.txPower,
        primaryPhy = scanResult.primaryPhy,
        secondaryPhy = scanResult.secondaryPhy,
        timestamp = scanResult.timestampNanos,
        services = scanResult.scanRecord?.serviceUuids?.map { it.uuid },
        serviceData = scanResult.scanRecord?.serviceData?.mapKeys { (parcelUuid, _) -> parcelUuid.uuid },
        solicitedServices = scanResult.uuSolicitedServices()
    )
}

internal fun ScanResult.uuSolicitedServices(): List<UUID>?
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
    {
        scanRecord?.serviceSolicitationUuids?.map { it.uuid }
    }
    else
    {
        null
    }
}
