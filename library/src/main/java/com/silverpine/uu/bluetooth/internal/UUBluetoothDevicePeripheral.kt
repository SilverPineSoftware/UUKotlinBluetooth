package com.silverpine.uu.bluetooth.internal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothSocket
import com.silverpine.uu.bluetooth.UUBluetoothConstants.DEFAULT_MTU
import com.silverpine.uu.bluetooth.UUDiscoverServicesCompletionBlock
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralConnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
import com.silverpine.uu.bluetooth.UUPeripheralDisconnectedBlock

@SuppressLint("MissingPermission")
internal class UUBluetoothDevicePeripheral(
    override val advertisement: UUBluetoothAdvertisement
) : UUPeripheral
{
    private val bluetoothDevice: BluetoothDevice = advertisement.device

    override var rssi: Int = advertisement.rssi
    override var firstDiscoveryTime: Long = 0L
    override var identifier: String = advertisement.address
    override var name: String = bluetoothDevice.name ?: ""
    override var friendlyName: String = advertisement.address
    override var services: List<BluetoothGattService>? = null
    override var mtuSize: Int = DEFAULT_MTU

    override val peripheralState: UUPeripheralConnectionState
        get()
        {
            val gatt = UUBluetoothGatt.get(bluetoothDevice)
            return gatt.getPeripheralState()
        }

    override fun connect(
        timeout: Long,
        connected: UUPeripheralConnectedBlock,
        disconnected: UUPeripheralDisconnectedBlock)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.connect(timeout, connected, disconnected)
    }

    override fun disconnect(timeout: Long)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.disconnect(null)
    }

    override fun discoverServices(
        timeout: Long,
        completion: UUDiscoverServicesCompletionBlock)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.discoverServices(timeout)
        { discoveredServices, error ->

            this.services = discoveredServices

            completion(discoveredServices, error)
        }
    }

    override fun setNotifyValue(
        enabled: Boolean,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        notifyHandler: UUCharacteristicDataCallback?,
        completion: UUCharacteristicErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.setNotifyValue(enabled, characteristic, timeout, notifyHandler, completion)
    }

    override fun read(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUDataErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.read(characteristic, timeout, completion)
    }

    override fun read(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUDataErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.read(descriptor, timeout, completion)
    }

    override fun write(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        completion: UUErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.write(data, characteristic, timeout, completion)
    }

    override fun writeWithoutResponse(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        completion: UUErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.writeWithoutResponse(data, characteristic, completion)
    }

    override fun write(
        data: ByteArray,
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        completion: UUErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.write(data, descriptor, timeout, completion)
    }

    override fun readRSSI(
        timeout: Long,
        completion: UUIntErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.readRSSI(timeout)
        { rssi, error ->

            rssi?.let { this.rssi = it }
            completion(rssi, error)
        }
    }

    override fun requestMtu(
        mtu: Int,
        timeout: Long,
        completion: UUIntErrorCallback)
    {
        val gatt = UUBluetoothGatt.get(bluetoothDevice)
        gatt.requestMtu(mtu, timeout)
        { updatedMtu, error ->

            updatedMtu?.let { this.mtuSize = it }
            completion(updatedMtu, error)
        }
    }

    override fun createL2capChannel(psm: Int): BluetoothSocket?
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

    override fun createInsecureL2capChannel(psm: Int): BluetoothSocket?
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







/*
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

*/















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




/*


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

*/

/*
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
*/














}