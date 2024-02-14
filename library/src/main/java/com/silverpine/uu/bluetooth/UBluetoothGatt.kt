package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.silverpine.uu.bluetooth.UUBluetooth.characteristicPermissionsToString
import com.silverpine.uu.bluetooth.UUBluetooth.characteristicPropertiesToString
import com.silverpine.uu.bluetooth.UUBluetooth.connectionStateToString
import com.silverpine.uu.bluetooth.UUBluetooth.gattStatusToString
import com.silverpine.uu.bluetooth.UUBluetooth.requireApplicationContext
import com.silverpine.uu.bluetooth.UUBluetoothError.timeoutError
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuIsNotEmpty
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.io.Closeable
import java.util.Locale
import java.util.UUID

typealias UUCharacteristicDelegate = (UUPeripheral, BluetoothGattCharacteristic, UUError?)->Unit
typealias UUDescriptorDelegate = (UUPeripheral, BluetoothGattDescriptor, UUError?)->Unit
typealias UUDiscoverServicesDelegate = (ArrayList<BluetoothGattService>, UUError?)->Unit
typealias UUPeripheralDelegate = (UUPeripheral?)->Unit
typealias UUPeripheralErrorDelegate = (UUPeripheral,UUError?)->Unit
typealias UUDataDelegate = (ByteArray)->Unit

/**
 * A helpful set of wrapper methods around BluetoothGatt
 */
@SuppressLint("MissingPermission")
internal class UUBluetoothGatt(private val context: Context, peripheral: UUPeripheral): Closeable
{
    private val id = UURandom.uuid()

    private val peripheral: UUPeripheral

    var bluetoothGatt: BluetoothGatt? = null
        private set

    private val bluetoothGattCallback: BluetoothGattCallback
    private var connectionDelegate: UUConnectionDelegate? = null
    private var serviceDiscoveryDelegate: UUPeripheralErrorDelegate? = null
    private var readRssiDelegate: UUPeripheralErrorDelegate? = null
    private var requestMtuDelegate: UUPeripheralErrorDelegate? = null
    private var pollRssiDelegate: UUPeripheralDelegate? = null
    private var disconnectError: UUError? = null
    private val readCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
    private val writeCharacteristicDelegates = HashMap<String, UUCharacteristicDelegate>()
    private val characteristicChangedDelegates = HashMap<String, UUCharacteristicDelegate>()
    private val setNotifyDelegates = HashMap<String, UUCharacteristicDelegate>()
    private val readDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
    private val writeDescriptorDelegates = HashMap<String, UUDescriptorDelegate>()
    private var disconnectTimeout: Long = 0
    val isConnecting: Boolean
        get() = (bluetoothGatt != null && isConnectWatchdogActive)
    private val isConnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(connectWatchdogTimerId()) != null)


    var __GATT_CREATE_CALLS = 0
    var __GATT_CLOSE_CALLS = 0

    fun connect(
        connectGattAutoFlag: Boolean,
        timeout: Long,
        disconnectTimeout: Long,
        delegate: UUConnectionDelegate)
    {
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

            bluetoothGatt = peripheral.bluetoothDevice.connectGatt(
                context,
                connectGattAutoFlag,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            ++__GATT_CREATE_CALLS
            logGattInfo("connect")

            if ((__GATT_CREATE_CALLS - __GATT_CLOSE_CALLS) > 1)
            {
                debugLog("connect", "ERROR -- Gatt connect/close calls are out of sync!")
            }
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
    }

    private fun clearDelegates()
    {
        connectionDelegate = null
        serviceDiscoveryDelegate = null
        readRssiDelegate = null
        requestMtuDelegate = null
        pollRssiDelegate = null
        readCharacteristicDelegates.clear()
        writeCharacteristicDelegates.clear()
        characteristicChangedDelegates.clear()
        setNotifyDelegates.clear()
        readDescriptorDelegates.clear()
        writeDescriptorDelegates.clear()
    }

    private fun requestHighPriority(): Boolean
    {
        val gatt = bluetoothGatt ?: return false

        try
        {
            val connectionPriority = BluetoothGatt.CONNECTION_PRIORITY_HIGH
            debugLog("requestHighPriority", "Requesting connection priority $connectionPriority")
            val result = gatt.requestConnectionPriority(connectionPriority)
            debugLog("requestHighPriority", "requestConnectionPriority returned $result")
            return result
        }
        catch (ex: Exception)
        {
            logException("requestHighPriority", ex)
        }

        return false
    }

    fun requestHighPriority(delegate: UUPeripheralBoolDelegate)
    {
        uuDispatchMain()
        {
            val result = requestHighPriority()
            notifyBoolResult(delegate, result)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestMtuSize(timeout: Long, mtuSize: Int, completion: UUPeripheralErrorDelegate)
    {
        val timerId = requestMtuWatchdogTimerId()

        requestMtuDelegate =
        { peripheral, error ->
            debugLog("requestMtuSize", "Request MTU Size complete: $peripheral, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            completion.invoke(peripheral, error)
        }

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("requestMtuSize", "Request MTU Size timeout: $peripheral")
            notifyReqeustMtuComplete(UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("requestMtuSize", "bluetoothGatt is null!")
                notifyReqeustMtuComplete(UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            debugLog("requestMtuSize", "Reading RSSI for: " + peripheral)
            val ok: Boolean = bluetoothGatt!!.requestMtu(mtuSize)
            debugLog("requestMtuSize", "returnCode: " + ok)

            if (!ok)
            {
                notifyReqeustMtuComplete(UUBluetoothError.operationFailedError("requestMtuSize"))
            }
        }
    }

    fun discoverServices(
        timeout: Long,
        completion: UUPeripheralErrorDelegate)
    {
        val timerId = serviceDiscoveryWatchdogTimerId()

        serviceDiscoveryDelegate =
        { peripheral, error ->
            debugLog("discoverServices", "Service Discovery complete: $peripheral, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            completion.invoke(peripheral, error)
        }

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("discoverServices", "Service Discovery timeout: $peripheral")
            disconnect("discoverServices.timeout", UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("discoverServices", "bluetoothGatt is null!")
                notifyServicesDiscovered(UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            debugLog("discoverServices", "Discovering services for: $peripheral")
            val ok = bluetoothGatt!!.discoverServices()
            debugLog("discoverServices", "returnCode: $ok")

            if (!ok)
            {
                notifyServicesDiscovered(UUBluetoothError.operationFailedError("discoverServices"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun readCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        delegate: UUCharacteristicDelegate)
    {
        val timerId = readCharacteristicWatchdogTimerId(characteristic)

        val readCharacteristicDelegate: UUCharacteristicDelegate =
        { peripheral,
                characteristic1: BluetoothGattCharacteristic,
                error: UUError? ->
                debugLog(
                    "readCharacteristic",
                    "Read characteristic complete: $peripheral, error: $error, data: ${characteristic1.value?.uuToHex()}")
                UUTimer.cancelActiveTimer(timerId)
                removeReadCharacteristicDelegate(characteristic)
                delegate.invoke(peripheral, characteristic, error)

        }

        registerReadCharacteristicDelegate(characteristic, readCharacteristicDelegate)

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _,_ ->
            debugLog("readCharacteristic", "Read characteristic timeout: $peripheral")
            disconnect("readCharacteristic.timeout", UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("readCharacteristic", "bluetoothGatt is null!")
                notifyCharacteristicRead(characteristic, UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            debugLog("readCharacteristic", "characteristic: " + characteristic.uuid)
            val success = bluetoothGatt!!.readCharacteristic(characteristic)
            debugLog("readCharacteristic", "readCharacteristic returned $success")

            if (!success)
            {
                notifyCharacteristicRead(characteristic, UUBluetoothError.operationFailedError("readCharacteristic"))
            }
        }
    }

    fun readDescriptor(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        delegate: UUDescriptorDelegate
    ) {
        val timerId = readDescritporWatchdogTimerId(descriptor)
        val readDescriptorDelegate: UUDescriptorDelegate =
        { peripheral: UUPeripheral,
          descriptor1: BluetoothGattDescriptor,
          error: UUError? ->
                debugLog(
                    "readDescriptor",
                    "Read descriptor complete: $peripheral, error: $error, data: ${descriptor.value?.uuToHex()}")
                removeReadDescriptorDelegate(descriptor)
                UUTimer.cancelActiveTimer(timerId)
                delegate(peripheral, descriptor, error)
        }

        registerReadDescriptorDelegate(descriptor, readDescriptorDelegate)
        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("readDescriptor", "Read descriptor timeout: $peripheral")
            disconnect("readDescriptor.timeout", UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("readDescriptor", "bluetoothGatt is null!")
                notifyDescriptorRead(descriptor, UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            debugLog("readDescriptor", "descriptor: " + descriptor.uuid)
            val success = bluetoothGatt!!.readDescriptor(descriptor)
            debugLog("readDescriptor", "readDescriptor returned $success")

            if (!success)
            {
                notifyDescriptorRead(descriptor, UUBluetoothError.operationFailedError("readDescriptor"))
            }
        }
    }

    fun writeDescriptor(
        descriptor: BluetoothGattDescriptor,
        data: ByteArray?,
        timeout: Long,
        delegate: UUDescriptorDelegate)
    {
        val timerId = writeDescriptorWatchdogTimerId(descriptor)

        val writeDescriptorDelegate: UUDescriptorDelegate =
        { peripheral: UUPeripheral,
          descriptor1: BluetoothGattDescriptor,
          error: UUError? ->

            debugLog(
                "readDescriptor",
                "Write descriptor complete: $peripheral, error: $error, data: ${descriptor.value?.uuToHex()}")
            removeWriteDescriptorDelegate(descriptor)
            UUTimer.cancelActiveTimer(timerId)
            delegate(peripheral, descriptor, error)
        }

        registerWriteDescriptorDelegate(descriptor, writeDescriptorDelegate)
        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("writeDescriptor", "Write descriptor timeout: $peripheral")
            disconnect("writeDescriptor", timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("writeDescriptor", "bluetoothGatt is null!")
                notifyDescriptorWritten(descriptor, UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            descriptor.value = data
            val success = bluetoothGatt!!.writeDescriptor(descriptor)
            debugLog("writeDescriptor", "writeDescriptor returned $success")
            if (!success)
            {
                notifyDescriptorWritten(descriptor, UUBluetoothError.operationFailedError("writeDescriptor"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun setNotifyState(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean,
        timeout: Long,
        notifyDelegate: UUCharacteristicDelegate?,
        delegate: UUCharacteristicDelegate)
    {
        val timerId = setNotifyStateWatchdogTimerId(characteristic)
        val setNotifyDelegate: UUCharacteristicDelegate =
        {
            peripheral: UUPeripheral,
            characteristic1: BluetoothGattCharacteristic,
            error: UUError? ->
            debugLog(
                "setNotifyState",
                "Set characteristic notify complete: $peripheral, error: $error, data: ${characteristic1.value?.uuToHex()}")
            removeSetNotifyDelegate(characteristic1)
            UUTimer.cancelActiveTimer(timerId)
            delegate.invoke(peripheral, characteristic1, error)
        }

        registerSetNotifyDelegate(characteristic, setNotifyDelegate)
        UUTimer.startTimer(timerId, timeout, peripheral)
        { _,_ ->
            debugLog("setNotifyState", "Set notify state timeout: $peripheral")
            disconnect("setNotifyState.timeout", UUBluetoothError.timeoutError())
        }

        val start = System.currentTimeMillis()
        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("toggleNotifyState", "bluetoothGatt is null!")
                notifyCharacteristicNotifyStateChanged(characteristic, UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            if (enabled && notifyDelegate != null)
            {
                registerCharacteristicChangedDelegate(characteristic, notifyDelegate)
            }
            else
            {
                removeCharacteristicChangedDelegate(characteristic)
            }

            debugLog("toggleNotifyState", "Setting characteristic notify for ${characteristic.uuid}")

            val success = bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
            debugLog("toggleNotifyState", "setCharacteristicNotification returned $success")
            if (!success)
            {
                notifyCharacteristicNotifyStateChanged(characteristic, UUBluetoothError.operationFailedError("setCharacteristicNotification"))
                return@uuDispatchMain
            }

            val descriptor = characteristic.getDescriptor(UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
            if (descriptor == null)
            {
                notifyCharacteristicNotifyStateChanged(characteristic, UUBluetoothError.operationFailedError("getDescriptor"))
                return@uuDispatchMain
            }

            val data = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            val timeoutLeft = timeout - (System.currentTimeMillis() - start)
            writeDescriptor(descriptor, data, timeoutLeft)
            {
                peripheral1: UUPeripheral,
                descriptor1: BluetoothGattDescriptor,
                error: UUError? ->

                notifyCharacteristicNotifyStateChanged(characteristic, error)
            }
        }
    }

    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        timeout: Long,
        delegate: UUCharacteristicDelegate)
    {
        writeCharacteristic(
            characteristic,
            data,
            timeout,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            delegate
        )
    }

    fun writeCharacteristicWithoutResponse(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        timeout: Long,
        delegate: UUCharacteristicDelegate)
    {
        writeCharacteristic(
            characteristic,
            data,
            timeout,
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            delegate
        )
    }

    private fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        timeout: Long,
        writeType: Int,
        delegate: UUCharacteristicDelegate)
    {
        val timerId = writeCharacteristicWatchdogTimerId(characteristic)
        val writeCharacteristicDelegate: UUCharacteristicDelegate =
        {
            peripheral: UUPeripheral,
            characteristic1: BluetoothGattCharacteristic,
            error: UUError? ->

            debugLog(
                "writeCharacteristic",
                "Write characteristic complete: $peripheral, error: $error, data: ${characteristic1.value?.uuToHex()}")
            removeWriteCharacteristicDelegate(characteristic1)
            UUTimer.cancelActiveTimer(timerId)
            delegate.invoke(peripheral, characteristic1, error)
        }

        registerWriteCharacteristicDelegate(characteristic, writeCharacteristicDelegate)
        UUTimer.startTimer(timerId, timeout, peripheral)
        { _,_ ->
            debugLog("writeCharacteristic", "Write characteristic timeout: $peripheral")
            disconnect("writeCharacteristic.timeout", UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null)
            {
                debugLog("writeCharacteristic", "bluetoothGatt is null!")
                notifyCharacteristicWritten(characteristic, UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }

            debugLog("writeCharacteristic", "characteristic: ${characteristic.uuid}, data: ${data.uuToHex()}")
            debugLog("writeCharacteristic", "props: ${characteristicPropertiesToString(characteristic.properties)}, (${characteristic.properties})")
            debugLog("writeCharacteristic", "permissions: ${characteristicPermissionsToString(characteristic.permissions)}, (${characteristic.permissions}")

            characteristic.value = data
            characteristic.writeType = writeType
            val success = bluetoothGatt!!.writeCharacteristic(characteristic)
            debugLog("writeCharacteristic", "writeCharacteristic returned $success")

            if (!success)
            {
                notifyCharacteristicWritten(characteristic, UUBluetoothError.operationFailedError("writeCharacteristic"))
            }
        }
    }

    fun readRssi(
        timeout: Long,
        completion: UUPeripheralErrorDelegate)
    {
        val timerId = readRssiWatchdogTimerId()

        readRssiDelegate =
        { peripheral, error ->
            debugLog("readRssi", "Read RSSI complete: $peripheral, error: $error")
            UUTimer.cancelActiveTimer(timerId)
            completion.invoke(peripheral, error)
        }

        UUTimer.startTimer(timerId, timeout, peripheral)
        { _, _ ->
            debugLog("readRssi", "Read RSSI timeout: $peripheral")
            notifyReadRssiComplete(UUBluetoothError.timeoutError())
        }

        uuDispatchMain()
        {
            if (bluetoothGatt == null) {
                debugLog("readRssi", "bluetoothGatt is null!")
                notifyReadRssiComplete(UUBluetoothError.notConnectedError())
                return@uuDispatchMain
            }
            debugLog("readRssi", "Reading RSSI for: $peripheral")
            val ok = bluetoothGatt!!.readRemoteRssi()
            debugLog("readRssi", "returnCode: $ok")
            if (!ok) {
                notifyReadRssiComplete(UUBluetoothError.operationFailedError("readRemoteRssi"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    // Begins polling RSSI for a peripheral.  When the RSSI is successfully
    // retrieved, the peripheralFoundBlock is called.  This method is useful to
    // perform a crude 'ranging' logic when already connected to a peripheral
    fun startRssiPolling(context: Context, interval: Long, delegate: UUPeripheralDelegate)
    {
        pollRssiDelegate = delegate
        val timerId = pollRssiTimerId()
        UUTimer.cancelActiveTimer(timerId)

        readRssi(TIMEOUT_DISABLED.toLong())
        { peripheral, error ->

            debugLog(
                "rssiPoll", String.format(
                    Locale.US, "RSSI (%d) Updated for %s-%s, error: %s",
                    peripheral.rssi, peripheral.address, peripheral.name, error
                )
            )
            val pollDelegate = pollRssiDelegate
            if (error == null) {
                notifyPeripheralDelegate(pollDelegate)
            } else {
                debugLog("startRssiPolling.onComplete", "Error while reading RSSI: $error")
            }
            if (pollDelegate != null) {
                UUTimer.startTimer(timerId, interval, peripheral) { _,_ ->
                    debugLog(
                        "rssiPolling.timer",
                        String.format(
                            Locale.US,
                            "RSSI Polling timer %s - %s",
                            peripheral.address,
                            peripheral.name
                        )
                    )

                    val pollingDelegate = pollRssiDelegate
                    if (pollingDelegate == null)
                    {
                        debugLog(
                            "rssiPolling.timer",
                            String.format(
                                Locale.US,
                                "Peripheral %s-%s not polling anymore",
                                peripheral.address,
                                peripheral.address
                            )
                        )
                    }
                    else if (peripheral.connectionState == UUPeripheral.ConnectionState.Connected)
                    {
                        startRssiPolling(context, interval, delegate)
                    }
                    else
                    {
                        debugLog(
                            "rssiPolling.timer",
                            String.format(
                                Locale.US,
                                "Peripheral %s-%s is not connected anymore, cannot poll for RSSI",
                                peripheral.address,
                                peripheral.name
                            )
                        )
                    }
                }
            }
        }
    }

    fun stopRssiPolling()
    {
        pollRssiDelegate = null
        UUTimer.cancelActiveTimer(pollRssiTimerId())
    }

    val isPollingForRssi: Boolean
        get() = (pollRssiDelegate != null)

    private fun notifyConnectDelegate(delegate: UUConnectionDelegate?)
    {
        try
        {
            delegate?.onConnected((peripheral))
        }
        catch (ex: Exception)
        {
            logException("notifyConnectDelegate", ex)
        }
    }

    private fun notifyDisconnectDelegate(delegate: UUConnectionDelegate?, error: UUError?)
    {
        try
        {
            delegate?.onDisconnected((peripheral), error)
        }
        catch (ex: Exception)
        {
            logException("notifyDisconnectDelegate", ex)
        }
    }

    private fun notifyPeripheralErrorDelegate(delegate: UUPeripheralErrorDelegate?, error: UUError?)
    {
        try
        {
            delegate?.invoke(peripheral, error)
        }
        catch (ex: Exception)
        {
            logException("notifyPeripheralErrorDelegate", ex)
        }
    }

    private fun notifyPeripheralDelegate(delegate: UUPeripheralDelegate?)
    {
        try
        {
            delegate?.invoke(peripheral)
        }
        catch (ex: Exception)
        {
            logException("notifyPeripheralDelegate", ex)
        }
    }

    private fun notifyCharacteristicDelegate(delegate: UUCharacteristicDelegate?, characteristic: BluetoothGattCharacteristic, error: UUError?)
    {
        try
        {
            delegate?.invoke(peripheral, characteristic, error)
        }
        catch (ex: Exception)
        {
            logException("notifyCharacteristicDelegate", ex)
        }
    }

    private fun notifyDescriptorDelegate(delegate: UUDescriptorDelegate?, descriptor: BluetoothGattDescriptor, error: UUError?)
    {
        try
        {
            delegate?.invoke(peripheral, descriptor, error)
        }
        catch (ex: Exception)
        {
            logException("notifyDescriptorDelegate", ex)
        }
    }

    private fun notifyConnected(fromWhere: String)
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
    }

    private fun notifyDisconnected(error: UUError?)
    {
        closeGatt()
        cancelAllTimers()

        peripheral.setBluetoothGatt(null)
        val delegate = connectionDelegate
        connectionDelegate = null
        notifyDisconnectDelegate(delegate, error)
    }

    private fun notifyServicesDiscovered(error: UUError?)
    {
        val delegate = serviceDiscoveryDelegate
        serviceDiscoveryDelegate = null
        notifyPeripheralErrorDelegate(delegate, error)
    }

    private fun notifyDescriptorWritten(descriptor: BluetoothGattDescriptor, error: UUError?)
    {
        val delegate = getWriteDescriptorDelegate(descriptor)
        removeWriteDescriptorDelegate(descriptor)
        notifyDescriptorDelegate(delegate, descriptor, error)
    }

    private fun notifyCharacteristicNotifyStateChanged(characteristic: BluetoothGattCharacteristic, error: UUError?)
    {
        val delegate = getSetNotifyDelegate(characteristic)
        removeSetNotifyDelegate(characteristic)
        notifyCharacteristicDelegate(delegate, characteristic, error)
    }

    private fun notifyCharacteristicWritten(characteristic: BluetoothGattCharacteristic, error: UUError?)
    {
        val delegate = getWriteCharacteristicDelegate(characteristic)
        removeWriteCharacteristicDelegate(characteristic)
        notifyCharacteristicDelegate(delegate, characteristic, error)
    }

    private fun notifyCharacteristicRead(characteristic: BluetoothGattCharacteristic, error: UUError?)
    {
        val delegate = getReadCharacteristicDelegate(characteristic)
        removeReadCharacteristicDelegate(characteristic)
        notifyCharacteristicDelegate(delegate, characteristic, error)
    }

    private fun notifyCharacteristicChanged(characteristic: BluetoothGattCharacteristic)
    {
        val delegate = getCharacteristicChangedDelegate(characteristic)
        notifyCharacteristicDelegate(delegate, characteristic, null)
    }

    private fun notifyDescriptorRead(descriptor: BluetoothGattDescriptor, error: UUError?)
    {
        val delegate = getReadDescriptorDelegate(descriptor)
        removeReadDescriptorDelegate(descriptor)
        notifyDescriptorDelegate(delegate, descriptor, error)
    }

    private fun notifyReadRssiComplete(error: UUError?)
    {
        val delegate = readRssiDelegate
        readRssiDelegate = null
        notifyPeripheralErrorDelegate(delegate, error)
    }

    private fun notifyReqeustMtuComplete(error: UUError?)
    {
        val delegate = requestMtuDelegate
        requestMtuDelegate = null
        notifyPeripheralErrorDelegate(delegate, error)
    }

    private fun notifyBoolResult(delegate: UUPeripheralBoolDelegate?, result: Boolean)
    {
        try
        {
            delegate?.onComplete((peripheral)!!, result)
        }
        catch (ex: Exception)
        {
            logException("notifyBoolResult", ex)
        }
    }

    private fun registerCharacteristicChangedDelegate(characteristic: BluetoothGattCharacteristic, delegate: UUCharacteristicDelegate)
    {
        characteristicChangedDelegates[safeUuidString(characteristic)] = delegate
    }

    private fun removeCharacteristicChangedDelegate(characteristic: BluetoothGattCharacteristic)
    {
        characteristicChangedDelegates.remove(safeUuidString(characteristic))
    }

    private fun getCharacteristicChangedDelegate(characteristic: BluetoothGattCharacteristic): UUCharacteristicDelegate?
    {
        return characteristicChangedDelegates[safeUuidString(characteristic)]
    }

    private fun registerSetNotifyDelegate(characteristic: BluetoothGattCharacteristic, delegate: UUCharacteristicDelegate)
    {
        setNotifyDelegates[safeUuidString(characteristic)] = delegate
    }

    private fun removeSetNotifyDelegate(characteristic: BluetoothGattCharacteristic)
    {
        setNotifyDelegates.remove(safeUuidString(characteristic))
    }

    private fun getSetNotifyDelegate(characteristic: BluetoothGattCharacteristic): UUCharacteristicDelegate?
    {
        return setNotifyDelegates[safeUuidString(characteristic)]
    }

    private fun registerReadCharacteristicDelegate(characteristic: BluetoothGattCharacteristic, delegate: UUCharacteristicDelegate)
    {
        readCharacteristicDelegates[safeUuidString(characteristic)] = delegate
    }

    private fun removeReadCharacteristicDelegate(characteristic: BluetoothGattCharacteristic)
    {
        readCharacteristicDelegates.remove(safeUuidString(characteristic))
    }

    private fun getReadCharacteristicDelegate(characteristic: BluetoothGattCharacteristic): UUCharacteristicDelegate?
    {
        return readCharacteristicDelegates[safeUuidString(characteristic)]
    }

    private fun registerWriteCharacteristicDelegate(characteristic: BluetoothGattCharacteristic, delegate: UUCharacteristicDelegate)
    {
        writeCharacteristicDelegates[safeUuidString(characteristic)] = delegate
    }

    private fun removeWriteCharacteristicDelegate(characteristic: BluetoothGattCharacteristic)
    {
        writeCharacteristicDelegates.remove(safeUuidString(characteristic))
    }

    private fun getWriteCharacteristicDelegate(characteristic: BluetoothGattCharacteristic): UUCharacteristicDelegate?
    {
        return writeCharacteristicDelegates[safeUuidString(characteristic)]
    }

    private fun registerReadDescriptorDelegate(descriptor: BluetoothGattDescriptor, delegate: UUDescriptorDelegate)
    {
        readDescriptorDelegates[safeUuidString(descriptor)] = delegate
    }

    private fun removeReadDescriptorDelegate(descriptor: BluetoothGattDescriptor)
    {
        readDescriptorDelegates.remove(safeUuidString(descriptor))
    }

    private fun getReadDescriptorDelegate(descriptor: BluetoothGattDescriptor): UUDescriptorDelegate?
    {
        return readDescriptorDelegates[safeUuidString(descriptor)]
    }

    private fun registerWriteDescriptorDelegate(descriptor: BluetoothGattDescriptor, delegate: UUDescriptorDelegate)
    {
        writeDescriptorDelegates[safeUuidString(descriptor)] = delegate
    }

    private fun removeWriteDescriptorDelegate(descriptor: BluetoothGattDescriptor)
    {
        writeDescriptorDelegates.remove(safeUuidString(descriptor))
    }

    private fun getWriteDescriptorDelegate(descriptor: BluetoothGattDescriptor): UUDescriptorDelegate?
    {
        return writeDescriptorDelegates[safeUuidString(descriptor)]
    }

    private fun safeUuidString(characteristic: BluetoothGattCharacteristic?): String
    {
        return if (characteristic != null)
        {
            safeUuidString(characteristic.uuid)
        }
        else
        {
            ""
        }
    }

    private fun safeUuidString(descriptor: BluetoothGattDescriptor?): String
    {
        return if (descriptor != null)
        {
            safeUuidString(descriptor.uuid)
        }
        else
        {
            ""
        }
    }

    private fun safeUuidString(uuid: UUID?): String
    {
        var result: String? = null

        if (uuid != null)
        {
            result = uuid.toString()
        }

        if (result == null)
        {
            result = ""
        }

        result = result.lowercase(Locale.getDefault())
        return result
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
            debugLog("disconnectGatt", "Disconnecting from: $peripheral, bluetoothGatt: $bluetoothGatt")
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
            val gatt = bluetoothGatt
            if (gatt != null)
            {
                ++__GATT_CLOSE_CALLS
                gatt.close()
            }
        }
        catch (ex: Exception)
        {
            logException("closeGatt", ex)
        }
        finally
        {
            bluetoothGatt = null
            logGattInfo("closeGatt-finally")
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

    private fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, message)
        }
    }

    private fun logException(method: String, exception: Throwable)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, "", exception)
        }
    }

    private fun logGattInfo(fromWhere: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass,
                "logGattInfo",
                "$fromWhere, $id, GATT create calls: $__GATT_CREATE_CALLS, GATT close calls: $__GATT_CLOSE_CALLS"
            )
        }
    }

    private fun statusLog(status: Int): String
    {
        return String.format(Locale.US, "%s (%d)", gattStatusToString(status), status)
    }

    private fun formatPeripheralTimerId(bucket: String): String
    {
        return String.format(Locale.US, "%s__%s", peripheral.address, bucket)
    }

    private fun formatCharacteristicTimerId(characteristic: BluetoothGattCharacteristic, bucket: String): String
    {
        return String.format(
            Locale.US,
            "%s__ch_%s__%s",
            peripheral.address,
            safeUuidString(characteristic),
            bucket
        )
    }

    private fun formatDescriptorTimerId(descriptor: BluetoothGattDescriptor, bucket: String): String
    {
        return String.format(
            Locale.US,
            "%s__de_%s__%s",
            peripheral.address,
            safeUuidString(descriptor),
            bucket
        )
    }

    private fun connectWatchdogTimerId(): String
    {
        return formatPeripheralTimerId(CONNECT_WATCHDOG_BUCKET)
    }

    private fun disconnectWatchdogTimerId(): String
    {
        return formatPeripheralTimerId(DISCONNECT_WATCHDOG_BUCKET)
    }

    private fun serviceDiscoveryWatchdogTimerId(): String
    {
        return formatPeripheralTimerId(SERVICE_DISCOVERY_WATCHDOG_BUCKET)
    }

    private fun setNotifyStateWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return formatCharacteristicTimerId(characteristic, CHARACTERISTIC_NOTIFY_STATE_WATCHDOG_BUCKET)
    }

    private fun readCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return formatCharacteristicTimerId(characteristic, READ_CHARACTERISTIC_WATCHDOG_BUCKET)
    }

    private fun readDescritporWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
    {
        return formatDescriptorTimerId(descriptor, READ_DESCRIPTOR_WATCHDOG_BUCKET)
    }

    private fun writeCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return formatCharacteristicTimerId(characteristic, WRITE_CHARACTERISTIC_WATCHDOG_BUCKET)
    }

    private fun writeDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
    {
        return formatDescriptorTimerId(descriptor, WRITE_DESCRIPTOR_WATCHDOG_BUCKET)
    }

    private fun readRssiWatchdogTimerId(): String
    {
        return formatPeripheralTimerId(READ_RSSI_WATCHDOG_BUCKET)
    }

    private fun requestMtuWatchdogTimerId(): String
    {
        return formatPeripheralTimerId(REQUEST_MTU_WATCHDOG_BUCKET)
    }

    private fun pollRssiTimerId(): String
    {
        return formatPeripheralTimerId(POLL_RSSI_BUCKET)
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
            val prefix = peripheral.address
            if (prefix != null)
            {
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
            }
        }
        catch (ex: Exception)
        {
            logException("cancelAllTimers", ex)
        }
    }

    private inner class UUBluetoothGattCallback : BluetoothGattCallback()
    {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
        {
            debugLog(
                "onConnectionStateChanged", String.format(
                    Locale.US, "status: %s, newState: %s (%d)",
                    statusLog(status), connectionStateToString(newState), newState))

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED)
            {
                notifyConnected("onConnectionStateChange")
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

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
        {
            debugLog("onServicesDiscovered", String.format(Locale.US, "status: %s", statusLog(status)))
            notifyServicesDiscovered(UUBluetoothError.gattStatusError("onServicesDiscovered", status))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int)
        {
            debugLog(
                "onCharacteristicRead",
                "characteristic: " + safeUuidString(characteristic) +
                        ", status: " + statusLog(status) +
                        ", char.data: ${characteristic.value?.uuToHex()}")

            notifyCharacteristicRead(
                characteristic,
                UUBluetoothError.gattStatusError("onCharacteristicRead", status)
            )
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            debugLog(
                "onCharacteristicWrite",
                "characteristic: " + safeUuidString(characteristic) +
                        ", status: " + statusLog(status) +
                        ", char.data: ${characteristic.value?.uuToHex()}")

            notifyCharacteristicWritten(
                characteristic,
                UUBluetoothError.gattStatusError("onCharacteristicWrite", status)
            )
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            debugLog(
                "onCharacteristicChanged",
                "characteristic: " + safeUuidString(characteristic) +
                        ", char.data: ${characteristic.value}")
            notifyCharacteristicChanged(characteristic)
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            debugLog(
                "onDescriptorRead",
                "descriptor: " + safeUuidString(descriptor) +
                        ", status: " + statusLog(status) +
                        ", char.data: ${descriptor.value?.uuToHex()}")
            notifyDescriptorRead(
                descriptor,
                UUBluetoothError.gattStatusError("onDescriptorRead", status)
            )
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            debugLog(
                "onDescriptorWrite",
                "descriptor: " + safeUuidString(descriptor) +
                        ", status: " + statusLog(status) +
                        ", char.data: ${descriptor.value?.uuToHex()}")
            notifyDescriptorWritten(
                descriptor,
                UUBluetoothError.gattStatusError("onDescriptorWrite", status)
            )
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
            debugLog("onReliableWriteCompleted", ", status: $status")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            debugLog(
                "onReadRemoteRssi",
                "device: " + peripheral!!.address + ", rssi: " + rssi + ", status: " + status
            )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                peripheral.updateRssi(rssi)
            }
            notifyReadRssiComplete(UUBluetoothError.gattStatusError("onReadRemoteRssi", status))
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            debugLog(
                "onMtuChanged",
                "device: " + peripheral!!.address + ", mtu: " + mtu + ", status: " + status
            )
            peripheral.negotiatedMtuSize = null
            if (status == BluetoothGatt.GATT_SUCCESS) {
                peripheral.negotiatedMtuSize = mtu
            }
            notifyReqeustMtuComplete(UUBluetoothError.gattStatusError("onMtuChanged", status))
        }
    }

    init {
        this.peripheral = peripheral
        bluetoothGattCallback = UUBluetoothGattCallback()
    }

    companion object {
        private val LOGGING_ENABLED = true //BuildConfig.DEBUG

        // Internal Constants
        private val CONNECT_WATCHDOG_BUCKET = "UUBluetoothConnectWatchdogBucket"
        private val SERVICE_DISCOVERY_WATCHDOG_BUCKET = "UUBluetoothServiceDiscoveryWatchdogBucket"
        private val CHARACTERISTIC_NOTIFY_STATE_WATCHDOG_BUCKET =
            "UUBluetoothCharacteristicNotifyStateWatchdogBucket"
        private val READ_CHARACTERISTIC_WATCHDOG_BUCKET =
            "UUBluetoothReadCharacteristicValueWatchdogBucket"
        private val WRITE_CHARACTERISTIC_WATCHDOG_BUCKET =
            "UUBluetoothWriteCharacteristicValueWatchdogBucket"
        private val READ_DESCRIPTOR_WATCHDOG_BUCKET = "UUBluetoothReadDescriptorValueWatchdogBucket"
        private val WRITE_DESCRIPTOR_WATCHDOG_BUCKET =
            "UUBluetoothWriteDescriptorValueWatchdogBucket"
        private val READ_RSSI_WATCHDOG_BUCKET = "UUBluetoothReadRssiWatchdogBucket"
        private val POLL_RSSI_BUCKET = "UUBluetoothPollRssiBucket"
        private val DISCONNECT_WATCHDOG_BUCKET = "UUBluetoothDisconnectWatchdogBucket"
        private val REQUEST_MTU_WATCHDOG_BUCKET = "UUBluetoothRequestWatchdogBucket"
        private val TIMEOUT_DISABLED = -1


        ////////////////////////////////////////////////////////////////////////////////////////////////
        // Static Gatt management
        ////////////////////////////////////////////////////////////////////////////////////////////////
        private val gattHashMap = HashMap<String?, UUBluetoothGatt>()
        fun gattForPeripheral(peripheral: UUPeripheral): UUBluetoothGatt?
        {
            val ctx = requireApplicationContext()
            var gatt: UUBluetoothGatt? = null
            val address = peripheral.address

            if (address.uuIsNotEmpty())
            {
                if (gattHashMap.containsKey(address))
                {
                    gatt = gattHashMap[address]
                    UULog.d(javaClass, "gattForPeripheral", "Found existing gatt for $address")
                }

                if (gatt == null)
                {
                    gatt = UUBluetoothGatt(ctx, peripheral)
                    UULog.d(javaClass, "gattForPeripheral", "Creating new gatt for $address")
                    gattHashMap[address] = gatt
                }
            }

            return gatt
        }
    }

    override fun close()
    {
        UULog.d(javaClass, "close", "Closing Gatt from Closable")
        closeGatt()

//        try
//        {
//            val gatt = bluetoothGatt
//            if (gatt != null)
//            {
//                gatt.close()
//            }
//
//            //bluetoothGatt?.close()
//            //bluetoothGatt = null
//        }
//        catch (ex: Exception)
//        {
//            UULog.d(javaClass, "close", "", ex)
//        }
//        finally
//        {
//            bluetoothGatt = null
//        }
    }
}