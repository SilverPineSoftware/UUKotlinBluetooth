package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcelable
import android.util.SparseArray
import androidx.core.util.size
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
class UUAdvertisement(
    val address: String = "",
    val rssi: Int = 0,
    val localName: String = "",
    val isConnectable: Boolean = false,
    val manufacturingData: Map<Int,ByteArray>? = null,
    val transmitPower: Int = 0,
    val primaryPhy: Int = 0,
    val secondaryPhy: Int = 0,
    val timestamp: Long = 0,
    val services: List<UUID>? = null,
    val serviceData: Map<UUID,ByteArray>? = null,
    val solicitedServices: List<UUID>? = null
) : Parcelable
{
    @SuppressLint("MissingPermission")
    constructor(scanResult: ScanResult): this(
        address = scanResult.device.address ?: "",
        rssi = scanResult.rssi,
        localName = scanResult.scanRecord?.deviceName ?: "",
        isConnectable = scanResult.isConnectable,
        manufacturingData = scanResult.scanRecord?.manufacturerSpecificData?.uuToByteArrayMap(),
        transmitPower = scanResult.txPower,
        primaryPhy = scanResult.primaryPhy,
        secondaryPhy = scanResult.secondaryPhy,
        timestamp = scanResult.timestampNanos,
        services = scanResult.scanRecord?.serviceUuids?.map { it.uuid },
        serviceData = scanResult.scanRecord?.serviceData?.mapKeys { (parcelUuid, _) -> parcelUuid.uuid },
        solicitedServices = scanResult.uuSolicitedServices()
    )

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true

        if (other !is UUAdvertisement) return false

        if (address != other.address) return false
        if (rssi != other.rssi) return false
        if (localName != other.localName) return false
        if (isConnectable != other.isConnectable) return false
        if (transmitPower != other.transmitPower) return false
        if (primaryPhy != other.primaryPhy) return false
        if (secondaryPhy != other.secondaryPhy) return false
        if (timestamp != other.timestamp) return false

        if (services != other.services) return false
        if (solicitedServices != other.solicitedServices) return false

        // Compare manufacturingData
        if (manufacturingData != null || other.manufacturingData != null)
        {
            if (manufacturingData == null || other.manufacturingData == null) return false

            if (manufacturingData.size != other.manufacturingData.size) return false

            for (entry in manufacturingData)
            {
                val key = entry.key
                val v1 = entry.value
                val v2 = other.manufacturingData.get(key)
                if (v2 == null || !v1.contentEquals(v2)) return false
            }
        }

        // Compare serviceData
        if (serviceData != null || other.serviceData != null)
        {
            if (serviceData == null || other.serviceData == null) return false
            if (serviceData.size != other.serviceData.size) return false

            for ((k, v1) in serviceData)
            {
                val v2 = other.serviceData[k] ?: return false
                if (!v1.contentEquals(v2)) return false
            }
        }

        return true
    }

    override fun hashCode(): Int
    {
        var result = address.hashCode()
        result = 31 * result + rssi
        result = 31 * result + localName.hashCode()
        result = 31 * result + isConnectable.hashCode()
        result = 31 * result + transmitPower
        result = 31 * result + primaryPhy
        result = 31 * result + secondaryPhy
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (services?.hashCode() ?: 0)
        result = 31 * result + (solicitedServices?.hashCode() ?: 0)

        // manufacturingData hash
        result = 31 * result + (manufacturingData?.let()
        {
            var h = 1
            for (entry in manufacturingData)
            {
                val key = entry.key
                val value = entry.value
                h = 31 * h + key
                h = 31 * h + value.contentHashCode()
            }
            h
        } ?: 0)

        // serviceData hash
        result = 31 * result + (serviceData?.let()
        {
            var h = 1
            for ((k, v) in it)
            {
                h = 31 * h + k.hashCode()
                h = 31 * h + v.contentHashCode()
            }
            h
        } ?: 0)

        return result
    }
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

/**
 * Converts this [SparseArray] of [ByteArray] values into a [Map] of key-value pairs.
 *
 * @return a new [Map] containing all key-value pairs from this [SparseArray].
 */
internal fun SparseArray<ByteArray>.uuToByteArrayMap(): Map<Int, ByteArray>
{
    val result = mutableMapOf<Int, ByteArray>()

    for (i in 0 until size)
    {
        val key = keyAt(i)
        val value = valueAt(i)
        result[key] = value
    }

    return result
}
