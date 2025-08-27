package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import com.silverpine.uu.bluetooth.internal.safeNotify
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimedMetric
import com.silverpine.uu.core.uuReadUInt16
import com.silverpine.uu.core.uuReadUInt32
import com.silverpine.uu.core.uuReadUInt64
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuWriteInt16
import com.silverpine.uu.core.uuWriteInt32
import com.silverpine.uu.core.uuWriteInt64
import com.silverpine.uu.core.uuWriteInt8
import com.silverpine.uu.core.uuWriteUInt16
import com.silverpine.uu.core.uuWriteUInt32
import com.silverpine.uu.core.uuWriteUInt64
import com.silverpine.uu.core.uuWriteUInt8
import com.silverpine.uu.logging.UULog
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias UUPeripheralSessionStartedCallback = ((UUPeripheralSession) -> Unit)
typealias UUPeripheralSessionObjectErrorCallback<T> = ((UUPeripheralSession, T?, UUError?) -> Unit)
typealias UUPeripheralSessionErrorCallback = ((UUPeripheralSession, UUError?) -> Unit)

/*
/**
 * Defines a session for interacting with a single UUPeripheral.
 * Implementers should provide a constructor accepting a UUPeripheral.
 */
interface UUPeripheralSession
{
    /** The peripheral this session operates on. */
    val peripheral: UUPeripheral

    /** Configuration for this session. */
    var configuration: UUPeripheralSessionConfiguration

    /** Services discovered on the peripheral. */
    val discoveredServices: List<BluetoothGattService>

    /** Characteristics discovered per service UUID. */
    val discoveredCharacteristics: Map<UUID, List<BluetoothGattCharacteristic>>

    /** Descriptors discovered per characteristic UUID. */
    val discoveredDescriptors: Map<UUID, List<BluetoothGattDescriptor>>

    /** Error that ended the session, if any. */
    val sessionEndError: UUError?

    /** Callback invoked when the session has started. */
    var started: UUPeripheralSessionStartedCallback?

    /** Callback invoked when the session has ended, with optional error. */
    var ended: UUPeripheralSessionErrorCallback?

    /** Begin the session (discover services, etc.). */
    fun start()

    /** End the session, optionally with an error. */
    fun end(error: UUError?)

    fun startTimer(name: String, timeout: Long, block: ()->Unit)

    fun cancelTimer(name: String)

    /**
     * Read data from the specified characteristic.
     * @param characteristic UUID of the characteristic to read.
     * @param completion Called with the read bytes, or null on failure.
     */
    fun read(
        characteristic: UUID,
        completion: UUPeripheralSessionObjectErrorCallback<ByteArray>)

    /**
     * Write data to a characteristic.
     * @param data Bytes to write.
     * @param characteristic UUID of the target characteristic.
     * @param withResponse True to request write-with-response, false for without-response.
     * @param completion Invoked when the write is complete.
     */
    fun write(
        data: ByteArray,
        characteristic: UUID,
        withResponse: Boolean,
        completion: UUPeripheralSessionErrorCallback)

    /**
     * Start listening for changes on a characteristic.
     * @param characteristic UUID to monitor.
     * @param dataChanged Called when new data arrives.
     * @param completion Called once notification is set up.
     */
    fun startListeningForDataChanges(
        characteristic: UUID,
        dataChanged: UUPeripheralSessionObjectErrorCallback<ByteArray>,
        completion: UUPeripheralSessionErrorCallback)

    /**
     * Stop listening for changes on a characteristic.
     * @param characteristic UUID to stop monitoring.
     * @param completion Called when notifications are torn down.
     */
    fun stopListeningForDataChanges(
        characteristic: UUID,
        completion: UUPeripheralSessionErrorCallback)
}
*/



open class UUPeripheralSession(val peripheral: UUPeripheral)
{
    var configuration: UUPeripheralSessionConfiguration = UUPeripheralSessionConfiguration()
    var discoveredServices: ArrayList<BluetoothGattService> = arrayListOf()
        protected set

    var discoveredCharacteristics: ConcurrentHashMap<UUID, List<BluetoothGattCharacteristic>> =
        ConcurrentHashMap()
        protected set

    var discoveredDescriptors: ConcurrentHashMap<UUID, List<BluetoothGattDescriptor>> =
        ConcurrentHashMap()
        protected set

    var sessionEndError: UUError? = null
        protected set

    var started: UUPeripheralSessionStartedCallback? = null
    var ended: UUPeripheralSessionErrorCallback? = null

    private val connectTimeMeasurement = UUTimedMetric("connectTime")
    private val disconnectTimeMeasurement = UUTimedMetric("disconnectTime")
    private val serviceDiscoveryTimeMeasurement = UUTimedMetric("serviceDiscoveryTime")
    private val characteristicDiscoveryTimeMeasurement = UUTimedMetric("characteristicDiscoveryTime")
    private val descriptorDiscoveryTimeMeasurement = UUTimedMetric("descriptorDiscoveryTime")

    open fun start()
    {
        connect()
    }

    open fun end(error: UUError?)
    {
        UULog.d(javaClass, "end", "Session ending with error: $error")

        sessionEndError = error
        disconnect()
    }

    fun startTimer(name: String, timeout: Long, block: () -> Unit)
    {
        peripheral.startTimer(name, timeout, block)
    }

    fun cancelTimer(name: String)
    {
        peripheral.cancelTimer(name)
    }

    fun read(
        characteristic: UUID,
        completion: UUPeripheralSessionObjectErrorCallback<ByteArray>)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, null, err)
            return
        }

        peripheral.read(
            characteristic = char,
            timeout = configuration.readTimeout,
            completion =
                { data, error ->
                    completion.safeNotify(this, data, error)
                })
    }

    fun write(
        data: ByteArray,
        characteristic: UUID,
        withResponse: Boolean,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, err)
            return
        }

        if (withResponse)
        {
            peripheral.write(
                data = data,
                characteristic = char,
                timeout = configuration.writeTimeout,
                completion =
                    { error: UUError? ->
                        completion.safeNotify(this, error)
                    })
        }
        else
        {
            peripheral.writeWithoutResponse(
                data = data,
                characteristic = char,
                completion =
                    { error: UUError? ->
                        completion.safeNotify(this, error)
                    })
        }
    }

    fun startListeningForDataChanges(
        characteristic: UUID,
        dataChanged: UUPeripheralSessionObjectErrorCallback<ByteArray>,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, err)
            return
        }

        peripheral.setNotifyValue(
            enabled = true,
            characteristic = char,
            timeout = configuration.readTimeout,
            notifyHandler =
                { updatedData ->

                    //TODO: Update char in discovered map?

                    dataChanged.safeNotify(this, updatedData, null)
                },
            completion =
                { error ->
                    completion.safeNotify(this, error)
                }
        )
    }

    fun stopListeningForDataChanges(
        characteristic: UUID,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion(this, err)
            return
        }

        peripheral.setNotifyValue(
            enabled = true,
            characteristic = char,
            timeout = configuration.readTimeout,
            notifyHandler = null,
            completion =
                { error ->
                    completion(this, error)
                }
        )
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection & Disconnection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun connect()
    {
        connectTimeMeasurement.start()

        peripheral.connect(
            timeout = configuration.connectTimeout,
            connected = this::handleConnected,
            disconnected = this::handleDisconnection)
    }

    private fun handleSessionStarted()
    {
        finishSessionStart()
        {
            started?.safeNotify(this)
        }
    }

    private fun disconnect()
    {
        disconnectTimeMeasurement.start()
        peripheral.disconnect(null) //timeout = configuration.disconnectTimeout)
    }

    private fun handleConnected()
    {
        connectTimeMeasurement.end()
        startServiceDiscovery()
    }

    private fun handleDisconnection(disconnectError: UUError?)
    {
        disconnectTimeMeasurement.end()

        // Only set error if not already set.  In the case where end(error) forcefully ends the session, preserve that error.
        if (sessionEndError != null)
        {
            sessionEndError = disconnectError
        }

        ended?.safeNotify(this, sessionEndError)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Service Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startServiceDiscovery()
    {
        serviceDiscoveryTimeMeasurement.start()

        discoveredServices.clear()

        peripheral.discoverServices(timeout = configuration.serviceDiscoveryTimeout)
        { services, error ->

            serviceDiscoveryTimeMeasurement.end()

            error?.let()
            { err ->
                end(err)
                return@discoverServices
            }

            if (services == null)
            {
                val err = UUBluetoothError.makeError(UUBluetoothErrorCode.NoServicesDiscovered)
                end(err)
                return@discoverServices
            }

            discoveredServices.addAll(services)
            logDiscoveredServices()
            startCharacteristicDiscovery()
        }
    }

    private fun logDiscoveredServices()
    {
        UULog.d(javaClass, "discoverServices", "Discovered ${discoveredServices.size} services.")
        discoveredServices.forEach()
        { service ->
            //let serviceDescription = UUServiceRepresentation(from: service)
            //UULog.debug(tag: LOG_TAG, message: "Service: \(serviceDescription.uuToJsonString())")
            UULog.d(javaClass, "discoverServices", "Service: ${service.uuid}, ${service.uuid.uuCommonName}")
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Characteristic Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startCharacteristicDiscovery()
    {
        characteristicDiscoveryTimeMeasurement.start()
        discoveredCharacteristics.clear()

        discoveredServices.forEach()
        { service ->
            discoveredCharacteristics[service.uuid] = service.characteristics
        }

        characteristicDiscoveryTimeMeasurement.end()
        logDiscoveredCharacteristics()

        startDescriptorDiscovery()
    }

    private fun logDiscoveredCharacteristics()
    {
        discoveredCharacteristics.forEach()
        { service, characteristics ->
            UULog.d(javaClass, "discoverCharacteristics", "Discovered ${characteristics.size} characteristics on service $service")

            characteristics.forEach()
            { characteristic ->
                //let characteristicDescription = UUCharacteristicRepresentation(from: characteristic)
                //UULog.debug(tag: LOG_TAG, message: "Characteristic: \(characteristicDescription.uuToJsonString())")

                UULog.d(javaClass, "discoverCharacteristics", "Characteristic: ${characteristic.uuid}, ${characteristic.uuid.uuCommonName}")
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Descriptor Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startDescriptorDiscovery()
    {
        descriptorDiscoveryTimeMeasurement.start()
        discoveredDescriptors.clear()

        discoveredCharacteristics.values.flatten().forEach()
        { characteristic ->
            discoveredDescriptors[characteristic.uuid] = characteristic.descriptors
        }

        descriptorDiscoveryTimeMeasurement.end()
        logDiscoveredDescriptors()

        handleSessionStarted()
    }

    private fun logDiscoveredDescriptors()
    {
        discoveredDescriptors.forEach()
        { characteristic, descriptors ->
            UULog.d(javaClass, "discoverDescriptors", "Discovered ${descriptors.size} descriptors on characteristic $characteristic")

            descriptors.forEach()
            { descriptor ->
                //let descriptorDescription = UUDescriptorRepresentation(from: descriptor)
                //UULog.debug(tag: LOG_TAG, message: "Descriptor: \(descriptorDescription.uuToJsonString())")

                UULog.d(javaClass, "discoverDescriptors", "Descriptor: ${descriptor.uuid}, ${descriptor.uuid.uuCommonName}")
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Additional Private Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun findDiscoveredCharacteristic(uuid: UUID): BluetoothGattCharacteristic?
    {
        return discoveredCharacteristics.values.flatten().firstOrNull { it.uuid == uuid }
    }

    open fun finishSessionStart(completion: ()->Unit)
    {
        completion()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Read Extensions
////////////////////////////////////////////////////////////////////////////////////////////////////

fun UUPeripheralSession.readString(
    characteristic: UUID,
    encoding: Charset,
    completion: UUPeripheralSessionObjectErrorCallback<String>)
{
    read(characteristic)
    { p, data, err ->

        var result: String? = null

        data?.let()
        { d ->
            result = String(d, encoding)

        }

        completion(p, result, null)
    }
}

fun UUPeripheralSession.readUtf8(
    characteristic: UUID,
    completion: UUPeripheralSessionObjectErrorCallback<String>)
{
    readString(characteristic, encoding = Charsets.UTF_8, completion)
}

fun UUPeripheralSession.readUByte(
    characteristic: UUID,
    completion: UUPeripheralSessionObjectErrorCallback<UByte>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt8(0)?.toUByte()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readUShort(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<UShort>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt16(byteOrder, 0)?.toUShort()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readUInt(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<UInt>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt32(byteOrder, 0)?.toUInt()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readULong(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<ULong>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt64(byteOrder, 0)?.toULong()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readByte(
    characteristic: UUID,
    completion: UUPeripheralSessionObjectErrorCallback<Byte>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt8(0)?.toByte()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readShort(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<Short>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt16(byteOrder, 0)?.toShort()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readInt(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<Int>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt32(byteOrder, 0)?.toInt()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readLong(
    characteristic: UUID,
    byteOrder: ByteOrder,
    completion: UUPeripheralSessionObjectErrorCallback<Long>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadUInt64(byteOrder, 0)?.toLong()
        completion(p, result, null)
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Write Extensions
////////////////////////////////////////////////////////////////////////////////////////////////////

fun UUPeripheralSession.write(
    string: String,
    encoding: Charset,
    characteristic: UUID,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    write(
        data = string.toByteArray(encoding),
        characteristic = characteristic,
        withResponse = withResponse,
        completion = completion)
}

fun UUPeripheralSession.writeUtf8(
    string: String,
    characteristic: UUID,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    write(string, Charsets.UTF_8, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeUByte(
    data: UByte,
    characteristic: UUID,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(UByte.SIZE_BYTES)
    buffer.uuWriteUInt8(0, data.toInt())
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeUShort(
    data: UShort,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(UShort.SIZE_BYTES)
    buffer.uuWriteUInt16(byteOrder, 0, data.toInt())
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeUInt(
    data: UInt,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(UInt.SIZE_BYTES)
    buffer.uuWriteUInt32(byteOrder, 0, data.toLong())
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeULong(
    data: ULong,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(ULong.SIZE_BYTES)
    buffer.uuWriteUInt64(byteOrder, 0, data.toLong())
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeByte(
    data: Byte,
    characteristic: UUID,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(Byte.SIZE_BYTES)
    buffer.uuWriteInt8(0, data)
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeShort(
    data: Short,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(Short.SIZE_BYTES)
    buffer.uuWriteInt16(byteOrder, 0, data)
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeInt(
    data: Int,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(Int.SIZE_BYTES)
    buffer.uuWriteInt32(byteOrder, 0, data)
    write(buffer, characteristic, withResponse, completion)
}

fun UUPeripheralSession.writeLong(
    data: Long,
    characteristic: UUID,
    byteOrder: ByteOrder,
    withResponse: Boolean,
    completion: UUPeripheralSessionErrorCallback)
{
    val buffer = ByteArray(Long.SIZE_BYTES)
    buffer.uuWriteInt64(byteOrder, 0, data.toLong())
    write(buffer, characteristic, withResponse, completion)
}
