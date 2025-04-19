package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.silverpine.uu.core.uuFormatAsRfc3339
import com.silverpine.uu.core.uuFormatAsRfc3339WithMillis
import com.silverpine.uu.core.uuFormatAsRfc3339WithMillisUtc
import com.silverpine.uu.core.uuNanoToRealTime
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuUtf8ByteArray
import com.silverpine.uu.logging.UULog

@SuppressLint("MissingPermission")
class UUSnifferResult(val callbackType: Int, val scanResult: ScanResult)
{
//    val address: String = scanResult.device.address
//    val name: String = scanResult.device.name
//    val txPower: Int = scanResult.txPower
//    val rssi: Int = scanResult.rssi
//    val foo = scanResult.isLegacy

//    init
//    {
//        scanResult.device
//        scanResult.rssi
//        scanResult.txPower
//        scanResult.scanRecord
//        scanResult.isLegacy
//        scanResult.advertisingSid
//        scanResult.dataStatus
//        scanResult.isConnectable
//        scanResult.periodicAdvertisingInterval
//        scanResult.primaryPhy
//        scanResult.secondaryPhy
//        scanResult.timestampNanos
//    }

    val macAddress: String = scanResult.device.address ?: ""
    val name: String = scanResult.device.name ?: ""
    val rssi: Int = scanResult.rssi
    //val periodicAdvertisingInterval: Int = scanResult.periodicAdvertisingInterval
    val timestamp: Long = scanResult.timestampNanos.uuNanoToRealTime() // (scanResult.timestampNanos.toDouble() / 1000000.0).toLong()
    val scanRecord = UUScanRecord(scanResult)
    var timestampDelta: Long = 0

    fun print()
    {
        Log.d("updateAdvertisement",
            "address: $macAddress, " +
                    "name: ${name}, " +
                    //"beaconCount: ${totalBeaconCount}, " +
                    "timestamp: ${timestamp.uuFormatAsRfc3339WithMillis()}, " +
                    "delta: ${timestampDelta}, " +
                    "rssi: $rssi")
    }

    fun csvLine(): ArrayList<String>
    {
        val list: ArrayList<String> = arrayListOf()

        list.add(macAddress)
        list.add(name)
        list.add("$rssi")
        list.add("${scanRecord.records.size}")

        System.nanoTime()
        list.add(timestamp.uuFormatAsRfc3339WithMillis())
        list.add("$timestampDelta")

        scanRecord.records.forEach()
        {
            list.add("${it.dataType} - ${it.data.uuToHex()}")
        }

        return list
    }

    companion object
    {
        fun csvHeader(): ArrayList<String>
        {
            return arrayListOf(
                "mac",
                "name",
                "rssi",
                "scan_record_count",
                "timestamp",
                "timestamp_delta"
            )
        }
    }
}

class UUSnifferSessionSummary
{
    val startTime: Long = System.currentTimeMillis()
    var endTime: Long = 0
        private set

    val results: ArrayList<UUSnifferResult> = arrayListOf()

    private val lastTimes: HashMap<String, Long> = hashMapOf()

    fun end()
    {
        endTime = System.currentTimeMillis()
    }

    fun addResult(callbackType: Int, result: ScanResult)
    {
        synchronized(results)
        {
            val snifferResult = UUSnifferResult(callbackType, result)
            val lastTimestamp = lastTimes.getOrDefault(snifferResult.macAddress, 0)
            snifferResult.timestampDelta = snifferResult.timestamp - lastTimestamp
            lastTimes[snifferResult.macAddress] = snifferResult.timestamp
            snifferResult.print()
            results.add(snifferResult)
        }
    }

    fun calculateTimestampDeltas()
    {
        val times = HashMap<String,Long>()

        results.forEach()
        {
            val lastTime = times[it.macAddress]
            if (lastTime == null)
            {
                it.timestampDelta = 0
            }
            else
            {
                it.timestampDelta = it.timestamp - lastTime
            }

            times[it.macAddress] = it.timestamp
        }
    }

    fun toCsvBytes(): ByteArray?
    {
        calculateTimestampDeltas()

        val header = UUSnifferResult.csvHeader()
        val lines = results.map { it.csvLine() }

        val sb = StringBuilder()
        sb.append(header.joinToString(","))
        sb.append("\n")

        lines.forEach()
        {
            sb.append(it.joinToString(","))
            sb.append("\n")
        }

        return sb.toString().uuUtf8ByteArray()
    }


}

@SuppressLint("MissingPermission")
class UUBluetoothSniffer(context: Context)
{
    private val bluetoothScanner: BluetoothLeScanner
    private var workingSummary = UUSnifferSessionSummary()

    init
    {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothScanner = bluetoothManager.adapter.bluetoothLeScanner
    }

    private val scanCallback = object: ScanCallback()
    {
        override fun onScanResult(callbackType: Int, result: ScanResult?)
        {
            val actualResult = result ?: return

//            if (result.device?.address != "00:17:55:D6:0C:06")
//                return

            workingSummary.addResult(callbackType, actualResult)
        }

        override fun onScanFailed(errorCode: Int)
        {
            UULog.d(javaClass, "onScanFailed", "Scan failed! ErrorCode: $errorCode")
            super.onScanFailed(errorCode)
        }
    }

    fun start()
    {
        val builder = ScanSettings.Builder()
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        builder.setReportDelay(0)
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        builder.setLegacy(false)
        val settings = builder.build()


        val filters = ArrayList<ScanFilter>()
        //filters.add(ScanFilter.Builder().setDeviceAddress("00:17:55:D6:0C:06").build())
        //filters.add(ScanFilter.Builder().setDeviceAddress("00:17:55:D6:0C:06").build())

        workingSummary = UUSnifferSessionSummary()
        bluetoothScanner.startScan(filters, settings, scanCallback)
    }

    fun stop(): UUSnifferSessionSummary
    {
        bluetoothScanner.stopScan(scanCallback)
        workingSummary.end()
        return workingSummary
    }
}