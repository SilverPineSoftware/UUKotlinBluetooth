package com.silverpine.uu.bluetooth.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetooth.connectionStateToString
import com.silverpine.uu.bluetooth.UUBluetooth.gattStatusToString
import com.silverpine.uu.bluetooth.UUBluetoothConstants
import com.silverpine.uu.bluetooth.UUBluetoothError
import com.silverpine.uu.bluetooth.UUBluetoothErrorCode
import com.silverpine.uu.bluetooth.UUDiscoverServicesCompletionBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
import com.silverpine.uu.bluetooth.UUPeripheralDisconnectedBlock
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.io.Closeable
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
internal class UUBluetoothGatt(private val bluetoothDevice: BluetoothDevice): Closeable
{
    companion object
    {
        private val gattHashMap = ConcurrentHashMap<String, UUBluetoothGatt>()

        fun get(device: BluetoothDevice): UUBluetoothGatt
        {
            return gattHashMap.computeIfAbsent(device.address)
            {
                UUBluetoothGatt(device)
            }
        }
    }

    internal val rootTimerId: String = bluetoothDevice.address

    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothGattCallback: UUBluetoothGattCallback = UUBluetoothGattCallback()

    private var disconnectError: UUError? = null
    //    private val readCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val writeCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val characteristicChangedDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val setNotifyDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val readDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
//    private val writeDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
    //private var disconnectTimeout: Long = 0

    val isConnecting: Boolean
        get() = (bluetoothGatt != null && isConnectWatchdogActive)

    private val isConnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(connectWatchdogTimerId) != null)

    private var disconnectTimeout: Long = 10000L

    private var disconnectedCallback: UUPeripheralDisconnectedBlock? = null

    fun getPeripheralState(): UUPeripheralConnectionState
    {
        val bluetoothManager = UUBluetooth.requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var state = bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT)
        //debugLog("getConnectionState", "Actual connection state is: $state (${ConnectionState.fromProfileConnectionState(state)})")
        val gatt = bluetoothGatt

        if (gatt != null)
        {
            if ((state != BluetoothProfile.STATE_CONNECTING) && isConnecting)
            {
                debugLog("getConnectionState", "Forcing state to connecting")
                state = BluetoothProfile.STATE_CONNECTING
            }
            else if (state != BluetoothProfile.STATE_DISCONNECTED && bluetoothGatt == null)
            {
                debugLog("getConnectionState", "Forcing state to disconnected")
                state = BluetoothProfile.STATE_DISCONNECTED
            }
        }

        return UUPeripheralConnectionState.fromProfileConnectionState(state)
    }

    fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock)
    {
        if (bluetoothGatt != null)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt is already connected to ${bluetoothGatt?.device?.address}")
            disconnected.safeNotify(UUBluetoothError.alreadyConnectedError())
            return
        }

        if (isConnectWatchdogActive)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt making connection attempt to ${bluetoothGatt?.device?.address}")
            disconnected.safeNotify(UUBluetoothError.alreadyConnectedError())
            return
        }

        val timerId = connectWatchdogTimerId

        disconnectedCallback =
        { error ->
            debugLog("connect", "Disconnected from: $bluetoothDevice, error: $error")
            notifyDisconnection(disconnected, error)
        }

        bluetoothGattCallback.connectionStateChangedCallback =
        { status, newState ->
            debugLog(
                "onConnectionStateChanged", String.format(
                    Locale.US, "status: %s, newState: %s (%d)",
                    statusLog(status), connectionStateToString(newState), newState))

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED)
            {
                debugLog("connect", "Connected to: $bluetoothDevice")
                UUTimer.cancelActiveTimer(timerId)
                disconnectError = null
                connected.safeNotify()
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED)
            {
                var err = disconnectError
                if (err == null)
                {
                    err = UUBluetoothError.gattStatusError("onConnectionStateChanged", status)
                }

                if (err == null)
                {
                    err = UUBluetoothError.disconnectedError()
                }

                // Special case - If an operation has finished with a success error code, then don't
                // pass it up to the caller.
                if (err.code == UUBluetoothErrorCode.Success.rawValue)
                {
                    err = null
                }

                notifyDisconnected(err)
            }
            else if (status == UUBluetoothConstants.GATT_ERROR)
            {
                // Sometimes when attempting a connection, the operation fails with status 133 and state
                // other than connected.  Through trial and error, calling BluetoothGatt.connect() after
                // this happens will make the connection happen.
                reconnectGatt()
            }
        }

        startTimeoutWatchdog(timerId, timeout)

        val connectGattAutoFlag = false

        uuDispatchMain()
        {
            debugLog("connect", "Connecting to: $bluetoothDevice, gattAuto: $connectGattAutoFlag")

            disconnectError = UUBluetoothError.connectionFailedError()

            val context = UUBluetooth.requireApplicationContext()

            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                connectGattAutoFlag,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    fun discoverServices(
        timeout: Long,
        completion: UUDiscoverServicesCompletionBlock)
    {
        val timerId = serviceDiscoveryWatchdogTimerId

        bluetoothGattCallback.servicesDiscoveredCallback =
        { services, error ->
            debugLog("discoverServices", "Service Discovery complete: $bluetoothDevice, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            completion.safeNotify(services, error)
        }

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("discoverServices", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyServicesDiscovered(null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("discoverServices", "Discovering services for: $bluetoothDevice")
            val ok = gatt.discoverServices()
            debugLog("discoverServices", "returnCode: $ok")

            if (!ok)
            {
                bluetoothGattCallback.notifyServicesDiscovered(null, UUBluetoothError.operationFailedError("discoverServices"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun readCharacteristic(
        serviceUuid: UUID,
        uuid: UUID,
        timeout: Long,
        completion: UUDataErrorCallback)
    {
        val timerId = readCharacteristicWatchdogTimerId(uuid)

        val callback: UUDataErrorCallback =
        { data: ByteArray?,
          error: UUError? ->
            debugLog(
                "readCharacteristic",
                "Read characteristic complete: $bluetoothDevice, error: $error, data: ${data?.uuToHex()}")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.clearReadCharacteristicCallback(uuid)
            completion.safeNotify(data, error)
        }

        bluetoothGattCallback.registerReadCharacteristicCallback(uuid, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("readCharacteristic", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicRead(uuid, null, UUBluetoothError.notConnectedError())
            return
        }

        val characteristic = gatt.getService(serviceUuid).characteristics.firstOrNull { it.uuid == uuid }

        if (characteristic == null)
        {
            debugLog("readCharacteristic", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicRead(uuid, null, UUBluetoothError.missingRequiredCharacteristic(uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("readCharacteristic", "characteristic: " + characteristic.uuid)
            val success = gatt.readCharacteristic(characteristic)
            debugLog("readCharacteristic", "readCharacteristic returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyCharacteristicRead(uuid, null, UUBluetoothError.operationFailedError("readCharacteristic"))
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Closeable Implementation
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun close()
    {
        bluetoothGatt?.uuSafeClose()
        bluetoothGatt = null
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startTimeoutWatchdog(timerId: String, timeout: Long)
    {
        UUTimer.startTimer(timerId, timeout, null)
        { _, _ ->
            disconnect(UUBluetoothError.timeoutError())
        }
    }
    private fun reconnectGatt()
    {
        try
        {
            bluetoothGatt?.let()
            {
                val success = it.connect()
                debugLog("reconnectGatt", "connect() returned $success")
            }
        }
        catch (ex: Exception)
        {
            logException("reconnectGatt", ex)
        }
    }

    private fun notifyDisconnection(callback: UUErrorCallback, error: UUError?)
    {
        cancelAllTimers()
        cleanupAfterDisconnect()

        bluetoothGatt?.uuSafeClose()
        bluetoothGatt = null

        callback.safeNotify(error)
        disconnectedCallback = null
    }

    private fun notifyDisconnected(error: UUError?)
    {
        disconnectedCallback?.let()
        {
            notifyDisconnection(it, error)
        }
    }

    /*private fun notifyDisconnected(error: UUError?)
    {
        //      closeGatt()
        cancelAllTimers()

        bluetoothGatt?.uuSafeClose()
        bluetoothGatt = null
//        peripheral.setBluetoothGatt(null)
//        val delegate = connectionDelegate
//        connectionDelegate = null
//        notifyDisconnectDelegate(delegate, error)
        val block = disconnectedCallback
        disconnectedCallback = null
        block?.safeNotify(error)

        cleanupAfterDisconnect()
    }*/

    fun disconnect(error: UUError?)
    {
        disconnectError = error

        if (disconnectError == null)
        {
            disconnectError = UUBluetoothError.success()
        }

        val timerId = disconnectWatchdogTimerId

        UUTimer.startTimer(timerId, disconnectTimeout, null)
        { _, _ ->

            debugLog("disconnect", "Disconnect timeout: $bluetoothDevice")
            notifyDisconnected(error)

            // Just in case the timeout fires and a real disconnect is needed, this is the last
            // ditch effort to close the connection
            disconnectGattOnMainThread()
        }

        disconnectGattOnMainThread()
    }

    private fun disconnectGattOnMainThread()
    {
        uuDispatchMain()
        {
            disconnectGatt()
        }
    }

    private fun disconnectGatt()
    {
        try
        {
            debugLog("disconnectGatt", "Disconnecting from: $bluetoothDevice, bluetoothGatt: $bluetoothGatt")
            bluetoothGatt?.disconnect()
        }
        catch (ex: Exception)
        {
            logException("disconnectGatt", ex)
        }
    }

    private fun cleanupAfterDisconnect()
    {
        cancelAllTimers()
        clearDelegates()
    }

    private fun cancelAllTimers()
    {
        try
        {
            val list: ArrayList<UUTimer> = UUTimer.listActiveTimers()
            val prefix = rootTimerId
//            if (prefix != null)
//            {
            for (t: UUTimer in list)
            {
                if (t.timerId.startsWith(prefix))
                {
                    debugLog("cancelAllTimers", "Cancelling peripheral timer: " + t.timerId)
                    t.cancel()
                }
                else
                {
                    debugLog("cancelAllTimers", "Leaving timer alone: " + t.timerId)
                }
            }
            //}
        }
        catch (ex: Exception)
        {
            logException("cancelAllTimers", ex)
        }
    }

    private fun clearDelegates()
    {
        // connectionCallback = null
        disconnectedCallback = null

        bluetoothGattCallback.clearAll()

//        connectionDelegate = null
//        serviceDiscoveryDelegate = null
//        readRssiDelegate = null
//        requestMtuDelegate = null
//        pollRssiDelegate = null
//        readCharacteristicDelegates.clear()
//        writeCharacteristicDelegates.clear()
//        characteristicChangedDelegates.clear()
//        setNotifyDelegates.clear()
//        readDescriptorDelegates.clear()
//        writeDescriptorDelegates.clear()
    }



    private fun statusLog(status: Int): String
    {
        return String.format(Locale.US, "%s (%d)", gattStatusToString(status), status)
    }

    private fun debugLog(method: String, message: String)
    {
//        if (LOGGING_ENABLED)
//        {
        UULog.d(javaClass, method, message)
        //}
    }

    private fun logException(method: String, exception: Throwable)
    {
//        if (LOGGING_ENABLED)
//        {
        UULog.d(javaClass, method, "", exception)
        //}
    }
}