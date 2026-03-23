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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
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

    private val bluetoothStateWatcher: UUBluetoothStateWatcher by lazy {
        UUBluetoothStateWatcher(UUBluetooth.requireApplicationContext())
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
            UULog.debug(LOG_TAG, "onScanFailed, errorCode: $errorCode")
            endScan(UUBluetoothError.scanFailedError(errorCode))
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
        UULog.debug(LOG_TAG, "Start Scan")

        var error = UUBluetooth.checkPermissions()
        if (error != null)
        {
            UULog.debug(LOG_TAG, "Unable to start scan, permissions check failed, error: $error")
            endScan(error)
            return
        }

        error = UUBluetooth.checkBluetoothState()
        if (error != null)
        {
            UULog.debug(LOG_TAG, "Unable to start scan, bluetooth state check failed, error: $error")
            endScan(error)
            return
        }

        clearNearbyPeripherals()

        // cancel any existing subscription
        nearbyPeripheralSubscription?.cancel()

        val peripheralNotifications = combine(
            nearbyPeripherals,
            bluetoothStateWatcher.stateFlow,
        ) { peripheralList, btState ->
            Pair(peripheralList, btState)
        }

        val throttleMillis = config.callbackThrottleMillis
        if (throttleMillis > 0)
        {
            nearbyPeripheralSubscription = peripheralNotifications
                .sample(throttleMillis.toDuration(DurationUnit.MILLISECONDS))
                .flowOn(Dispatchers.IO)
                .onEach { pair ->
                    notifyNearbyPeripherals(pair.first, pair.second)
                }
                .launchIn(scope)
        }
        else
        {
            nearbyPeripheralSubscription = peripheralNotifications
                .onEach { pair ->
                    notifyNearbyPeripherals(pair.first, pair.second)
                }
                .launchIn(scope)
        }

        isScanning = true
        val scanFilters = config.buildUuidFilters()
        val scanSettings = config.buildScanSettings()

        bluetoothStateWatcher.start()

        notifyScanStarted()
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
    }

    override fun stop()
    {
        UULog.debug(LOG_TAG, "Stopping scan")
        endScan(null)
    }

    private fun endScan(error: UUError? = null)
    {
        isScanning = false

        nearbyPeripheralSubscription?.cancel()
        nearbyPeripheralSubscription = null

        bluetoothLeScanner.stopScan(scanCallback)
        bluetoothStateWatcher.stop()

        notifyScanEnded(error)
    }

    override fun getPeripheral(identifier: String): UUPeripheral?
    {
        synchronized(nearbyPeripheralMap)
        {
            return nearbyPeripheralMap[identifier]
        }
    }

    private fun clearNearbyPeripherals()
    {
        synchronized(nearbyPeripheralMap)
        {
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
                UULog.verbose(LOG_TAG, "Not scanning, ignoring advertisement from ${scanResult.device.address}")
                return
            }

            val watcherState = bluetoothStateWatcher.currentState

            if (watcherState != UUBluetoothState.ON)
            {
                UULog.verbose(LOG_TAG, "BLE is not on, ignoring advertisement from ${scanResult.device.address}")
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

        //UULog.verbose(LOG_TAG, "handleAdvertisement: $advertisement")

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

    private fun notifyNearbyPeripherals(list: List<UUPeripheral>, state: UUBluetoothState)
    {
        if (state == UUBluetoothState.TURNING_OFF || state == UUBluetoothState.OFF)
        {
            UULog.debug(LOG_TAG, "notifyNearbyPeripherals, Unable to continue scan, bluetooth state is off or turning off, state: $state")
            endScan(UUBluetoothError.makeError(UUBluetoothErrorCode.BluetoothDisabled))
            return
        }

        val sorted = config.peripheralSorting?.let()
            { comparator ->
                list.sortedWith(comparator)
            } ?: list

        peripherals = sorted
        listChanged(this, sorted)
    }
}