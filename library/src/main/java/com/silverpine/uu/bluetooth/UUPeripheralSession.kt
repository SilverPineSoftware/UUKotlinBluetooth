package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import com.silverpine.uu.bluetooth.internal.safeNotify
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUObjectErrorBlock
import com.silverpine.uu.core.UUTimedMetric
import com.silverpine.uu.core.uuReadInt16
import com.silverpine.uu.core.uuReadInt32
import com.silverpine.uu.core.uuReadInt64
import com.silverpine.uu.core.uuReadInt8
import com.silverpine.uu.core.uuReadUInt16
import com.silverpine.uu.core.uuReadUInt32
import com.silverpine.uu.core.uuReadUInt64
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuToHex
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

private const val LOG_TAG = "UUPeripheralSession"

typealias UUPeripheralSessionStartedCallback = ((UUPeripheralSession) -> Unit)
typealias UUPeripheralSessionObjectErrorCallback<T> = ((UUPeripheralSession, T?, UUError?) -> Unit)
typealias UUPeripheralSessionErrorCallback = ((UUPeripheralSession, UUError?) -> Unit)

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
        UULog.debug(LOG_TAG, "end, Session ending with error: $error")

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

        UULog.debug(LOG_TAG, "write, TX (${data.size}) [${data.uuToHex()}] to $characteristic, withResponse: $withResponse")

        val writeType = if (withResponse)
        {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        else
        {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        peripheral.write(
            data = data,
            characteristic = char,
            timeout = configuration.writeTimeout,
            writeType = writeType,
            completion =
            { error: UUError? ->
                completion.safeNotify(this, error)
            })

        /*
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
        }*/
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

    fun requestConnectionPriority(priority: Int, completion: UUObjectErrorBlock<Boolean>)
    {
        peripheral.requestConnectionPriority(priority, completion)
    }

    fun requestMtuSize(mtuSize: Int, timeout: Long, completion: UUObjectErrorBlock<Int>)
    {
        peripheral.requestMtu(mtuSize, timeout, completion)
    }

    fun updatePhy(txPhy: Int,
                  rxPhy: Int,
                  phyOptions: Int,
                  timeout: Long,
                  completion: UUObjectErrorBlock<Pair<Int, Int>>)
    {
        peripheral.updatePhy(txPhy, rxPhy, phyOptions, timeout, completion)
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
        peripheral.disconnect(null)
    }

    private fun handleConnected()
    {
        connectTimeMeasurement.end()

        setupConnection()
        {
            startServiceDiscovery()
        }
    }

    open fun setupConnection(completion: () -> Unit)
    {
        completion()
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
        UULog.debug(LOG_TAG, "discoverServices, Discovered ${discoveredServices.size} services.")
        discoveredServices.forEach()
        { service ->
            //let serviceDescription = UUServiceRepresentation(from: service)
            //UULog.debug(tag: LOG_TAG, message: "Service: \(serviceDescription.uuToJsonString())")
            UULog.debug(LOG_TAG, "discoverServices, Service: ${service.uuid}, ${service.uuid.uuCommonName}")
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
            UULog.debug(LOG_TAG, "discoverCharacteristics, Discovered ${characteristics.size} characteristics on service $service")

            characteristics.forEach()
            { characteristic ->
                //let characteristicDescription = UUCharacteristicRepresentation(from: characteristic)
                //UULog.debug(tag: LOG_TAG, message: "Characteristic: \(characteristicDescription.uuToJsonString())")

                UULog.debug(LOG_TAG, "discoverCharacteristics, Characteristic: ${characteristic.uuid}, ${characteristic.uuid.uuCommonName}")
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
            UULog.debug(LOG_TAG, "discoverDescriptors, Discovered ${descriptors.size} descriptors on characteristic $characteristic")

            descriptors.forEach()
            { descriptor ->
                //let descriptorDescription = UUDescriptorRepresentation(from: descriptor)
                //UULog.debug(tag: LOG_TAG, message: "Descriptor: \(descriptorDescription.uuToJsonString())")

                UULog.debug(LOG_TAG, "discoverDescriptors, Descriptor: ${descriptor.uuid}, ${descriptor.uuid.uuCommonName}")
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

        val result = data?.uuReadUInt8(0)?.getOrNull()
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

        val result = data?.uuReadUInt16(byteOrder, 0)?.getOrNull()
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

        val result = data?.uuReadUInt32(byteOrder, 0)?.getOrNull()
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

        val result = data?.uuReadUInt64(byteOrder, 0)?.getOrNull()
        completion(p, result, null)
    }
}

fun UUPeripheralSession.readByte(
    characteristic: UUID,
    completion: UUPeripheralSessionObjectErrorCallback<Byte>)
{
    read(characteristic)
    { p, data, err ->

        val result = data?.uuReadInt8(0)?.getOrNull()
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

        val result = data?.uuReadInt16(byteOrder, 0)?.getOrNull()
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

        val result = data?.uuReadInt32(byteOrder, 0)?.getOrNull()
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

        val result = data?.uuReadInt64(byteOrder, 0)?.getOrNull()
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
    buffer.uuWriteUInt8(0, data)
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
    buffer.uuWriteUInt16(byteOrder, 0, data)
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
    buffer.uuWriteUInt32(byteOrder, 0, data)
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
    buffer.uuWriteUInt64(byteOrder, 0, data)
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
