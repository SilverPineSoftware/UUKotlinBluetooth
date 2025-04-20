package com.silverpine.uu.bluetooth.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetooth.connectionStateToString
import com.silverpine.uu.bluetooth.UUBluetooth.gattStatusToString
import com.silverpine.uu.bluetooth.UUBluetoothConstants
import com.silverpine.uu.bluetooth.UUBluetoothError
import com.silverpine.uu.bluetooth.UUBluetoothErrorCode
import com.silverpine.uu.bluetooth.UUBluetoothGattCallback
import com.silverpine.uu.bluetooth.UUDiscoverCharacteristicsCompletionBlock
import com.silverpine.uu.bluetooth.UUDiscoverDescriptorsCompletionBlock
import com.silverpine.uu.bluetooth.UUDiscoverServicesCompletionBlock
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralCharacteristicErrorBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
import com.silverpine.uu.bluetooth.UUPeripheralDescriptorErrorBlock
import com.silverpine.uu.bluetooth.UUPeripheralDisconnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralErrorBlock
import com.silverpine.uu.bluetooth.UUPeripheralIntegerErrorBlock
import com.silverpine.uu.bluetooth.connectWatchdogTimerId
import com.silverpine.uu.bluetooth.disconnectWatchdogTimerId
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import java.util.Locale

@SuppressLint("MissingPermission")
internal class UUBluetoothDevicePeripheral(
    override val advertisement: UUBluetoothAdvertisement
) : UUPeripheral
{

    private val bluetoothDevice: BluetoothDevice = advertisement.device
    private val context: Context = UUBluetooth.requireApplicationContext()


    override var rssi: Int = advertisement.rssi
    override var firstDiscoveryTime: Long = 0L
    override var identifier: String = advertisement.address
    override var name: String = bluetoothDevice.name ?: ""
    override var friendlyName: String = advertisement.address
    override var peripheralState: UUPeripheralConnectionState =
        UUPeripheralConnectionState.Disconnected
    override var services: List<BluetoothGattService>? = null
    override var negotiatedMtuSize: Int? = 0






    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothGattCallback: UUBluetoothGattCallback = UUBluetoothGattCallback()

    private val isConnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(connectWatchdogTimerId) != null)


    private var disconnectError: UUError? = null
//    private val readCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val writeCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val characteristicChangedDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val setNotifyDelegates = HashMap<String, UUCharacteristicDelegate>()
//    private val readDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
//    private val writeDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
    private var disconnectTimeout: Long = 0


    private var disconnectedCallback: UUPeripheralDisconnectedBlock? = null

    override fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock
    )
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
            cleanupAfterDisconnect()
            notifyDisconnected(error)
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

        UUTimer.startTimer(timerId, timeout, null)
        { _, _ ->
            debugLog("connect", "Connect timeout: $this")
            disconnect("connect.timeout", UUBluetoothError.timeoutError())
        }

        //this.disconnectTimeout = disconnectTimeout

        val connectGattAutoFlag = false

        uuDispatchMain()
        {
            debugLog("connect", "Connecting to: $bluetoothDevice, gattAuto: $connectGattAutoFlag")

            disconnectError = UUBluetoothError.connectionFailedError()

            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                connectGattAutoFlag,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    override fun disconnect(timeout: Long)
    {

    }

    override fun discoverServices(
        serviceUUIDs: List<ParcelUuid>?,
        timeout: Long,
        completion: UUDiscoverServicesCompletionBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun discoverCharacteristics(
        characteristicUUIDs: List<ParcelUuid>?,
        service: BluetoothGattService,
        timeout: Long,
        completion: UUDiscoverCharacteristicsCompletionBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun discoverIncludedServices(
        includedServiceUUIDs: List<ParcelUuid>?,
        service: BluetoothGattService,
        timeout: Long,
        completion: UUPeripheralErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun discoverDescriptorsForCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUDiscoverDescriptorsCompletionBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun discover(
        characteristics: List<ParcelUuid>?,
        serviceUuid: ParcelUuid,
        timeout: Long,
        completion: UUDiscoverCharacteristicsCompletionBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun setNotifyValue(
        enabled: Boolean,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        notifyHandler: UUPeripheralCharacteristicErrorBlock?,
        completion: UUPeripheralCharacteristicErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun readValue(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUPeripheralCharacteristicErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun readValue(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUPeripheralDescriptorErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun writeValue(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUPeripheralCharacteristicErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun writeValue(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUPeripheralDescriptorErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun writeValueWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUPeripheralCharacteristicErrorBlock
    ) {
        TODO("Not yet implemented")
    }

    override fun readRSSI(timeout: Long, completion: UUPeripheralIntegerErrorBlock) {
        TODO("Not yet implemented")
    }

    override fun createL2capChannel(psm: Int): BluetoothSocket?
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            bluetoothDevice.createL2capChannel(psm)
        }
        else
        {
            null
        }
    }

    override fun createInsecureL2capChannel(psm: Int): BluetoothSocket?
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            bluetoothDevice.createInsecureL2capChannel(psm)
        }
        else
        {
            null
        }
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

















/*
    private fun notifyConnected(block: UUPeripheralConnectedBlock)
    {
        try
        {
            block()
        }
        catch (ex: Exception)
        {
            logException("notifyConnected", ex)
        }
    }

    private fun notifyDisconnected(error: UUError?)
    {
        try
        {
            val block = disconnectedCallback
            disconnectedCallback = null
            block?.invoke(error)
        }
        catch (ex: Exception)
        {
            logException("notifyDisconnected", ex)
        }
    }*/





    /*private fun notifyConnected(fromWhere: String)
    {
        try
        {
            debugLog("notifyConnected", "Notifying connected from: $fromWhere")
            peripheral.setBluetoothGatt(bluetoothGatt)
            val delegate = connectionDelegate
            notifyConnectDelegate(delegate)
        }
        catch (ex: Exception)
        {
            logException("notifyConnected", ex)
        }
    }*/

    /*private fun notifyDisconnected(error: UUError?)
    {
        closeGatt()
        cancelAllTimers()

        peripheral.setBluetoothGatt(null)
        val delegate = connectionDelegate
        connectionDelegate = null
        notifyDisconnectDelegate(delegate, error)
    }*/







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

    private fun notifyDisconnected(error: UUError?)
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
    }

    fun disconnect(fromWhere: String, error: UUError?)
    {
        disconnectError = error

        if (disconnectError == null)
        {
            disconnectError = UUBluetoothError.success()
        }

        val timerId = disconnectWatchdogTimerId
        val timeout = disconnectTimeout

        UUTimer.startTimer(timerId, timeout, null)
        { _, _ ->

            debugLog("disconnect", "Disconnect timeout: $bluetoothDevice, from: $fromWhere")
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
            val prefix = identifier
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















}