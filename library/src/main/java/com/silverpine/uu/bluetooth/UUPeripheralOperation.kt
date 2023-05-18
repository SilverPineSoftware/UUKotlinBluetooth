package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUBluetoothError.operationFailedError
import com.silverpine.uu.core.UUError
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

    fun findDiscoveredService(uuid: UUID): BluetoothGattService? {
        for (service in discoveredServices) {
            if (service.uuid == uuid) {
                return service
            }
        }
        return null
    }

    fun findDiscoveredCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        for (characteristic in discoveredCharacteristics) {
            if (characteristic.uuid == uuid) {
                return characteristic
            }
        }
        return null
    }

    fun requireDiscoveredService(
        uuid: UUID,
        completion: (BluetoothGattService)->Unit
    ) {
        val discovered = findDiscoveredService(uuid)
        if (discovered == null) {
            val err = operationFailedError("requireDiscoveredService")
            end(err)
            return
        }
        completion.invoke(discovered)
    }

    fun requireDiscoveredCharacteristic(
        uuid: UUID,
        completion: (BluetoothGattCharacteristic)->Unit
    ) {
        val discovered = findDiscoveredCharacteristic(uuid)
        if (discovered == null) {
            val err = operationFailedError("requireDiscoveredCharacteristic")
            end(err)
            return
        }

        completion.invoke(discovered)
    }

    fun write(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic
        ) { characteristic ->
            peripheral!!.writeCharacteristic(
                characteristic,
                data,
                writeTimeout
            ) { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null) {
                    end(error)
                    return@writeCharacteristic
                }
                completion.invoke()
            }
        }
    }

    fun wwor(data: ByteArray, toCharacteristic: UUID, completion: ()->Unit)
    {
        requireDiscoveredCharacteristic(toCharacteristic
        ) { characteristic ->
            peripheral!!.writeCharacteristicWithoutResponse(
                characteristic,
                data,
                writeTimeout
            ) { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null) {
                    end(error)
                    return@writeCharacteristicWithoutResponse
                }
                completion.invoke()
            }
        }
    }

    fun read(fromCharacteristic: UUID, completion: (ByteArray?)->Unit)
    {
        requireDiscoveredCharacteristic(fromCharacteristic
        ) { characteristic ->
            peripheral!!.readCharacteristic(
                characteristic,
                readTimeout
            ) { peripheral1: UUPeripheral?, characteristic1: BluetoothGattCharacteristic?, error: UUError? ->
                if (error != null) {
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
    ) {
        read(fromCharacteristic) { data ->
            var result: String? = null
            if (data != null) {
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
        /*read(fromCharacteristic)
        { data ->
            var result: Int? = null
            if (data != null && data.length >= java.lang.Byte.BYTES) {
                result = UUData.readUInt8(data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        }*/
    }

    fun readUInt16(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Int?)->Unit
    ) {
        /*read(fromCharacteristic, UUObjectDelegate<ByteArray> { data ->
            var result: Int? = null
            if (data != null && data.length >= java.lang.Short.BYTES) {
                result = UUData.readUInt16(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        })*/
    }

    fun readUInt32(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Long?)->Unit
    ) {
        /*
        read(fromCharacteristic, UUObjectDelegate<ByteArray> { data ->
            var result: Long? = null
            if (data != null && data.length >= Integer.BYTES) {
                result = UUData.readUInt32(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        })*/
    }

    fun readUInt64(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Long?)->Unit
    ) {
        /*
        read(fromCharacteristic, UUObjectDelegate<ByteArray> { data ->
            var result: Long? = null
            if (data != null && data.length >= java.lang.Long.BYTES) {
                result = UUData.readUInt64(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        })*/
    }

    fun readInt8(fromCharacteristic: UUID, completion: (Byte?)->Unit)
    {
        /*
        read(fromCharacteristic) { data ->
            var result: Byte? = null
            if (data != null && data.length >= java.lang.Byte.BYTES) {
                result = UUData.readInt8(data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        }*/
    }

    fun readInt16(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Short?)->Unit
    ) {
        /*
        read(fromCharacteristic) { data ->
            var result: Short? = null
            if (data != null && data.length >= java.lang.Short.BYTES) {
                result = UUData.readInt16(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        }*/
    }

    fun readInt32(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Int?)->Unit
    ) {
        /*
        read(fromCharacteristic) { data ->
            var result: Int? = null
            if (data != null && data.length >= Integer.BYTES) {
                result = UUData.readInt32(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        }*/
    }

    fun readInt64(
        fromCharacteristic: UUID,
        byteOrder: ByteOrder,
        completion: (Long?)->Unit
    ) {
        /*
        read(fromCharacteristic) { data ->
            var result: Long? = null
            if (data != null && data.length >= java.lang.Long.BYTES) {
                result = UUData.readInt64(byteOrder, data, 0)
            }
            UUObjectDelegate.safeInvoke(completion, result)
        }*/
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
        val buffer = ByteArray(java.lang.Byte.BYTES)
        //UUData.writeUInt8(buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt16(
        value: Int,
        byteOrder: ByteOrder,
        toCharacteristic: UUID,
        completion: ()->Unit
    ) {
        val buffer = ByteArray(java.lang.Short.BYTES)
        //UUData.writeUInt16(byteOrder, buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt32(
        value: Long,
        byteOrder: ByteOrder,
        toCharacteristic: UUID,
        completion: ()->Unit
    ) {
        val buffer = ByteArray(Integer.BYTES)
        //UUData.writeUInt32(byteOrder, buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeUInt64(
        value: Long,
        byteOrder: ByteOrder,
        toCharacteristic: UUID,
        completion: ()->Unit
    ) {
        val buffer = ByteArray(java.lang.Long.BYTES)
        //UUData.writeUInt64(byteOrder, buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt8(value: Byte, toCharacteristic: UUID, completion: ()->Unit) {
        val buffer = ByteArray(java.lang.Byte.BYTES)
        //UUData.writeInt8(buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt16(
        value: Short,
        byteOrder: ByteOrder,
        toCharacteristic: UUID,
        completion: ()->Unit
    ) {
        val buffer = ByteArray(java.lang.Short.BYTES)
        //UUData.writeInt16(byteOrder, buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt32(value: Int, byteOrder: ByteOrder, toCharacteristic: UUID, completion: ()->Unit) {
        val buffer = ByteArray(Integer.BYTES)
        //UUData.writeInt32(byteOrder, buffer, 0, value)
        write(buffer, toCharacteristic, completion)
    }

    fun writeInt64(
        value: Long,
        byteOrder: ByteOrder,
        toCharacteristic: UUID,
        completion: ()->Unit
    ) {
        val buffer = ByteArray(java.lang.Long.BYTES)
        //UUData.writeInt64(byteOrder, buffer, 0, value)
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

//    protected fun execute(completion: (UUError?)->Unit) {
//        //UUObjectDelegate.safeInvoke(completion, null)
//        completion.invoke(null)
//    }

    private fun handleConnected()
    {
        peripheral?.discoverServices(serviceDiscoveryTimeout)
        { services, error ->
            if (error != null) {
                end(error)
                return@discoverServices
            }
            discoveredServices.clear()
            discoveredCharacteristics.clear()
            servicesNeedingCharacteristicDiscovery.clear()
            val services: List<BluetoothGattService> =
                peripheral.discoveredServices()
            if (services.isEmpty()) {
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

    private fun discoverNextCharacteristics() {
        if (servicesNeedingCharacteristicDiscovery.isEmpty()) {
            handleCharacteristicDiscoveryFinished()
            return
        }
        val service = servicesNeedingCharacteristicDiscovery.removeAt(0)
        discoverCharacteristics(service) { discoverNextCharacteristics() }
    }

    private fun discoverCharacteristics(service: BluetoothGattService, completion: Runnable) {
        discoveredCharacteristics.addAll(service.characteristics)
        completion.run()
    }

    private fun handleCharacteristicDiscoveryFinished()
    {
        execute { error: UUError? -> end(error) }
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