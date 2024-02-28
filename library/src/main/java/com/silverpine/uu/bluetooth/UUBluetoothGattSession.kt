package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.Job
import java.io.Closeable
import java.util.Locale

@SuppressLint("MissingPermission")
class UUBluetoothGattSession(private val context: Context, private val bluetoothDevice: BluetoothDevice): Closeable
{
    companion object
    {
        const val DEFAULT_TIMEOUT = 60 * UUDate.MILLIS_IN_ONE_SECOND
        const val DEFAULT_DISCONNECT_TIMEOUT = 10 * UUDate.MILLIS_IN_ONE_SECOND
    }

    private val gattCallback = UUBluetoothGattCallback()
    private var bluetoothGatt: BluetoothGatt? = null

    private val isConnecting: Boolean
        get() = (bluetoothGatt != null && isConnectWatchdogActive)

    private val isDisconnecting: Boolean
        get() = (bluetoothGatt != null && isDisconnectWatchdogActive)

    private val isConnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(connectWatchdogTimerId()) != null)

    private val isDisconnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(disconnectWatchdogTimerId()) != null)

    private var activeJob: Job? = null

    var connectTimeout: Long = DEFAULT_TIMEOUT
    //var disconnectTimeout: Long = DEFAULT_DISCONNECT_TIMEOUT
    var gattAutoConnect: Boolean = false

    var sessionEnded: UUErrorCallback? = null
    private var sessionEndError: UUError? = null

    suspend fun connectionState(): UUConnectionState
    {
        val job = Job()
        var result = UUConnectionState.Undetermined

        uuDispatch()
        {
            val bluetoothManager = UUBluetooth.requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bleState = bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT)
            debugLog("getConnectionState", "Actual connection state is: $bleState (${UUConnectionState.fromProfileConnectionState(bleState)})")
            val gatt = bluetoothGatt

            result = UUConnectionState.fromProfileConnectionState(bleState)

            if (gatt != null)
            {
                if ((bleState != BluetoothProfile.STATE_CONNECTING) && isConnecting)
                {
                    debugLog("getConnectionState", "Forcing state to connecting")
                    result = UUConnectionState.Connected
                }
                else if (bleState != BluetoothProfile.STATE_DISCONNECTING && isDisconnecting)
                {
                    debugLog("getConnectionState", "Forcing state to disconnected")
                    result = UUConnectionState.Disconnected
                }
            }

            job.complete()
        }

        job.join()

        return result
    }

    suspend fun connect(): UUError?
    {
        val method = "connect"
        val timerId = connectWatchdogTimerId()

        if (activeJob != null)
        {
            debugLog(method, "Another job is active ${bluetoothGatt?.device?.address}")
            return UUBluetoothError.preconditionFailedError("session busy")
        }

        if (bluetoothGatt != null)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt is already connected to ${bluetoothGatt?.device?.address}")
            return UUBluetoothError.alreadyConnectedError()
        }

        if (isConnectWatchdogActive)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt making connection attempt to ${bluetoothGatt?.device?.address}")
            return UUBluetoothError.alreadyConnectedError()
        }

        val job = Job()
        var error: UUError? = null

        uuDispatchMain()
        {
            gattCallback.connectionStateChangedCallback =
            { gattStatus, connectionState ->

                if (gattStatus != BluetoothGatt.GATT_SUCCESS)
                {
                    error = UUBluetoothError.gattStatusError("connect", gattStatus)
                }
                else if (connectionState != BluetoothGatt.STATE_CONNECTED)
                {
                    error = UUBluetoothError.disconnectedError()
                }

                gattCallback.connectionStateChangedCallback = null
                job.complete()
            }

            UUTimer.startTimer(timerId, connectTimeout, null)
            { _, _ ->
                debugLog(method, "Connect timeout: $bluetoothDevice")
                error = UUBluetoothError.timeoutError()
                bluetoothGatt?.disconnect()
            }

            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                gattAutoConnect,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        activeJob = job

        job.join()

        gattCallback.connectionStateChangedCallback = null
        UUTimer.cancelActiveTimer(timerId)

        activeJob = null
        return error
    }

    suspend fun disconnect()
    {
        val job = Job()

        uuDispatchMain()
        {
            disconnectGatt()
            closeGatt()
            bluetoothGatt = null
            job.complete()
        }

        job.join()
    }

    suspend fun discoverServices(timeout: Long = DEFAULT_TIMEOUT): UUBluetoothResult<List<BluetoothGattService>>
    {
        val method = "discoverServices"
        val timerId = bluetoothDevice.uuTimerId(method)

        val result = UUBluetoothResult<List<BluetoothGattService>>()

        if (activeJob != null)
        {
            debugLog(method, "Another job is active ${bluetoothGatt?.device?.address}")
            result.error = UUBluetoothError.preconditionFailedError("session busy")
            return result
        }

        val gatt = bluetoothGatt
        if (gatt == null)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt is null")
            result.error = UUBluetoothError.notConnectedError()
            return result
        }

        val job = Job()

        uuDispatchMain()
        {
            gattCallback.connectionStateChangedCallback =
            { gattStatus, connectionState ->

                if (gattStatus != BluetoothGatt.GATT_SUCCESS)
                {
                    result.error = UUBluetoothError.gattStatusError(method, gattStatus)
                }
                else if (connectionState != BluetoothGatt.STATE_CONNECTED)
                {
                    result.error = UUBluetoothError.disconnectedError()
                }

                job.complete()
            }

            gattCallback.servicesDiscoveredCallback =
            { services, error ->
                result.error = error
                result.success = services
                job.complete()
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog(method, "$method timeout: $bluetoothDevice")
                result.error = UUBluetoothError.timeoutError()
                gattCallback.connectionStateChangedCallback = null
                bluetoothGatt?.disconnect()
            }

            val ok = gatt.discoverServices()
            if (!ok)
            {
                result.error = UUBluetoothError.operationFailedError(method)
                job.complete()
            }
        }

        activeJob = job

        job.join()

        gattCallback.connectionStateChangedCallback = null
        gattCallback.servicesDiscoveredCallback = null
        UUTimer.cancelActiveTimer(timerId)
        activeJob = null

        return result
    }

    suspend fun readCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long = DEFAULT_TIMEOUT): UUBluetoothResult<ByteArray>
    {
        val method = "readCharacteristic"
        val timerId = bluetoothDevice.uuCharacteristicTimerId(characteristic, method)

        val result = UUBluetoothResult<ByteArray>()

        if (activeJob != null)
        {
            debugLog(method, "Another job is active ${bluetoothGatt?.device?.address}")
            result.error = UUBluetoothError.preconditionFailedError("session busy")
            return result
        }

        val gatt = bluetoothGatt
        if (gatt == null)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt is null")
            result.error = UUBluetoothError.notConnectedError()
            return result
        }

        val job = Job()

        uuDispatchMain()
        {
            gattCallback.connectionStateChangedCallback =
            { gattStatus, connectionState ->

                if (gattStatus != BluetoothGatt.GATT_SUCCESS)
                {
                    result.error = UUBluetoothError.gattStatusError(method, gattStatus)
                }
                else if (connectionState != BluetoothGatt.STATE_CONNECTED)
                {
                    result.error = UUBluetoothError.disconnectedError()
                }

                job.complete()
            }

            gattCallback.registerReadCharacteristicCallback(characteristic)
            { data, error ->
                result.error = error
                result.success = data
                job.complete()
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog(method, "$method timeout: $bluetoothDevice")
                result.error = UUBluetoothError.timeoutError()
                gattCallback.connectionStateChangedCallback = null
                bluetoothGatt?.disconnect()
            }

            val ok = gatt.readCharacteristic(characteristic)
            if (!ok)
            {
                result.error = UUBluetoothError.operationFailedError(method)
                job.complete()
            }
        }

        activeJob = job

        job.join()

        gattCallback.connectionStateChangedCallback = null
        gattCallback.clearReadCharacteristicCallback(characteristic)
        UUTimer.cancelActiveTimer(timerId)

        activeJob = null

        return result
    }

    suspend fun readDescriptor(
        descriptor: BluetoothGattDescriptor,
        timeout: Long = DEFAULT_TIMEOUT): UUBluetoothResult<ByteArray>
    {
        val method = "readDescriptor"
        val timerId = bluetoothDevice.uuDescriptorTimerId(descriptor, method)

        val result = UUBluetoothResult<ByteArray>()

        if (activeJob != null)
        {
            debugLog(method, "Another job is active ${bluetoothGatt?.device?.address}")
            result.error = UUBluetoothError.preconditionFailedError("session busy")
            return result
        }

        val gatt = bluetoothGatt
        if (gatt == null)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt is null")
            result.error = UUBluetoothError.notConnectedError()
            return result
        }

        val job = Job()

        uuDispatchMain()
        {
            gattCallback.connectionStateChangedCallback =
                { gattStatus, connectionState ->

                    if (gattStatus != BluetoothGatt.GATT_SUCCESS)
                    {
                        result.error = UUBluetoothError.gattStatusError(method, gattStatus)
                    }
                    else if (connectionState != BluetoothGatt.STATE_CONNECTED)
                    {
                        result.error = UUBluetoothError.disconnectedError()
                    }

                    job.complete()
                }

            gattCallback.registerReadDescriptorCallback(descriptor)
            { data, error ->
                result.error = error
                result.success = data
                job.complete()
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog(method, "$method timeout: $bluetoothDevice")
                result.error = UUBluetoothError.timeoutError()
                gattCallback.connectionStateChangedCallback = null
                bluetoothGatt?.disconnect()
            }

            val ok = gatt.readDescriptor(descriptor)
            if (!ok)
            {
                result.error = UUBluetoothError.operationFailedError(method)
                job.complete()
            }
        }

        activeJob = job

        job.join()

        gattCallback.connectionStateChangedCallback = null
        gattCallback.clearReadDescriptorCallback(descriptor)
        UUTimer.cancelActiveTimer(timerId)

        activeJob = null

        return result
    }

    suspend fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int,
        timeout: Long = DEFAULT_TIMEOUT): UUError?
    {
        val method = "writeCharacteristic"
        val timerId = bluetoothDevice.uuCharacteristicTimerId(characteristic, method)

        var result: UUError? = null

        if (activeJob != null)
        {
            debugLog(method, "Another job is active ${bluetoothGatt?.device?.address}")
            return UUBluetoothError.preconditionFailedError("session busy")
        }

        val gatt = bluetoothGatt
        if (gatt == null)
        {
            debugLog(method, "WARNING -- Bluetooth Gatt is null")
            return UUBluetoothError.notConnectedError()
        }

        val job = Job()

        uuDispatchMain()
        {
            gattCallback.connectionStateChangedCallback =
            { gattStatus, connectionState ->

                if (gattStatus != BluetoothGatt.GATT_SUCCESS)
                {
                    result = UUBluetoothError.gattStatusError(method, gattStatus)
                }
                else if (connectionState != BluetoothGatt.STATE_CONNECTED)
                {
                    result = UUBluetoothError.disconnectedError()
                }

                job.complete()
            }

            gattCallback.registerWriteCharacteristicCallback(characteristic)
            { error ->
                result = error
                job.complete()
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog(method, "$method timeout: $bluetoothDevice")
                result = UUBluetoothError.timeoutError()
                gattCallback.connectionStateChangedCallback = null
                bluetoothGatt?.disconnect()
            }

            val err = gatt.uuWriteCharacteristic(characteristic, data, writeType)
            if (err != null)
            {
                result = err
                job.complete()
            }

        }

        activeJob = job

        job.join()

        gattCallback.connectionStateChangedCallback = null
        gattCallback.clearWriteCharacteristicCallback(characteristic)
        UUTimer.cancelActiveTimer(timerId)

        activeJob = null

        return result
    }

    @Suppress("DEPRECATION")
    private fun BluetoothGatt.uuWriteCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int): UUError?
    {
        val method = "writeCharacteristic"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            val bleResult = writeCharacteristic(characteristic, data, writeType)
            val error = if (bleResult != BluetoothStatusCodes.SUCCESS)
            {
                UUBluetoothError.operationFailedError(method)
            }
            else
            {
                null
            }

            error
        }
        else
        {
            characteristic.value = data
            characteristic.writeType = writeType
            val ok = writeCharacteristic(characteristic)
            val error = if (!ok)
            {
                UUBluetoothError.operationFailedError(method)
            }
            else
            {
                null
            }

            error
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

    private fun closeGatt()
    {
        try
        {
            bluetoothGatt?.close()
        }
        catch (ex: Exception)
        {
            logException("closeGatt", ex)
        }
        finally
        {
            bluetoothGatt = null
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



    /*
    fun connect(
        connectGattAutoFlag: Boolean,
        timeout: Long,
        disconnectTimeout: Long)
    {
        if (bluetoothGatt != null)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt is already connected to ${bluetoothGatt?.device?.address}")
            //notifyDisconnectDelegate(delegate, UUBluetoothError.alreadyConnectedError())
            return
        }

        if (isConnectWatchdogActive)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt making connection attempt to ${bluetoothGatt?.device?.address}")
            //notifyDisconnectDelegate(delegate, UUBluetoothError.alreadyConnectedError())
            return
        }

        val timerId = connectWatchdogTimerId()

        connectionDelegate = object : UUConnectionDelegate
        {
            override fun onConnected(peripheral: UUPeripheral)
            {
                debugLog("connect", "Connected to: $peripheral")
                UUTimer.cancelActiveTimer(timerId)
                disconnectError = null
                delegate.onConnected(peripheral)
            }

            override fun onDisconnected(peripheral: UUPeripheral, error: UUError?)
            {
                debugLog("connect", "Disconnected from: $peripheral, error: $error")
                cleanupAfterDisconnect()
                delegate.onDisconnected(peripheral, error)
            }
        }

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("connect", "Connect timeout: $peripheral")
            disconnect("connect.timeout", UUBluetoothError.timeoutError())
        }

        this.disconnectTimeout = disconnectTimeout

        uuDispatchMain()
        {
            debugLog("connect", "Connecting to: $peripheral, gattAuto: $connectGattAutoFlag")

            disconnectError = UUBluetoothError.connectionFailedError()

            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                connectGattAutoFlag,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

//            ++__GATT_CREATE_CALLS
//            logGattInfo("connect")
//
//            if ((__GATT_CREATE_CALLS - __GATT_CLOSE_CALLS) > 1)
//            {
//                debugLog("connect", "ERROR -- Gatt connect/close calls are out of sync!")
//            }
        }
    }

    fun disconnect(fromWhere: String, error: UUError?)
    {
        disconnectError = error

        if (disconnectError == null)
        {
            disconnectError = UUBluetoothError.success()
        }

        val timerId = disconnectWatchdogTimerId()
        val timeout = disconnectTimeout

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->

            debugLog("disconnect", "Disconnect timeout: $peripheral, from: $fromWhere")
            notifyDisconnected(error)

            // Just in case the timeout fires and a real disconnect is needed, this is the last
            // ditch effort to close the connection
            disconnectGattOnMainThread()
        }

        disconnectGattOnMainThread()
    }*/

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Closable Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun close()
    {
        debugLog("close", "$bluetoothDevice is being closed.")
        closeGatt()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun connectWatchdogTimerId(): String
    {
        return bluetoothDevice.uuTimerId("connect")
    }

    private fun disconnectWatchdogTimerId(): String
    {
        return bluetoothDevice.uuTimerId("disconnect")
    }

    private fun notifySessionEnded()
    {
        val block = sessionEnded
        if (block == null)
        {
            debugLog("notifySessionEnded", "sessionEnded callback is null, nothing to notify")
            return
        }

        uuDispatch()
        {
            block(sessionEndError)
        }
    }

    private fun debugLog(method: String, message: String)
    {
        UULog.d(javaClass, method, message)
    }

    private fun logException(method: String, exception: Throwable)
    {
        UULog.d(javaClass, method, "", exception)
    }

//    private fun logGattInfo(fromWhere: String)
//    {
//        UULog.d(javaClass,
//            "logGattInfo",
//            "$fromWhere, $id, GATT create calls: $__GATT_CREATE_CALLS, GATT close calls: $__GATT_CLOSE_CALLS"
//        )
//    }

    private fun statusLog(status: Int): String
    {
        return String.format(Locale.US, "%s (%d)", UUBluetooth.gattStatusToString(status), status)
    }
}