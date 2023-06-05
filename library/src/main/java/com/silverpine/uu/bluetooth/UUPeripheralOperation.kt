package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUBluetoothError.operationFailedError
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuReadInt16
import com.silverpine.uu.core.uuReadInt32
import com.silverpine.uu.core.uuReadInt64
import com.silverpine.uu.core.uuReadInt8
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

abstract class UUPeripheralOperation<T : UUPeripheral?>(protected val peripheral: T)
{
    private var operationCallback: ((UUError?)->Unit)? = null
    private val discoveredServices = ArrayList<BluetoothGattService>()
    private val discoveredCharacteristics = ArrayList<BluetoothGattCharacteristic>()
    private val servicesNeedingCharacteristicDiscovery = ArrayList<BluetoothGattService>()
    var connectTimeout = UUPeripheral.Defaults.ConnectTimeout.toLong()
    var disconnectTimeout = UUPeripheral.Defaults.DisconnectTimeout.toLong()
    var serviceDiscoveryTimeout = UUPeripheral.Defaults.ServiceDiscoveryTimeout.toLong()
    private val readTimeout = UUPeripheral.Defaults.OperationTimeout.toLong()
    private val writeTimeout = UUPeripheral.Defaults.OperationTimeout.toLong()

    fun findDiscoveredService(uuid: UUID): BluetoothGattService?
    {
        for (service in discoveredServices)
        {
            if (service.uuid == uuid) {
                return service
            }
        }

        return null
    }

    fun findDiscoveredCharacteristic(uuid: UUID): BluetoothGattCharacteristic?
    {
        for (characteristic in discoveredCharacteristics)
        {
            if (characteristic.uuid == uuid)
            {
                return characteristic
            }
        }

        return null
    }

    fun requireDiscoveredService(uuid: UUID, completion: (BluetoothGattService)->Unit)
    {
        val discovered = findDiscoveredService(uuid)
        if (discovered == null)
        {
            val err = operationFailedError("requireDiscoveredService")
            end(err)
            return
        }

        completion.invoke(discovered)
    }

    fun requireDiscoveredCharacteristic(uuid: UUID, completion: (BluetoothGattCharacteristic)->Unit)
    {
        val discovered = findDiscoveredCharacteristic(uuid)
        if (discovered == null)
        {
            val err = operationFailedError("requireDiscoveredCharacteristic")
            end(err)
            return
        }

        completion.invoke(discovered)
    }

    fun write(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic)
        { characteristic ->

            peripheral!!.writeCharacteristic(characteristic, data, writeTimeout)
            { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null)
                {
                    end(error)
                    return@writeCharacteristic
                }

                completion.invoke()
            }
        }
    }

    fun wwor(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic)
        { characteristic ->
            peripheral!!.writeCharacteristicWithoutResponse(characteristic, data, writeTimeout)
            { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->

                if (error != null)
                {
                    end(error)
                    return@writeCharacteristicWithoutResponse
                }

                completion.invoke()
            }
        }
    }

    fun read(fromCharacteristic: UUID, completion: (ByteArray?)->Unit)
    {
        requireDiscoveredCharacteristic(fromCharacteristic)
        { characteristic ->
            peripheral!!.readCharacteristic(characteristic, readTimeout)
            { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->

                if (error != null)
                {
                    end(error)
                    return@readCharacteristic
                }

                completion.invoke(characteristic.value)
            }
        }
    }

    fun readString(
        fromCharacteristic: UUID,
        charset: Charset,
        completion: (String?)->Unit
    )
    {
        read(fromCharacteristic)
        { data ->

            var result: String? = null

            if (data != null)
            {
                result = String(data, charset)
            }

            completion.invoke(result)
        }
    }

    fun readUtf8(fromCharacteristic: UUID, completion: (String?)->Unit)
    {
        readString(fromCharacteristic, Charsets.UTF_8, completion)
    }

    fun readUInt8(fromCharacteristic: UUID, completion: (Int?)->Unit)
    {
        read(fromCharacteristic)
        { data ->
            var result: Int? = null
            if (data != null && data.size >= Byte.SIZE_BYTES)
            {
                result = data.uuReadUInt8(0)
            }

            completion.invoke(result)
        }
    }

    fun readUInt16(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Int?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Int? = null

            if (data != null && data.size >= Short.SIZE_BYTES)
            {
                result = data.uuReadUInt16(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun readUInt32(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Long?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Long? = null

            if (data != null && data.size >= Int.SIZE_BYTES)
            {
                result = data.uuReadUInt32(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun readUInt64(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Long?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Long? = null

            if (data != null && data.size >= Long.SIZE_BYTES)
            {
                result = data.uuReadUInt64(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun readInt8(fromCharacteristic: UUID, completion: (Byte?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Byte? = null

            if (data != null && data.size >= Byte.SIZE_BYTES)
            {
                result = data.uuReadInt8(0)
            }

            completion.invoke(result)
        }
    }

    fun readInt16(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Short?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Short? = null

            if (data != null && data.size >= Short.SIZE_BYTES)
            {
                result = data.uuReadInt16(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun readInt32(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Int?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Int? = null

            if (data != null && data.size >= Int.SIZE_BYTES)
            {
                result = data.uuReadInt32(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun readInt64(fromCharacteristic: UUID, byteOrder: ByteOrder, completion: (Long?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: Long? = null

            if (data != null && data.size >= Long.SIZE_BYTES)
            {
                result = data.uuReadInt64(byteOrder, 0)
            }

            completion.invoke(result)
        }
    }

    fun write(value: String, charset: Charset, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = value.toByteArray(charset)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUtf8(value: String, toCharacteristic: UUID, completion: ()->Unit)
    {
        write(value, Charsets.UTF_8, toCharacteristic, completion)
    }

    fun writeUInt8(value: Int, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Byte.SIZE_BYTES)
        buffer.uuWriteUInt8(0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt16(value: Int, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Short.SIZE_BYTES)
        buffer.uuWriteUInt16(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt32(value: Long, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Int.SIZE_BYTES)
        buffer.uuWriteUInt32(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt64(value: Long, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Long.SIZE_BYTES)
        buffer.uuWriteUInt64(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt8(value: Byte, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Byte.SIZE_BYTES)
        buffer.uuWriteInt8(0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt16(value: Short, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Short.SIZE_BYTES)
        buffer.uuWriteInt16(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt32(value: Int, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Int.SIZE_BYTES)
        buffer.uuWriteInt32(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt64(value: Long, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit)
    {
        val buffer = ByteArray(Long.SIZE_BYTES)
        buffer.uuWriteInt64(byteOrder, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun start(completion: (UUError?)->Unit)
    {
        operationCallback = completion
        peripheral!!.connect(connectTimeout, disconnectTimeout,
            { handleConnected() }) { disconnectError: UUError? ->
            handleDisconnection(
                disconnectError
            )
        }
    }

    fun end(error: UUError?)
    {
        //debug(javaClass, "end", "**** Ending Operation with error: " + UUString.safeToString(error))
        peripheral!!.disconnect(error)
    }

    abstract fun execute(completion: (UUError?)->Unit)

    private fun handleConnected()
    {
        peripheral?.discoverServices(serviceDiscoveryTimeout)
        { services, error ->

            if (error != null)
            {
                end(error)
                return@discoverServices
            }

            discoveredServices.clear()
            discoveredCharacteristics.clear()
            servicesNeedingCharacteristicDiscovery.clear()

            val services: List<BluetoothGattService> = peripheral.discoveredServices()
            if (services.isEmpty())
            {
                val err = operationFailedError("No Services Found")
                end(err)
                return@discoverServices
            }

            discoveredServices.addAll(services)
            servicesNeedingCharacteristicDiscovery.addAll(services)
            discoverNextCharacteristics()
        }
    }

    private fun handleDisconnection(disconnectError: UUError?)
    {
        val callback = operationCallback
        operationCallback = null
        callback?.invoke(disconnectError)
    }

    private fun discoverNextCharacteristics()
    {
        if (servicesNeedingCharacteristicDiscovery.isEmpty())
        {
            handleCharacteristicDiscoveryFinished()
            return
        }

        val service = servicesNeedingCharacteristicDiscovery.removeAt(0)
        discoverCharacteristics(service)
        {
            discoverNextCharacteristics()
        }
    }

    private fun discoverCharacteristics(service: BluetoothGattService, completion: Runnable)
    {
        discoveredCharacteristics.addAll(service.characteristics)
        completion.run()
    }

    private fun handleCharacteristicDiscoveryFinished()
    {
        execute()
        { error: UUError? ->
            end(error)
        }
    }

    fun startListeningForDataChanges(
        characteristicUuid: UUID,
        dataChanged: (ByteArray?)->Unit,
        completion: Runnable
    ) {
        requireDiscoveredCharacteristic(characteristicUuid)
        { characteristic ->
            peripheral!!.setNotifyState(characteristic, true, readTimeout,
                { peripheral: UUPeripheral?, characteristic1: BluetoothGattCharacteristic, error: UUError? ->
                    if (error != null) {
                        end(error)
                        return@setNotifyState
                    }
                    dataChanged.invoke(characteristic1.value)
                }) { peripheral: UUPeripheral?, characteristic12: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null) {
                    end(error)
                    return@setNotifyState
                }
                completion.run()
            }
        }
    }

    fun stopListeningForDataChanges(characteristicUuid: UUID, completion: Runnable)
    {
        requireDiscoveredCharacteristic(characteristicUuid) { characteristic ->
            peripheral!!.setNotifyState(
                characteristic,
                false,
                readTimeout,
                null
            ) { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null) {
                    end(error)
                    return@setNotifyState
                }
                completion.run()
            }
        }
    }
}