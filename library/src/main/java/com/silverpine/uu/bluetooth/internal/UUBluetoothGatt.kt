package com.silverpine.uu.bluetooth.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
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
import com.silverpine.uu.bluetooth.uuIsNotifying
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.io.Closeable
import java.util.Locale
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

    private val isConnecting: Boolean
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

    fun setNotifyValue(
        enabled: Boolean,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        notifyHandler: UUCharacteristicDataCallback?,
        completion: UUCharacteristicErrorCallback)
    {
        val timerId = setNotifyStateWatchdogTimerId(characteristic)

        val callback: UUCharacteristicErrorCallback =
        { updatedCharacteristic, error ->
            debugLog(
                "setNotifyState",
                "Set characteristic notify complete: $bluetoothDevice, error: $error}")
            bluetoothGattCallback.clearSetCharacteristicNotificationCallback(characteristic)
            UUTimer.cancelActiveTimer(timerId)
            completion.safeNotify(updatedCharacteristic, error)
        }

        bluetoothGattCallback.registerSetCharacteristicNotificationCallback(characteristic, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("setNotifyState", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(characteristic, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("setNotifyState", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(characteristic, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        val descriptorUuid = UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
        val descriptor = chr.getDescriptor(descriptorUuid)
        if (descriptor == null)
        {
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(chr, UUBluetoothError.missingRequiredDescriptor(descriptorUuid))
            return
        }

        if (enabled && notifyHandler != null)
        {
            bluetoothGattCallback.registerCharacteristicDataChangedCallback(chr, notifyHandler)
        }
        else
        {
            bluetoothGattCallback.clearCharacteristicDataChangedCallback(chr)
        }

        val start = System.currentTimeMillis()
        uuDispatchMain()
        {
            debugLog("toggleNotifyState", "Setting characteristic notify for ${characteristic.uuid}")

            val success = gatt.setCharacteristicNotification(chr, enabled)
            debugLog("toggleNotifyState", "setCharacteristicNotification returned $success")
            if (!success)
            {
                bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(chr, UUBluetoothError.operationFailedError("setCharacteristicNotification"))
                return@uuDispatchMain
            }

            val data = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            val timeoutLeft = timeout - (System.currentTimeMillis() - start)
            write(data, descriptor, timeoutLeft)
            { error ->

                debugLog("setNotifyState", "original char.isNotifying: ${characteristic.uuIsNotifying()}, updated char.isNotifying: ${chr.uuIsNotifying()}")
                bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(chr, error)
            }
        }
    }

    fun read(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUDataErrorCallback)
    {
        val timerId = readCharacteristicWatchdogTimerId(characteristic)

        val callback: UUDataErrorCallback =
        { data: ByteArray?,
          error: UUError? ->
            debugLog(
                "read:characteristic",
                "Read characteristic complete: $bluetoothDevice, error: $error, data: ${data?.uuToHex()}")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.clearReadCharacteristicCallback(characteristic)
            completion.safeNotify(data, error)
        }

        bluetoothGattCallback.registerReadCharacteristicCallback(characteristic, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("read:characteristic", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicRead(characteristic, null, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("read:characteristic", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicRead(characteristic, null, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("read:characteristic", "characteristic: $characteristic")
            val success = gatt.readCharacteristic(chr)
            debugLog("read:characteristic", "readCharacteristic returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyCharacteristicRead(characteristic, null, UUBluetoothError.operationFailedError("read:characteristic"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun read(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUDataErrorCallback)
    {
        val timerId = readDescriptorWatchdogTimerId(descriptor)

        val callback: UUDataErrorCallback =
        { data: ByteArray?,
          error: UUError? ->
            debugLog(
                "read:descriptor",
                "Read descriptor complete: $bluetoothDevice, error: $error, data: ${data?.uuToHex()}")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.clearReadDescriptorCallback(descriptor)
            completion.safeNotify(data, error)
        }

        bluetoothGattCallback.registerReadDescriptorCallback(descriptor, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("read:descriptor", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyDescriptorRead(descriptor, null, UUBluetoothError.notConnectedError())
            return
        }

        val desc = gatt.uuLookupDescriptor(descriptor)

        if (desc == null)
        {
            debugLog("read:descriptor", "descriptor is null!")
            bluetoothGattCallback.notifyDescriptorRead(descriptor, null, UUBluetoothError.missingRequiredDescriptor(descriptor.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("read:descriptor", "descriptor: ${descriptor.uuid}")
            val success = gatt.readDescriptor(desc)
            debugLog("read:descriptor", "readDescriptor returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyDescriptorRead(descriptor, null, UUBluetoothError.operationFailedError("read:descriptor"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun write(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUErrorCallback)
    {
        val timerId = writeCharacteristicWatchdogTimerId(characteristic)

        val callback: UUErrorCallback =
        { error: UUError? ->
            debugLog(
                "write:characteristic",
                "Write characteristic complete: $bluetoothDevice, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.clearWriteCharacteristicCallback(characteristic)
            completion.safeNotify(error)
        }

        bluetoothGattCallback.registerWriteCharacteristicCallback(characteristic, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("write:characteristic", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicWrite(characteristic, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("write:characteristic", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicWrite(characteristic, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("write:characteristic", "characteristic: ${characteristic.uuid}, tx: ${data.uuToHex()}")

            val err = gatt.uuWrite(data, chr, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            if (err != null)
            {
                bluetoothGattCallback.notifyCharacteristicWrite(characteristic, err)
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun writeWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUErrorCallback)
    {
        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("writeWithoutResponse", "bluetoothGatt is null!")
            completion.safeNotify(UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("writeWithoutResponse", "characteristic is null!")
            completion.safeNotify(UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("writeWithoutResponse", "characteristic: ${characteristic.uuid}, tx: ${data.uuToHex()}")

            val err = gatt.uuWrite(data, chr, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            if (err != null)
            {
                bluetoothGattCallback.notifyCharacteristicWrite(characteristic, err)
            }

            // Notify completion always.  There is no timeout or callback
            completion.safeNotify(err)
        }
    }

    fun write(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUErrorCallback)
    {
        val timerId = writeDescriptorWatchdogTimerId(descriptor)

        val callback: UUErrorCallback =
        { error: UUError? ->
            debugLog(
                "write:descriptor",
                "Write descriptor complete: $bluetoothDevice, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.clearWriteDescriptorCallback(descriptor)
            completion.safeNotify(error)
        }

        bluetoothGattCallback.registerWriteDescriptorCallback(descriptor, callback)

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("write:descriptor", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyDescriptorWrite(descriptor, UUBluetoothError.notConnectedError())
            return
        }

        val desc = gatt.uuLookupDescriptor(descriptor)

        if (desc == null)
        {
            debugLog("write:descriptor", "descriptor is null!")
            bluetoothGattCallback.notifyDescriptorWrite(descriptor, UUBluetoothError.missingRequiredDescriptor(descriptor.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("write:descriptor", "descriptor: ${descriptor.uuid}, tx: ${data.uuToHex()}")

            val err = gatt.uuWrite(data, desc)
            if (err != null)
            {
                bluetoothGattCallback.notifyDescriptorWrite(descriptor, err)
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun readRSSI(
        timeout: Long,
        completion: UUIntErrorCallback)
    {
        val timerId = readRssiWatchdogTimerId

        val callback: UUIntErrorCallback =
        { data, error ->
            debugLog(
                "readRSSI",
                "Read RSSI complete: $bluetoothDevice, error: $error, data: $data")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.readRssiCallback = null
            completion.safeNotify(data, error)
        }

        bluetoothGattCallback.readRssiCallback = callback

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("readRSSI", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyRemoteRssiRead(null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("readRSSI", "Reading remote RSSI")
            val success = gatt.readRemoteRssi()
            debugLog("readRSSI", "readRemoteRssi returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyRemoteRssiRead( null, UUBluetoothError.operationFailedError("readRSSI"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun requestMtu(
        mtu: Int,
        timeout: Long,
        completion: UUIntErrorCallback)
    {
        val timerId = requestMtuWatchdogTimerId

        val callback: UUIntErrorCallback =
        { data, error ->
            debugLog(
                "requestMtu",
                "Request MTU complete: $bluetoothDevice, error: $error, data: $data")
            UUTimer.cancelActiveTimer(timerId)
            bluetoothGattCallback.mtuChangedCallback = null
            completion.safeNotify(data, error)
        }

        bluetoothGattCallback.mtuChangedCallback = callback

        startTimeoutWatchdog(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("requestMtu", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyMtuChanged(null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("requestMtu", "Reading remote RSSI")
            val success = gatt.requestMtu(mtu)
            debugLog("requestMtu", "requestMtu returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyMtuChanged( null, UUBluetoothError.operationFailedError("requestMtu"))
            }
            // else
            //
            // wait for delegate or timeout
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

    private fun BluetoothGatt.uuSafeClose()
    {
        try
        {
            close()
        }
        catch (ex: Exception)
        {
            logException( "uuSafeClose", ex)
        }
    }

    // When passing BluetoothGatt objects around via Parcelable, the objects are not fully functional
    // So we always go lookup the characteristic from this BluetoothGatt instance.
    private fun BluetoothGatt.uuLookupCharacteristic(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic?
    {
        return getService(characteristic.service.uuid).characteristics.firstOrNull { it.uuid == characteristic.uuid }
    }

    // When passing BluetoothGatt objects around via Parcelable, the objects are not fully functional
    // So we always go lookup the characteristic from this BluetoothGatt instance.
    private fun BluetoothGatt.uuLookupDescriptor(descriptor: BluetoothGattDescriptor): BluetoothGattDescriptor?
    {
        return uuLookupCharacteristic(descriptor.characteristic)?.descriptors?.firstOrNull {  it.uuid == descriptor.uuid }
    }

    @Suppress("DEPRECATION")
    private fun BluetoothGatt.uuWrite(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        writeType: Int): UUError?
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            val result = writeCharacteristic(characteristic, data, writeType)
            debugLog("write:characteristic", "writeCharacteristic returned $result")

            return if (result == BluetoothStatusCodes.SUCCESS)
            {
                null
            }
            else
            {
                UUBluetoothError.operationFailedError("write:characteristic", result)
            }
        }
        else
        {
            characteristic.value = data
            characteristic.writeType = writeType
            val success = writeCharacteristic(characteristic)
            debugLog("write:characteristic", "writeCharacteristic returned $success")

            return if (success)
            {
                null
            }
            else
            {
                UUBluetoothError.operationFailedError("write:characteristic")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun BluetoothGatt.uuWrite(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor): UUError?
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            val result = writeDescriptor(descriptor, data)
            debugLog("write:descriptor", "writeDescriptor returned $result")

            return if (result == BluetoothStatusCodes.SUCCESS)
            {
                null
            }
            else
            {
                UUBluetoothError.operationFailedError("write:descriptor", result)
            }
        }
        else
        {
            descriptor.value = data
            val success = writeDescriptor(descriptor)
            debugLog("write:descriptor", "writeDescriptor returned $success")

            return if (success)
            {
                null
            }
            else
            {
                UUBluetoothError.operationFailedError("write:descriptor")
            }
        }
    }

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