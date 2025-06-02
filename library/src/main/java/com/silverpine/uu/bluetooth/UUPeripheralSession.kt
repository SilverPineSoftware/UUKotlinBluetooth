package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.core.UUError
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
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID

//typealias UUPeripheralSessionStartedCallback = ((UUPeripheralSession) -> Unit)
//typealias UUPeripheralSessionEndedCallback = ((UUPeripheralSession, UUError?) -> Unit)
//typealias UUByteArrayCallback = ((ByteArray?) -> Unit)
//typealias UUStringCallback = ((String?) -> Unit)
//typealias UUVoidCallback = ()->Unit
//typealias UUSessionErrorHandler = ((UUError) -> Boolean)
//typealias UUUByteCallback = ((UByte?) -> Unit)
//typealias UUUShortCallback = ((UShort?) -> Unit)
//typealias UUUIntCallback = ((UInt?) -> Unit)
//typealias UUULongCallback = ((ULong?) -> Unit)
//typealias UUByteCallback = ((Byte?) -> Unit)
//typealias UUShortCallback = ((Short?) -> Unit)
//typealias UUIntCallback = ((Int?) -> Unit)
//typealias UULongCallback = ((Long?) -> Unit)

typealias UUPeripheralSessionStartedCallback = ((UUPeripheralSession) -> Unit)
//typealias UUPeripheralSessionEndedCallback = ((UUPeripheralSession, UUError?) -> Unit)
typealias UUPeripheralSessionObjectErrorCallback<T> = ((UUPeripheralSession, T?, UUError?) -> Unit)
typealias UUPeripheralSessionErrorCallback = ((UUPeripheralSession, UUError?) -> Unit)

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
