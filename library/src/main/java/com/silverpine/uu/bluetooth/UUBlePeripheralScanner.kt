package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.silverpine.uu.logging.UULog
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

@SuppressLint("MissingPermission")
class UUBlePeripheralScanner(context: Context) : UUPeripheralScanner
{
    val deviceCache: UUBluetoothDeviceCache = UUInMemoryBluetoothDeviceCache

    private val nearbyPeripheralMap: MutableMap<String, UUPeripheral> = mutableMapOf()
    //private var scanSettings = UUBluetoothScanSettings()
    //private var nearbyPeripheralCallback: (List<UUPeripheral>) -> Unit = {}
    private val nearbyPeripherals = MutableStateFlow<List<UUPeripheral>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.IO)


    private var nearbyPeripheralSubscription: Job? = null

    private val bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner
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
            UULog.d(javaClass, "onScanFailed", "errorCode: $errorCode")
        }
    }

    init
    {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
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

    private fun notifyScanEnded(error: Error?)
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
            UULog.d(javaClass, "handleScanResult", "", ex)
        }
    }

    private fun handleAdvertisement(advertisement: UUAdvertisement)
    {
        if (advertisement.address.isEmpty())
        {
            UULog.w(javaClass, "handleAdvertisement", "Throwing out advertisement with empty address")
            return
        }

        synchronized(nearbyPeripheralMap)
        {
            val existing = nearbyPeripheralMap[advertisement.address] ?: UUPeripheral(advertisement)

            //existing.update(advertisement)

            nearbyPeripheralMap[advertisement.address] = existing
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