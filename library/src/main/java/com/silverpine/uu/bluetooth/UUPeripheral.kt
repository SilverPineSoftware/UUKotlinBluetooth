package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Parcelable
import com.silverpine.uu.bluetooth.UUBluetooth.connectionStateToString
import com.silverpine.uu.bluetooth.UUBluetooth.gattStatusToString
import com.silverpine.uu.bluetooth.UUBluetoothConstants.DEFAULT_MTU
import com.silverpine.uu.bluetooth.internal.safeNotify
import com.silverpine.uu.bluetooth.internal.uuHashLookup
import com.silverpine.uu.bluetooth.internal.uuToLowercaseString
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.io.Closeable
import java.util.Locale
import java.util.UUID

typealias UUPeripheralConnectedBlock = (()->Unit)
typealias UUPeripheralDisconnectedBlock = ((UUError?)->Unit)

@SuppressLint("MissingPermission")
class UUPeripheral(
    val advertisement: UUAdvertisement
): Closeable
{
    companion object
    {
        var deviceCache: UUBluetoothDeviceCache = UUInMemoryBluetoothDeviceCache
        val gattCache: UUBluetoothGattCache = UUInMemoryBluetoothGattCache
    }

    /*
    init
    {
        debugLog("init", "Creating peripheral for ${advertisement.address} - ${advertisement.localName}")
    }
    */

    internal val rootTimerId: String = advertisement.address

    private val bluetoothGatt: BluetoothGatt?
        get()
        {
            return gattCache[identifier]
        }

    private val bluetoothGattCallback: UUBluetoothGattCallback = UUBluetoothGattCallback()

    private var disconnectError: UUError? = null

    private val isConnecting: Boolean
        get() = (bluetoothGatt != null && isConnectWatchdogActive)

    private val isConnectWatchdogActive: Boolean
        get() = (UUTimer.findActiveTimer(connectWatchdogTimerId) != null)

    private var disconnectTimeout: Long = 10000L

    private var disconnectedCallback: UUPeripheralDisconnectedBlock? = null

    var rssi: Int = advertisement.rssi
    var firstDiscoveryTime: Long = 0L
    var identifier: String = advertisement.address
    var name: String = deviceCache[identifier]?.name ?: ""
    var services: List<BluetoothGattService>? = null
    var mtuSize: Int = DEFAULT_MTU
    var txPhy: Int? = null
    var rxPhy: Int? = null
    var peripheralState: UUPeripheralConnectionState = UUPeripheralConnectionState.Undetermined

    val timeSinceLastUpdate: Long
        get() = System.currentTimeMillis() - advertisement.timestamp

    var userInfo: Parcelable? = null

    fun refreshConnectionState()
    {
        val bluetoothDevice = deviceCache[identifier]
        if (bluetoothDevice == null)
        {
            debugLog("connect", "WARNING -- Bluetooth Device not found $identifier")
            peripheralState = UUPeripheralConnectionState.Undetermined
            return
        }

        val bluetoothManager = UUBluetooth.requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var state = bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT)
        debugLog("getConnectionState", "Actual connection state is: $state (${UUPeripheralConnectionState.fromProfileConnectionState(state)})")
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

        peripheralState = UUPeripheralConnectionState.fromProfileConnectionState(state)
    }

    fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock)
    {
        if (bluetoothGatt != null)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt is already connected to $identifier")
            disconnected.safeNotify(UUBluetoothError.alreadyConnectedError())
            return
        }

        val bluetoothDevice = deviceCache[identifier]
        if (bluetoothDevice == null)
        {
            debugLog("connect", "ERROR -- Bluetooth Device not found $identifier")
            disconnected.safeNotify(UUBluetoothError.preconditionFailedError("Bluetooth device is null!"))
            return
        }

        if (isConnectWatchdogActive)
        {
            debugLog("connect", "WARNING -- Bluetooth Gatt making connection attempt to $identifier")
            disconnected.safeNotify(UUBluetoothError.alreadyConnectedError())
            return
        }

        val timerId = connectWatchdogTimerId

        disconnectedCallback =
        { error ->
            debugLog("connect", "Disconnected from: $identifier, error: $error")
            notifyDisconnection(disconnected, error)
        }

        bluetoothGattCallback.connectionStateChangedCallback =
        { result ->
            val status = result.first
            val newState = result.second
            peripheralState = UUPeripheralConnectionState.fromProfileConnectionState(newState)

            debugLog(
                "onConnectionStateChanged", String.format(
                    Locale.US, "status: %s, newState: %s (%d)",
                    statusLog(status), connectionStateToString(newState), newState))

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED)
            {
                debugLog("connect", "Connected to: $identifier")
                cancelTimer(timerId)
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

        startDisconnectWatchdogTimer(timerId, timeout)

        val connectGattAutoFlag = false

        uuDispatchMain()
        {
            debugLog("connect", "Connecting to: $identifier, gattAuto: $connectGattAutoFlag")

            disconnectError = UUBluetoothError.connectionFailedError()

            val context = UUBluetooth.requireApplicationContext()

            val gatt = bluetoothDevice.connectGatt(
                context,
                connectGattAutoFlag,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            gattCache[identifier] = gatt
        }
    }

    fun discoverServices(
        timeout: Long,
        completion: UUListErrorBlock<BluetoothGattService>)
    {
        val timerId = serviceDiscoveryWatchdogTimerId

        bluetoothGattCallback.servicesDiscoveredCallback =
        { services, error ->
            debugLog("discoverServices", "Service Discovery complete: $identifier, error: $error")
            this.services = services
            cancelTimer(timerId)
            completion(services, error)
        }

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("discoverServices", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyServicesDiscovered(null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("discoverServices", "Discovering services for: $identifier")
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
        notifyHandler: UUObjectBlock<ByteArray>?,
        completion: UUErrorBlock)
    {
        val identifier = characteristic.uuHashLookup()
        val timerId = setNotifyStateWatchdogTimerId(characteristic)

        val callback: UUErrorBlock =
        { error ->

            debugLog(
                "setNotifyState",
                "Set characteristic notify complete: $identifier, error: $error}")
            bluetoothGattCallback.clearSetCharacteristicNotificationCallback(identifier)
            UUTimer.cancelActiveTimer(timerId)
            completion.safeNotify(error)
        }

        bluetoothGattCallback.registerSetCharacteristicNotificationCallback(identifier, callback)

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("setNotifyState", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("setNotifyState", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        val descriptorUuid = UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
        val descriptor = chr.getDescriptor(descriptorUuid)
        if (descriptor == null)
        {
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, UUBluetoothError.missingRequiredDescriptor(descriptorUuid))
            return
        }

        if (enabled && notifyHandler != null)
        {
            bluetoothGattCallback.registerCharacteristicDataChangedCallback(identifier, notifyHandler)
        }
        else
        {
            bluetoothGattCallback.clearCharacteristicDataChangedCallback(identifier)
        }

        val start = System.currentTimeMillis()
        uuDispatchMain()
        {
            debugLog("toggleNotifyState", "Setting characteristic notify for ${characteristic.uuid}")

            val success = gatt.setCharacteristicNotification(chr, enabled)
            debugLog("toggleNotifyState", "setCharacteristicNotification returned $success")
            if (!success)
            {
                bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, UUBluetoothError.operationFailedError("setCharacteristicNotification"))
                return@uuDispatchMain
            }

            val data = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            val timeoutLeft = timeout - (System.currentTimeMillis() - start)
            write(data, descriptor, timeoutLeft)
            { error ->

                bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, error)
            }
        }
    }

    fun isNotifying(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUObjectErrorBlock<Boolean>)
    {
        val descriptorUuid = UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
        val descriptor = characteristic.getDescriptor(descriptorUuid)

        if (descriptor == null)
        {
            debugLog("isNotifying", "descriptor is null!")
            bluetoothGattCallback.notifyCharacteristicSetNotifyCallback(identifier, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        read(descriptor, timeout)
        { result, error ->
            val isNotifying = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.contentEquals(result)
            completion(isNotifying, error)
        }
    }

    fun read(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUObjectErrorBlock<ByteArray>)
    {
        val identifier = characteristic.uuHashLookup()
        val timerId = readCharacteristicWatchdogTimerId(characteristic)

        val callback: UUObjectErrorBlock<ByteArray> =
        { data: ByteArray?,
          error: UUError? ->
            debugLog(
                "read:characteristic",
                "Read characteristic complete: $identifier, error: $error, data: ${data?.uuToHex()}")
            cancelTimer(timerId)
            bluetoothGattCallback.clearReadCharacteristicCallback(identifier)
            completion(data, error)
        }

        bluetoothGattCallback.registerReadCharacteristicCallback(identifier, callback)

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("read:characteristic", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicRead(identifier, null, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("read:characteristic", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicRead(identifier, null, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("read:characteristic", "characteristic: $characteristic")
            val success = gatt.readCharacteristic(chr)
            debugLog("read:characteristic", "readCharacteristic returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyCharacteristicRead(identifier, null, UUBluetoothError.operationFailedError("read:characteristic"))
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun read(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUObjectErrorBlock<ByteArray>)
    {
        val identifier = descriptor.uuHashLookup()
        val timerId = readDescriptorWatchdogTimerId(descriptor)

        val callback: UUObjectErrorBlock<ByteArray> =
        { data: ByteArray?,
          error: UUError? ->
            debugLog(
                "read:descriptor",
                "Read descriptor complete: $identifier, error: $error, data: ${data?.uuToHex()}")
            cancelTimer(timerId)
            bluetoothGattCallback.clearReadDescriptorCallback(identifier)
            completion(data, error)
        }

        bluetoothGattCallback.registerReadDescriptorCallback(identifier, callback)

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("read:descriptor", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyDescriptorRead(identifier, null, UUBluetoothError.notConnectedError())
            return
        }

        val desc = gatt.uuLookupDescriptor(descriptor)

        if (desc == null)
        {
            debugLog("read:descriptor", "descriptor is null!")
            bluetoothGattCallback.notifyDescriptorRead(identifier, null, UUBluetoothError.missingRequiredDescriptor(descriptor.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("read:descriptor", "descriptor: ${descriptor.uuid}")
            val success = gatt.readDescriptor(desc)
            debugLog("read:descriptor", "readDescriptor returned $success")

            if (!success)
            {
                bluetoothGattCallback.notifyDescriptorRead(identifier, null, UUBluetoothError.operationFailedError("read:descriptor"))
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
        writeType: Int,
        completion: UUErrorBlock)
    {
        val identifier = characteristic.uuHashLookup()
        val timerId = writeCharacteristicWatchdogTimerId(characteristic)

        val callback: UUErrorBlock =
        { error: UUError? ->
            debugLog(
                "write:characteristic",
                "Write characteristic complete: $identifier, error: $error")
            cancelTimer(timerId)
            bluetoothGattCallback.clearWriteCharacteristicCallback(identifier)
            completion.safeNotify(error)
        }

        bluetoothGattCallback.registerWriteCharacteristicCallback(identifier, callback)

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("write:characteristic", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyCharacteristicWrite(identifier, UUBluetoothError.notConnectedError())
            return
        }

        val chr = gatt.uuLookupCharacteristic(characteristic)

        if (chr == null)
        {
            debugLog("write:characteristic", "characteristic is null!")
            bluetoothGattCallback.notifyCharacteristicWrite(identifier, UUBluetoothError.missingRequiredCharacteristic(characteristic.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("write:characteristic", "characteristic: ${characteristic.uuid}, tx: ${data.uuToHex()}")

            val err = gatt.uuWrite(data, chr, writeType) //BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            if (err != null)
            {
                bluetoothGattCallback.notifyCharacteristicWrite(identifier, err)
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    /*
    fun writeWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUErrorBlock)
    {
        val identifier = characteristic.uuHashLookup()

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
                bluetoothGattCallback.notifyCharacteristicWrite(identifier, err)
            }

            // Notify completion always.  There is no timeout or callback
            completion.safeNotify(err)
        }
    }*/

    fun write(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUErrorBlock)
    {
        val identifier = descriptor.uuHashLookup()
        val timerId = writeDescriptorWatchdogTimerId(descriptor)

        val callback: UUErrorBlock =
        { error: UUError? ->
            debugLog(
                "write:descriptor",
                "Write descriptor complete: $identifier, error: $error")
            cancelTimer(timerId)
            bluetoothGattCallback.clearWriteDescriptorCallback(identifier)
            completion.safeNotify(error)
        }

        bluetoothGattCallback.registerWriteDescriptorCallback(identifier, callback)

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("write:descriptor", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyDescriptorWrite(identifier, UUBluetoothError.notConnectedError())
            return
        }

        val desc = gatt.uuLookupDescriptor(descriptor)

        if (desc == null)
        {
            debugLog("write:descriptor", "descriptor is null!")
            bluetoothGattCallback.notifyDescriptorWrite(identifier, UUBluetoothError.missingRequiredDescriptor(descriptor.uuid))
            return
        }

        uuDispatchMain()
        {
            debugLog("write:descriptor", "descriptor: ${descriptor.uuid}, tx: ${data.uuToHex()}")

            val err = gatt.uuWrite(data, desc)
            if (err != null)
            {
                bluetoothGattCallback.notifyDescriptorWrite(identifier, err)
            }
            // else
            //
            // wait for delegate or timeout
        }
    }

    fun readRSSI(
        timeout: Long,
        completion: UUObjectErrorBlock<Int>)
    {
        val timerId = readRssiWatchdogTimerId

        val callback: UUObjectErrorBlock<Int> =
        { data, error ->
            debugLog(
                "readRSSI",
                "Read RSSI complete: $identifier, error: $error, data: $data")
            cancelTimer(timerId)
            bluetoothGattCallback.readRssiCallback = null
            completion(data, error)
        }

        bluetoothGattCallback.readRssiCallback = callback

        startDisconnectWatchdogTimer(timerId, timeout)

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
        completion: UUObjectErrorBlock<Int>)
    {
        val timerId = requestMtuWatchdogTimerId

        val callback: UUObjectErrorBlock<Int> =
        { data, error ->
            debugLog(
                "requestMtu",
                "Request MTU complete: $identifier, error: $error, data: $data")
            cancelTimer(timerId)
            bluetoothGattCallback.mtuChangedCallback = null
            completion(data, error)
        }

        bluetoothGattCallback.mtuChangedCallback = callback

        startDisconnectWatchdogTimer(timerId, timeout)

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

    fun readPhy(
        timeout: Long,
        completion: UUObjectErrorBlock<Pair<Int, Int>>)
    {
        val timerId = readPhyWatchdogTimerId

        val callback: UUObjectErrorBlock<Pair<Int, Int>> =
        { result, error ->
            debugLog(
                "readPhy",
                "Read Phy complete: $identifier, error: $error, txPhy: ${result?.first}, rxPhy: ${result?.second}")

            cancelTimer(timerId)
            bluetoothGattCallback.phyReadCallback = null

            completion(result, error)
        }

        bluetoothGattCallback.phyReadCallback = callback

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("readPhy", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyPhyRead(null, null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("readPhy", "Reading Phy")
            gatt.readPhy()
            debugLog("readPhy", "readPhy called")

            // wait for delegate or timeout
        }
    }

    fun updatePhy(
        txPhy: Int,
        rxPhy: Int,
        phyOptions: Int,
        timeout: Long,
        completion: UUObjectErrorBlock<Pair<Int, Int>>)
    {
        val timerId = updatePhyWatchdogTimerId

        val callback: UUObjectErrorBlock<Pair<Int, Int>> =
        { result, error ->
            debugLog(
                "updatePhy",
                "Update Phy complete: $identifier, error: $error, txPhy: ${result?.first}, rxPhy: ${result?.second}")
            cancelTimer(timerId)
            bluetoothGattCallback.phyUpdatedCallback = null
            completion(result, error)
        }

        bluetoothGattCallback.phyUpdatedCallback = callback

        startDisconnectWatchdogTimer(timerId, timeout)

        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("updatePhy", "bluetoothGatt is null!")
            bluetoothGattCallback.notifyPhyUpdate(null, null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("updatePhy", "Requesting Phy update, txPhy: $txPhy, rxPhy: $rxPhy, phyOptions: $phyOptions")
            gatt.setPreferredPhy(txPhy, rxPhy, phyOptions)
            debugLog("updatePhy", "setPreferredPhy called")

            // wait for delegate or timeout
        }
    }

    fun requestConnectionPriority(priority: Int, completion: UUObjectErrorBlock<Boolean>)
    {
        val gatt = bluetoothGatt

        if (gatt == null)
        {
            debugLog("requestConnectionPriority", "bluetoothGatt is null!")
            completion(null, UUBluetoothError.notConnectedError())
            return
        }

        uuDispatchMain()
        {
            debugLog("requestConnectionPriority", "Requesting connection priority: $priority")
            val result = gatt.requestConnectionPriority(priority)
            debugLog("requestHighPriority", "requestConnectionPriority returned $result")
            completion(result, null)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Closeable Implementation
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun close()
    {
        bluetoothGatt?.uuSafeClose()
        //bluetoothGatt = null
        gattCache[identifier] = null
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

    fun startDisconnectWatchdogTimer(timerId: String, timeout: Long)
    {
        startTimer(timerId, timeout)
        {
            disconnect(UUBluetoothError.timeoutError())
        }
    }

    fun startTimer(timerId: String, timeout: Long, block: ()->Unit)
    {
        UUTimer.startTimer(timerId, timeout, null)
        { _, _ ->
            block()
        }
    }

    fun cancelTimer(timerId: String)
    {
        UUTimer.cancelActiveTimer(timerId)
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

    private fun notifyDisconnection(callback: UUErrorBlock, error: UUError?)
    {
        cancelAllTimers()
        cleanupAfterDisconnect()

        bluetoothGatt?.uuSafeClose()
        gattCache[identifier] = null

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

            debugLog("disconnect", "Disconnect timeout: $identifier")
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
            debugLog("disconnectGatt", "Disconnecting from: $identifier, bluetoothGatt: $bluetoothGatt")
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











    fun createL2capChannel(psm: Int): BluetoothSocket?
    {
        return null

        /*
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            bluetoothDevice.createL2capChannel(psm)
        }
        else
        {
            null
        }*/
    }

    fun createInsecureL2capChannel(psm: Int): BluetoothSocket?
    {
        return null
        /*
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            bluetoothDevice.createInsecureL2capChannel(psm)
        }
        else
        {
            null
        }*/
    }


















    private enum class TimerBucket
    {
        Connect,
        ServiceDiscovery,
        CharacteristicNotifyState,
        ReadCharacteristic,
        WriteCharacteristic,
        ReadDescriptor,
        WriteDescriptor,
        ReadRssi,
        Disconnect,
        RequestMtu,
        ReadPhy,
        UpdatePhy
    }

    private fun timerId(bucket: TimerBucket): String
    {
        return String.format(Locale.US, "%s__%s", rootTimerId, bucket.name)
    }

    private fun timerId(uuid: UUID, bucket: TimerBucket): String
    {
        return String.format(Locale.US, "%s__%s__%s", rootTimerId, uuid.uuToLowercaseString(), bucket.name)
    }

    private val connectWatchdogTimerId: String
        get() = timerId(TimerBucket.Connect)

    private val disconnectWatchdogTimerId: String
        get() = timerId(TimerBucket.Disconnect)

    private val serviceDiscoveryWatchdogTimerId: String
        get() = timerId(TimerBucket.ServiceDiscovery)

    private fun setNotifyStateWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return timerId(characteristic.uuid, TimerBucket.CharacteristicNotifyState)
    }

    private fun readCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return timerId(characteristic.uuid, TimerBucket.ReadCharacteristic)
    }

    private fun readDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
    {
        return timerId(descriptor.uuid, TimerBucket.ReadDescriptor)
    }

    private fun writeCharacteristicWatchdogTimerId(characteristic: BluetoothGattCharacteristic): String
    {
        return timerId(characteristic.uuid, TimerBucket.WriteCharacteristic)
    }

    private fun writeDescriptorWatchdogTimerId(descriptor: BluetoothGattDescriptor): String
    {
        return timerId(descriptor.uuid, TimerBucket.WriteDescriptor)
    }

    private val readRssiWatchdogTimerId: String
        get() = timerId(TimerBucket.ReadRssi)

    private val requestMtuWatchdogTimerId: String
        get() = timerId(TimerBucket.RequestMtu)

    private val readPhyWatchdogTimerId: String
        get() = timerId(TimerBucket.ReadPhy)

    private val updatePhyWatchdogTimerId: String
        get() = timerId(TimerBucket.UpdatePhy)

}