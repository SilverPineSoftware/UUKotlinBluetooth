package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.util.SparseArray
import androidx.core.util.forEach
import com.silverpine.uu.bluetooth.UUAdvertisement
import com.silverpine.uu.bluetooth.UUScanRecord
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.util.UUID

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

    override val localName: String
        get() = scanResult.scanRecord?.deviceName ?: ""

    override val isConnectable: Boolean
        get() = scanResult.isConnectable

    override val manufacturingData: SparseArray<ByteArray>?
        get() = scanResult.scanRecord?.manufacturerSpecificData

    override val transmitPower: Int
        get() = scanResult.txPower

    override val primaryPhy: Int
        get() = scanResult.primaryPhy

    override val secondaryPhy: Int
        get() = scanResult.secondaryPhy

    override val timestamp: Long
        get() = scanResult.timestampNanos

    override val services: List<UUID>?
        get() = scanResult.scanRecord?.serviceUuids?.map { it.uuid }

    override val serviceData: Map<UUID,ByteArray>?
        get() = scanResult.scanRecord?.serviceData?.mapKeys { (parcelUuid, _) -> parcelUuid.uuid }

    override val solicitedServices: List<UUID>?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            scanResult.scanRecord?.serviceSolicitationUuids?.map { it.uuid }
        }
        else
        {
            null
        }


    init
    {
        val scanRecord = UUScanRecord(scanResult)
        scanRecord.records.forEach()
        {
            UULog.d(javaClass, "init", "$address, dataType: ${it.dataType}, data: ${it.data.uuToHex()}")

        }

        manufacturingData?.forEach()
        { companyId, data ->
            UULog.d(javaClass, "init", "$address, companyId: $companyId, data: ${data.uuToHex()}")
        }

    }
}