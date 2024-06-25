package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
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
    val periodicAdvertisingInterval: Int = scanResult.periodicAdvertisingInterval
    val timestamp: Long = (scanResult.timestampNanos.toDouble() / 1000000.0).toLong()
    //val foo = scanResult.scanRecord?.advertisingDataMap.get()
    val scanRecord = UUScanRecord(scanResult)
    var timestampDelta: Long = 0

    fun csvLine(): ArrayList<String>
    {
        val list: ArrayList<String> = arrayListOf()

        list.add(macAddress)
        list.add(name)
        list.add("$rssi")
        list.add("${scanRecord.records.size}")
        list.add("$periodicAdvertisingInterval")
        list.add("$timestamp")
        list.add("$timestampDelta")

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
                "periodic_advertising_interval",
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

    fun end()
    {
        endTime = System.currentTimeMillis()
    }

    fun addResult(callbackType: Int, result: ScanResult)
    {
        synchronized(results)
        {
            results.add(UUSnifferResult(callbackType, result))
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
        val settings = builder.build()


        val filters = ArrayList<ScanFilter>()
//        filters.add(ScanFilter.Builder()
//            .setDeviceAddress("00:00:00:00:00:00").build())

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