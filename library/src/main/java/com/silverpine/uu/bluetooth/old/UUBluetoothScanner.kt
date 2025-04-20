package com.silverpine.uu.bluetooth.old
/*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuFormatAsRfc3339WithMillis
import com.silverpine.uu.core.uuFormatDate
import com.silverpine.uu.core.uuNanoToRealTime
import com.silverpine.uu.logging.UULog
import java.util.UUID

@SuppressLint("MissingPermission")
class UUBluetoothScanner<T : UUPeripheral>(context: Context, factory: UUPeripheralFactory<T>)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG

        private const val outOfRangeFilterEvaluationFrequencyTimerId = "UUBluetoothScanner_outOfRangeFilterEvaluationFrequency"
        private const val SCAN_RESTART_TIMER_ID = "UUBluetoothScanner_scanRestartTimerId"

        // Starting with Android N the OS has a limitation where you cannot start a BLE scan more
        // than five times within 30 seconds.
        private const val SCAN_LIMIT_MILLIS = 30000
        private const val SCAN_LIMIT_COUNT = 5
    }

    private val bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    var isScanning = false
        private set

    private var scanFilters: ArrayList<UUPeripheralFilter<T>> = arrayListOf()
    private var outOfRangeScanFilters: ArrayList<UUOutOfRangePeripheralFilter<T>> = arrayListOf()
    private val peripheralFactory: UUPeripheralFactory<T>
    private val ignoredDevices = HashMap<String, Boolean>()
    private val nearbyPeripherals = HashMap<String, T>()
    private var nearbyPeripheralCallback: ((ArrayList<T>)->Unit)? = null
    var outOfRangeFilterEvaluationFrequency: Long = 500
    var allowExtendedAdvertisements: Boolean = true

    var scanDelayedCallback: (Long)->Unit = { }

    private var scanCheckStartTime: Long = 0
    private var scanCheckCount = 0

    fun startScanning(
        serviceUuidList: ArrayList<UUID>?,
        filters: ArrayList<UUPeripheralFilter<T>>?,
        outOfRangeFilters: ArrayList<UUOutOfRangePeripheralFilter<T>>?,
        callback: ((ArrayList<T>)->Unit)
    )
    {
        scanFilters.clear()
        filters?.let()
        {
            scanFilters.addAll(it)
        }

        outOfRangeScanFilters.clear()
        outOfRangeFilters?.let()
        {
            outOfRangeScanFilters.addAll(it)
        }

        isScanning = true
        clearIgnoredDevices()
        nearbyPeripheralCallback = callback

        uuDispatchMain()
        {
            startScan(serviceUuidList)
        }
    }

    @Synchronized
    private fun clearIgnoredDevices()
    {
        ignoredDevices.clear()
    }

    fun stopScanning()
    {
        isScanning = false

        stopAllTimers()

        uuDispatchMain()
        {
            stopScan()
        }
    }

    private fun restartScanAfterDelay(delay: Long, serviceUuidList: ArrayList<UUID>?)
    {
        stopScanRestartTimer()

        UUTimer.startTimer(SCAN_RESTART_TIMER_ID, delay, null)
        { _,_ ->
            debugLog("restartScanAfterDelay", "Scan is being restarted after delay")
            startScan(serviceUuidList)
        }
    }

    private fun startScan(serviceUuidList: ArrayList<UUID>?)
    {
        stopScan()

        try
        {
            if (scanCheckCount == 0)
            {
                scanCheckStartTime = System.currentTimeMillis()
                scanCheckCount = 1
            }
            else
            {
                val timeSinceLastScan = System.currentTimeMillis() - scanCheckStartTime
                if (timeSinceLastScan < SCAN_LIMIT_MILLIS)
                {
                    ++scanCheckCount
                }
                else
                {
                    scanCheckStartTime = System.currentTimeMillis()
                    scanCheckCount = 1
                }
            }

            if (scanCheckCount >= SCAN_LIMIT_COUNT)
            {
                val timeLeft = scanCheckStartTime + SCAN_LIMIT_MILLIS - System.currentTimeMillis() + 1000

                // Notify app of error/warning condition

                debugLog("startScan", "Scanning too frequently, scheduling scan restart in $timeLeft millis")
                restartScanAfterDelay(timeLeft, serviceUuidList)

                uuDispatch()
                {
                    scanDelayedCallback(timeLeft)
                }

                return
            }

            val filters = ArrayList<ScanFilter>()
            serviceUuidList?.forEach()
            { uuid ->
                val fb = ScanFilter.Builder()
                fb.setServiceUuid(ParcelUuid(uuid))
                filters.add(fb.build())
            }

            val builder = ScanSettings.Builder()
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            builder.setReportDelay(0)
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            builder.setLegacy(!allowExtendedAdvertisements)
            val settings = builder.build()
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

            if (scanCallback == null)
            {
                scanCallback = object : ScanCallback()
                {
                    override fun onScanResult(callbackType: Int, result: ScanResult)
                    {
//                        debugLog("startScan.onScanResult",
//                            "callbackType: $callbackType, " +
//                                    "timestamp: ${result.timestampNanos.uuNanoToRealTime().uuFormatAsRfc3339WithMillis()}, " +
//                                    "rssi: ${result.rssi}, " +
//                                    "name: ${result.device?.name}, " +
//                                    "address: ${result.device?.address}, " +
//                                    "result: $result")

                        handleScanResult(result)
                    }

                    /**
                     * Callback when batch results are delivered.
                     *
                     * @param results List of scan results that are previously scanned.
                     */
                    override fun onBatchScanResults(results: List<ScanResult>)
                    {
                        debugLog("startScan.onBatchScanResults", "There are " + results.size + " batched results")

                        for (sr in results)
                        {
                            debugLog("startScan.onBatchScanResults", results.toString())
                            handleScanResult(sr)
                        }
                    }

                    /**
                     * Callback when scan could not be started.
                     *
                     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
                     */
                    override fun onScanFailed(errorCode: Int)
                    {
                        debugLog("startScan.onScanFailed", "errorCode: $errorCode")
                    }
                }
            }

            bluetoothLeScanner!!.startScan(filters, settings, scanCallback)
            startOutOfRangeEvaluationTimer()
        }
        catch (ex: Exception)
        {
            debugLog("startScan", ex)
        }
    }

    private fun handleScanResult(scanResult: ScanResult)
    {
        try
        {
            if (!isScanning)
            {
                //debugLog("handleScanResult", "Not scanning, ignoring advertisement from " + scanResult.getDevice().getAddress());
                return
            }

            if (isIgnored(scanResult))
            {
                //debugLog("handleScanResult", "Ignoring advertisement from " + scanResult.getDevice().getAddress());
                return
            }

            val address = scanResult.device.address ?: return

            var peripheral: T?
            synchronized(nearbyPeripherals)
            {
                peripheral = nearbyPeripherals[address]
            }

            if (peripheral == null)
            {
                peripheral = peripheralFactory.createPeripheral(scanResult.device)
            }

            peripheral?.let()
            { p ->

                p.updateAdvertisement(scanResult.rssi, scanResult.timestampNanos.uuNanoToRealTime(), safeGetScanRecord(scanResult))

                if (shouldDiscoverPeripheral(p))
                {
                    synchronized(nearbyPeripherals)
                    {
                        nearbyPeripherals[address] = p
                    }

                    uuDispatch()
                    {
                        val sorted = sortedPeripherals()
                        nearbyPeripheralCallback?.invoke(sorted)
                    }
                }
            }
        }
        catch (ex: Exception)
        {
            debugLog("handleScanResult", ex)
        }
    }

    private fun safeGetScanRecord(result: ScanResult?): ByteArray?
    {
        if (result != null)
        {
            val sr = result.scanRecord
            if (sr != null)
            {
                return sr.bytes
            }
        }

        return null
    }

    private fun sortedPeripherals(): ArrayList<T>
    {
        val list: ArrayList<T>
        synchronized(nearbyPeripherals)
        {
            list = ArrayList(nearbyPeripherals.values)
        }

        list.sortWith()
        { lhs: T, rhs: T ->

            val lhsRssi = lhs.rssi
            val rhsRssi = rhs.rssi

            if (lhsRssi > rhsRssi)
            {
                return@sortWith -1
            }
            else if (lhsRssi < rhsRssi)
            {
                return@sortWith 1
            }
            0
        }
        return list
    }

    private fun stopScan()
    {
        try
        {
            if (bluetoothLeScanner != null && scanCallback != null)
            {
                bluetoothLeScanner!!.stopScan(scanCallback)
            }
        }
        catch (ex: Exception)
        {
            debugLog("stopScan", ex)
        }
    }

    private fun safeEndAllScanning()
    {
        stopScan()
    }

    @Synchronized
    private fun isIgnored(device: BluetoothDevice?): Boolean
    {
        return device == null || ignoredDevices.containsKey(device.address)
    }

    private fun isIgnored(scanResult: ScanResult?): Boolean
    {
        return scanResult == null || isIgnored(scanResult.device)
    }

    @Synchronized
    fun ignoreDevice(device: BluetoothDevice)
    {
        ignoredDevices[device.address] = java.lang.Boolean.TRUE
    }

    @Synchronized
    fun clearIgnoreList()
    {
        ignoredDevices.clear()
    }

    private fun shouldDiscoverPeripheral(peripheral: T?): Boolean
    {
        if (peripheral == null)
        {
            return false
        }

        for (filter in scanFilters)
        {
            val result = filter.shouldDiscoverPeripheral(peripheral)
            if (result == UUPeripheralFilter.Result.IgnoreForever)
            {
                ignoreDevice(peripheral.bluetoothDevice)
                return false
            }

            if (result == UUPeripheralFilter.Result.IgnoreOnce)
            {
                return false
            }
        }

        return true
    }

    init
    {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        peripheralFactory = factory
    }

    private fun startOutOfRangeEvaluationTimer()
    {
        stopOutOfRangeEvaluationTimer()
        val t = UUTimer(
            outOfRangeFilterEvaluationFrequencyTimerId,
            outOfRangeFilterEvaluationFrequency,
            true,
            null
        )
        { _, _ ->

            synchronized(nearbyPeripherals)
            {
                var didChange = false
                val keep = ArrayList<T>()

                for (peripheral in nearbyPeripherals.values)
                {
                    var outOfRange = false
                    for (filter in outOfRangeScanFilters)
                    {
                        if (filter.checkPeripheralRange(peripheral) == UUOutOfRangePeripheralFilter.Result.OutOfRange)
                        {
                            outOfRange = true
                            didChange = true
                            break
                        }
                    }

                    if (!outOfRange)
                    {
                        keep.add(peripheral)
                    }
                }

                nearbyPeripherals.clear()

                for (peripheral in keep)
                {
                    nearbyPeripherals[peripheral.address!!] = peripheral
                }

                if (didChange)
                {
                    val sorted = sortedPeripherals()
                    nearbyPeripheralCallback?.invoke(sorted)
                }
            }
        }

        t.start()
    }

    private fun stopOutOfRangeEvaluationTimer()
    {
        UUTimer.cancelActiveTimer(outOfRangeFilterEvaluationFrequencyTimerId)
    }

    private fun stopScanRestartTimer()
    {
        UUTimer.cancelActiveTimer(SCAN_RESTART_TIMER_ID)
    }

    private fun stopAllTimers()
    {
        stopOutOfRangeEvaluationTimer()
        stopScanRestartTimer()
    }

    private fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, message)
        }
    }

    @Synchronized
    private fun debugLog(method: String, exception: Throwable)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, "", exception)
        }
    }
}*/