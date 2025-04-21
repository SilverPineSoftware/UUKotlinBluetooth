package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUBluetoothError.missingRequiredCharacteristic
import com.silverpine.uu.bluetooth.UUBluetoothError.operationFailedError
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimedMetric
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
import com.silverpine.uu.logging.UULog
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID

abstract class UUPeripheralOperation<T : UUPeripheral>(protected val peripheral: T)
{
    object Defaults
    {
        const val ConnectTimeout = 60000
        const val DisconnectTimeout = 10000
        const val ServiceDiscoveryTimeout = 60000
        const val OperationTimeout = 60000
    }

    private var operationCallback: ((UUError?)->Unit)? = null
    val discoveredServices = ArrayList<BluetoothGattService>()
    val discoveredCharacteristics = ArrayList<BluetoothGattCharacteristic>()
    private val servicesNeedingCharacteristicDiscovery = ArrayList<BluetoothGattService>()
    var connectTimeout = Defaults.ConnectTimeout.toLong()
    var disconnectTimeout = Defaults.DisconnectTimeout.toLong()
    var serviceDiscoveryTimeout = Defaults.ServiceDiscoveryTimeout.toLong()
    protected val readTimeout = Defaults.OperationTimeout.toLong()
    protected val writeTimeout = Defaults.OperationTimeout.toLong()

    val connectMetric = UUTimedMetric("connect")
    val serviceDiscoveryMetric = UUTimedMetric("service_discovery")
    val operationTimeMetric = UUTimedMetric("operation_time")
    val overallTimeMetric = UUTimedMetric("overall_time")

    fun findDiscoveredService(uuid: UUID): BluetoothGattService?
    {
        return discoveredServices.firstOrNull { it.uuid == uuid }
    }

    fun findDiscoveredCharacteristic(uuid: UUID): BluetoothGattCharacteristic?
    {
        return discoveredCharacteristics.firstOrNull { it.uuid == uuid }
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

        completion(discovered)
    }

    fun requireDiscoveredCharacteristic(uuid: UUID, completion: (BluetoothGattCharacteristic)->Unit)
    {
        val discovered = findDiscoveredCharacteristic(uuid)
        if (discovered == null)
        {
            val err = missingRequiredCharacteristic(uuid)
            end(err)
            return
        }

        completion(discovered)
    }

    fun write(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic)
        { characteristic ->

            peripheral.write(data, characteristic, writeTimeout)
            { error ->
                if (error != null)
                {
                    end(error)
                    return@write
                }

                completion()
            }
        }
    }

    fun writeData(data: ByteArray, toCharacteristic: UUID, completion: (UUError?)->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic)
        { characteristic ->

            peripheral.write(data, characteristic, writeTimeout)
            { error ->
                if (error != null)
                {
                    end(error)
                    return@write
                }

                completion(error)
            }
        }
    }

    fun wwor(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic)
        { characteristic ->
            peripheral.writeWithoutResponse(data, characteristic)
            { error: UUError? ->

                if (error != null)
                {
                    end(error)
                    return@writeWithoutResponse
                }

                completion()
            }
        }
    }

    fun read(fromCharacteristic: UUID, completion: (ByteArray?)->Unit)
    {
        requireDiscoveredCharacteristic(fromCharacteristic)
        { characteristic ->

            peripheral.read(characteristic, readTimeout)
            { data, error ->

                if (error != null)
                {
                    end(error)
                    return@read
                }

                completion(data)
            }
        }
    }

    fun readString(fromCharacteristic: UUID, charset: Charset, completion: (String?)->Unit)
    {
        read(fromCharacteristic)
        { data ->

            var result: String? = null

            if (data != null)
            {
                result = String(data, charset)
            }

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

            completion(result)
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

    private fun resetAllMetrics()
    {
        connectMetric.reset()
        serviceDiscoveryMetric.reset()
        operationTimeMetric.reset()
        overallTimeMetric.reset()
    }

    private fun endAllMetrics()
    {
        connectMetric.end(false)
        serviceDiscoveryMetric.end(false)
        operationTimeMetric.end(false)
        overallTimeMetric.end(false)
    }

    private fun logMetrics()
    {
        UULog.d(javaClass, "logMetrics", "${javaClass.simpleName}, $connectMetric")
        UULog.d(javaClass, "logMetrics", "${javaClass.simpleName}, $serviceDiscoveryMetric")
        UULog.d(javaClass, "logMetrics", "${javaClass.simpleName}, $operationTimeMetric")
        UULog.d(javaClass, "logMetrics", "${javaClass.simpleName}, $overallTimeMetric")
    }

    fun start(completion: (UUError?)->Unit)
    {
        UULog.d(javaClass, "start", "${javaClass.simpleName}, Starting operation")

        resetAllMetrics()
        connectMetric.start()
        overallTimeMetric.start()

        operationCallback = completion
        peripheral.connect(connectTimeout, { handleConnected() })
        { disconnectError: UUError? ->
            handleDisconnection(
                disconnectError
            )
        }
    }

    fun end(error: UUError?)
    {
        UULog.d(javaClass, "end", "${javaClass.simpleName}, Ending operation, error: $error")
        //debug(javaClass, "end", "**** Ending Operation with error: " + UUString.safeToString(error))
        // peripheral.disconnect(error)
        peripheral.disconnect(disconnectTimeout)
    }

    fun interrupt()
    {
        end(UUBluetoothError.userInterruptedError())
    }

    abstract fun execute(completion: (UUError?)->Unit)

    private fun handleConnected()
    {
        connectMetric.end()
        UULog.d(javaClass, "handleConnected", "Connect took ${connectMetric.duration} ms")
        UULog.d(javaClass, "handleConnected", "${javaClass.simpleName}, is connected, discovering services now")

        serviceDiscoveryMetric.start()

        peripheral.discoverServices(serviceDiscoveryTimeout)
        { services, error ->

            if (error != null)
            {
                end(error)
                return@discoverServices
            }

            discoveredServices.clear()
            discoveredCharacteristics.clear()
            servicesNeedingCharacteristicDiscovery.clear()

            if (services?.isEmpty() == true)
            {
                val err = operationFailedError("No Services Found")
                end(err)
                return@discoverServices
            }

            services?.let()
            {
                discoveredServices.addAll(services)
                servicesNeedingCharacteristicDiscovery.addAll(services)
            }

            serviceDiscoveryMetric.end()
            UULog.d(javaClass, "handleConnected", "Service Discovery took ${connectMetric.duration} ms")

            discoverNextCharacteristics()
        }
    }

    private fun handleDisconnection(disconnectError: UUError?)
    {
        endAllMetrics()
        logMetrics()

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

    private fun discoverCharacteristics(service: BluetoothGattService, completion: ()->Unit)
    {
        discoveredCharacteristics.addAll(service.characteristics)
        completion()
    }

    private fun handleCharacteristicDiscoveryFinished()
    {
        executeOperation()
    }

    private fun executeOperation()
    {
        try
        {
            operationTimeMetric.start()

            execute()
            { error: UUError? ->

                operationTimeMetric.end()
                UULog.d(javaClass, "executeOperation", "Operation took ${connectMetric.duration} ms")
                end(error)
            }
        }
        catch (ex: Exception)
        {
            UULog.e(javaClass, "executeOperation", "", ex)
        }
    }

    fun startListeningForDataChanges(characteristicUuid: UUID, dataChanged: (ByteArray?)->Unit, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(characteristicUuid)
        { characteristic ->

            peripheral.setNotifyValue(true, characteristic, readTimeout,
                notifyHandler =
                    { p, chr, error ->

                        if (error != null)
                        {
                            end(error)
                            return@setNotifyValue
                        }

                        dataChanged(chr.value)

                    },
                completion =
                    { p, chr, error ->

                        if (error != null)
                        {
                            end(error)
                            return@setNotifyValue
                        }

                        completion()
                    })
        }
    }

    fun stopListeningForDataChanges(characteristicUuid: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(characteristicUuid)
        { characteristic ->

            peripheral.setNotifyValue(false, characteristic, readTimeout,
                notifyHandler = null,
                completion =
                    { p, chr, error ->

                        if (error != null)
                        {
                            end(error)
                            return@setNotifyValue
                        }

                        completion()
                    })

            /*
            peripheral.setNotifyState(characteristic, false, readTimeout, null)
            { _, _, error: UUError? ->

                if (error != null)
                {
                    end(error)
                    return@setNotifyState
                }

                completion()
            }*/
        }
    }
}