package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.logException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val LOG_TAG = "UUBlePeripheralScanner"

@SuppressLint("MissingPermission")
class UUBlePeripheralScanner : UUPeripheralScanner
{
    val deviceCache: UUBluetoothDeviceCache = UUInMemoryBluetoothDeviceCache

    private val nearbyPeripheralMap: MutableMap<String, UUPeripheral> = mutableMapOf()
    private val nearbyPeripherals = MutableStateFlow<List<UUPeripheral>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)

    private var nearbyPeripheralSubscription: Job? = null

    private val bluetoothLeScanner: BluetoothLeScanner by lazy {
        val bluetoothManager = UUBluetooth.requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter.bluetoothLeScanner
    }

    private var scanCallback: ScanCallback = object: ScanCallback()
    {
        override fun onScanResult(callbackType: Int, result: ScanResult)
        {
            handleScanResult(callbackType, result)
        }

        override fun onBatchScanResults(results: List<ScanResult>)
        {
            // TODO: Is -1 the thing to pass here?
            results.forEach { handleScanResult(-1, it) }
        }

        override fun onScanFailed(errorCode: Int)
        {
            // TODO: Handle scan failed
            UULog.debug(LOG_TAG, "onScanFailed, errorCode: $errorCode")
        }
    }

    override var isScanning: Boolean = false

    override var config: UUPeripheralScannerConfig = UUPeripheralScannerConfig()
    override var peripherals: List<UUPeripheral> = listOf()
    override var started: UUPeripheralScannerStartedCallback = { scanner -> }
    override var ended: UUPeripheralScannerStoppedCallback = { scanner, error -> }
    override var listChanged: UUPeripheralListChangedCallback = { scanner, list -> }

    @OptIn(FlowPreview::class)
    override fun start()
    {
        val error = UUBluetooth.checkPermissions()
        if (error != null)
        {
            notifyScanEnded(error)
            return
        }

        // cancel any existing subscription
        nearbyPeripheralSubscription?.cancel()

        val throttleMillis = config.callbackThrottleMillis
        if (throttleMillis > 0)
        {
            nearbyPeripheralSubscription = nearbyPeripherals
                .sample(throttleMillis.toDuration(DurationUnit.MILLISECONDS))
                .flowOn(Dispatchers.IO)
                .onEach { peripheralList ->
                    withContext(Dispatchers.Main) {
                        notifyNearbyPeripherals(peripheralList)
                    }
                }
                .launchIn(scope)
        }
        else
        {
            nearbyPeripheralSubscription = nearbyPeripherals
                .onEach { peripheralList ->
                    withContext(Dispatchers.Main) {
                        notifyNearbyPeripherals(peripheralList)
                    }
                }
                .launchIn(scope)
        }

        isScanning = true
        val scanFilters = config.buildUuidFilters()
        val scanSettings = config.buildScanSettings()

        notifyScanStarted()
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
    }

    override fun stop()
    {
        isScanning = false

        nearbyPeripheralSubscription?.cancel()
        nearbyPeripheralSubscription = null

        bluetoothLeScanner.stopScan(scanCallback)

        notifyScanEnded(null)
    }

    /*
    @OptIn(FlowPreview::class)
    override fun startScan(
        settings: UUBluetoothScanSettings,
        callback: (List<UUPeripheral>) -> Unit
    ) {
        this.scanSettings = settings
        this.nearbyPeripheralCallback = callback
        clearNearbyPeripherals()
    }*/

    override fun getPeripheral(identifier: String): UUPeripheral?
    {
        synchronized(nearbyPeripheralMap)
        {
            return nearbyPeripheralMap[identifier]
        }
    }

    private fun clearNearbyPeripherals()
    {
        synchronized(nearbyPeripheralMap) {
            nearbyPeripheralMap.clear()
            nearbyPeripherals.value = emptyList()
        }
    }

    private fun notifyScanStarted()
    {
        started(this)
    }

    private fun notifyScanEnded(error: UUError?)
    {
        ended(this, error)
    }

    private fun handleScanResult(callbackType: Int, scanResult: ScanResult)
    {
        try
        {
            if (!isScanning)
            {
                //debugLog("handleScanResult", "Not scanning, ignoring advertisement from " + scanResult.getDevice().getAddress());
                return
            }

            val advertisement = UUAdvertisement(scanResult)
            deviceCache[advertisement.address] = scanResult.device
            handleAdvertisement(advertisement)
        }
        catch (ex: Exception)
        {
            UULog.logException(LOG_TAG, "handleScanResult", ex)
        }
    }

    private fun handleAdvertisement(advertisement: UUAdvertisement)
    {
        if (advertisement.address.isEmpty())
        {
            UULog.warn(LOG_TAG, "handleAdvertisement, Throwing out advertisement with empty address")
            return
        }

        synchronized(nearbyPeripheralMap)
        {
            val peripheral = nearbyPeripheralMap[advertisement.address] ?: UUPeripheral()
            peripheral.advertisement = advertisement
            peripheral.refreshConnectionState(false)

            nearbyPeripheralMap[advertisement.address] = peripheral
            nearbyPeripherals.value = nearbyPeripheralMap.values.filter { shouldDiscoverPeripheral(it) }
        }
    }

    private fun shouldDiscoverPeripheral(peripheral: UUPeripheral): Boolean
    {
        val filters = config.discoveryFilters ?: return true
        return filters.all { it.shouldDiscover(peripheral) }
    }

    private fun notifyNearbyPeripherals(list: List<UUPeripheral>)
    {
        val sorted = config.peripheralSorting?.let()
        { comparator ->
            list.sortedWith(comparator)
        } ?: list

        peripherals = sorted
        listChanged(this, sorted)
    }
}