package com.silverpine.uu.bluetooth

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.silverpine.uu.core.UUDate

/**
 * Contains BLE scanning settings.
 */
data class UUBluetoothScanSettings(

    var scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY,
    var callbackType: Int = ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
    var matchMode: Int = ScanSettings.MATCH_MODE_STICKY,
    var numMatches: Int = ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT,
    var reportDelay: Long = 0,
    var legacyOnly: Boolean = false,
    var phy: Int? = null,

    /**
     * List of BLE service UUIDs to scan for.
     */
    var serviceUUIDs: List<ParcelUuid>? = null,

    /**
     * Optional filters (e.g. by RSSI or manufacturer data) that determine
     * when a peripheral enters/exits the “nearby” list.
     */
    var discoveryFilters: List<UUPeripheralFilter>? = null,

    /**
     * Time interval in seconds to throttle scan callbacks.
     */
    var callbackThrottle: Double = 0.5,

    /**
     * Comparator used to sort discovered peripherals.
     * (e.g. by RSSI, discovery time, friendly name, etc.)
     */
    var peripheralSorting: Comparator<UUPeripheral>? = null
)

internal val UUBluetoothScanSettings.callbackThrottleMillis: Long
    get() = callbackThrottle.times(UUDate.Constants.millisInOneSecond).toLong()

internal fun UUBluetoothScanSettings.buildUuidFilters(): List<ScanFilter>?
{
    return serviceUUIDs?.map { ScanFilter.Builder().setServiceUuid(it).build() }
}

internal fun UUBluetoothScanSettings.buildScanSettings(): ScanSettings
{
    val builder = ScanSettings.Builder()
    builder.setCallbackType(callbackType)
    builder.setMatchMode(matchMode)
    builder.setNumOfMatches(numMatches)
    builder.setReportDelay(reportDelay)
    builder.setScanMode(scanMode)
    builder.setLegacy(legacyOnly)

    // The phy option is only valid when legacy is false
    if (!legacyOnly)
    {
        phy?.let { builder.setPhy(it) }
    }

    val settings = builder.build()
    return settings
}