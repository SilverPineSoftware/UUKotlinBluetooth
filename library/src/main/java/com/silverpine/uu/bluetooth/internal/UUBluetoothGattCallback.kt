package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUBluetoothError
import com.silverpine.uu.bluetooth.UUVoidCallback
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog

internal class UUBluetoothGattCallback : BluetoothGattCallback()
{
    var connectionStateChangedCallback: UUIntIntCallback? = null
    var servicesDiscoveredCallback: UUServiceListCallback? = null
    private var readCharacteristicCallbacks: HashMap<String, UUDataErrorCallback> = hashMapOf()
    private var characteristicDataChangedCallbacks: HashMap<String, UUCharacteristicDataCallback> = hashMapOf()
    private var writeCharacteristicCallbacks: HashMap<String, UUErrorCallback> = hashMapOf()
    private var readDescriptorCallbacks: HashMap<String, UUDataErrorCallback> = hashMapOf()
    private var writeDescriptorCallbacks: HashMap<String, UUErrorCallback> = hashMapOf()
    private var setCharacteristicNotificationCallbacks: HashMap<String, UUCharacteristicErrorCallback> = hashMapOf()
    var readRssiCallback: UUIntErrorCallback? = null
    var mtuChangedCallback: UUIntErrorCallback? = null
    var phyReadCallback: UUIntIntErrorCallback? = null
    var phyUpdatedCallback: UUIntIntErrorCallback? = null

    var executeReliableWriteCallback: UUErrorCallback? = null
    var serviceChangedCallback: UUVoidCallback? = null

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Public Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun clearAll()
    {
        connectionStateChangedCallback = null
        servicesDiscoveredCallback = null
        readCharacteristicCallbacks.clear()
        characteristicDataChangedCallbacks.clear()
        writeCharacteristicCallbacks.clear()
        readDescriptorCallbacks.clear()
        writeDescriptorCallbacks.clear()
        setCharacteristicNotificationCallbacks.clear()
        executeReliableWriteCallback = null
        readRssiCallback = null
        mtuChangedCallback = null
        phyReadCallback = null
        phyUpdatedCallback = null
        serviceChangedCallback = null
    }

    fun registerReadCharacteristicCallback(characteristic: BluetoothGattCharacteristic, callback: UUDataErrorCallback)
    {
        synchronized(readCharacteristicCallbacks)
        {
            readCharacteristicCallbacks[characteristic.uuHashLookup()] = callback
        }
    }

    fun clearReadCharacteristicCallback(characteristic: BluetoothGattCharacteristic)
    {
        synchronized(readCharacteristicCallbacks)
        {
            readCharacteristicCallbacks.remove(characteristic.uuHashLookup())
        }
    }

    private fun popReadCharacteristicCallback(characteristic: BluetoothGattCharacteristic): UUDataErrorCallback?
    {
        val block: UUDataErrorCallback?
        synchronized(readCharacteristicCallbacks)
        {
            val id = characteristic.uuHashLookup()
            block = readCharacteristicCallbacks[id]
            readCharacteristicCallbacks.remove(id)
        }

        return block
    }

    fun registerSetCharacteristicNotificationCallback(characteristic: BluetoothGattCharacteristic, callback: UUCharacteristicErrorCallback)
    {
        synchronized(setCharacteristicNotificationCallbacks)
        {
            setCharacteristicNotificationCallbacks[characteristic.uuHashLookup()] = callback
        }
    }

    fun clearSetCharacteristicNotificationCallback(characteristic: BluetoothGattCharacteristic)
    {
        synchronized(setCharacteristicNotificationCallbacks)
        {
            setCharacteristicNotificationCallbacks.remove(characteristic.uuHashLookup())
        }
    }

    private fun popSetCharacteristicNotificationCallback(characteristic: BluetoothGattCharacteristic): UUCharacteristicErrorCallback?
    {
        val block: UUCharacteristicErrorCallback?
        synchronized(setCharacteristicNotificationCallbacks)
        {
            val id = characteristic.uuHashLookup()
            block = setCharacteristicNotificationCallbacks[id]
            setCharacteristicNotificationCallbacks.remove(id)
        }

        return block
    }

    fun notifyCharacteristicSetNotifyCallback(
        characteristic: BluetoothGattCharacteristic,
        error: UUError?)
    {
        val block = popSetCharacteristicNotificationCallback(characteristic)

        block?.let()
        {
            uuDispatch()
            {
                it(characteristic, error)
            }
        }
    }

    fun registerCharacteristicDataChangedCallback(characteristic: BluetoothGattCharacteristic, callback: UUCharacteristicDataCallback)
    {
        synchronized(characteristicDataChangedCallbacks)
        {
            characteristicDataChangedCallbacks[characteristic.uuHashLookup()] = callback
        }
    }

    fun clearCharacteristicDataChangedCallback(characteristic: BluetoothGattCharacteristic)
    {
        synchronized(characteristicDataChangedCallbacks)
        {
            characteristicDataChangedCallbacks.remove(characteristic.uuHashLookup())
        }
    }

    fun registerWriteCharacteristicCallback(characteristic: BluetoothGattCharacteristic, callback: UUErrorCallback)
    {
        synchronized(writeCharacteristicCallbacks)
        {
            writeCharacteristicCallbacks[characteristic.uuHashLookup()] = callback
        }
    }

    fun clearWriteCharacteristicCallback(characteristic: BluetoothGattCharacteristic)
    {
        synchronized(writeCharacteristicCallbacks)
        {
            writeCharacteristicCallbacks.remove(characteristic.uuHashLookup())
        }
    }

    private fun popWriteCharacteristicCallback(characteristic: BluetoothGattCharacteristic): UUErrorCallback?
    {
        val block: UUErrorCallback?
        synchronized(writeCharacteristicCallbacks)
        {
            val id = characteristic.uuHashLookup()
            block = writeCharacteristicCallbacks[id]
            writeCharacteristicCallbacks.remove(id)
        }

        return block
    }

    fun registerReadDescriptorCallback(descriptor: BluetoothGattDescriptor, callback: UUDataErrorCallback)
    {
        synchronized(readDescriptorCallbacks)
        {
            readDescriptorCallbacks[descriptor.uuHashLookup()] = callback
        }
    }

    fun clearReadDescriptorCallback(descriptor: BluetoothGattDescriptor)
    {
        synchronized(readDescriptorCallbacks)
        {
            readDescriptorCallbacks.remove(descriptor.uuHashLookup())
        }
    }

    private fun popReadDescriptorCallback(descriptor: BluetoothGattDescriptor): UUDataErrorCallback?
    {
        val block: UUDataErrorCallback?
        synchronized(readDescriptorCallbacks)
        {
            val id = descriptor.uuHashLookup()
            block = readDescriptorCallbacks[id]
            readDescriptorCallbacks.remove(id)
        }

        return block
    }

    fun registerWriteDescriptorCallback(descriptor: BluetoothGattDescriptor, callback: UUErrorCallback)
    {
        synchronized(writeDescriptorCallbacks)
        {
            writeDescriptorCallbacks[descriptor.uuHashLookup()] = callback
        }
    }

    fun clearWriteDescriptorCallback(descriptor: BluetoothGattDescriptor)
    {
        synchronized(writeDescriptorCallbacks)
        {
            writeDescriptorCallbacks.remove(descriptor.uuHashLookup())
        }
    }

    private fun popWriteDescriptorCallback(descriptor: BluetoothGattDescriptor): UUErrorCallback?
    {
        val block: UUErrorCallback?
        synchronized(writeDescriptorCallbacks)
        {
            val id = descriptor.uuHashLookup()
            block = writeDescriptorCallbacks[id]
            writeDescriptorCallbacks.remove(id)
        }

        return block
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Bluetooth Gatt Overrides
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
    {
        UULog.d(javaClass, "onConnectionStateChange", "status: $status, newState: $newState")

        notifyConnectionStateChanged(status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
    {
        UULog.d(javaClass, "onServicesDiscovered", "gatt.services: ${gatt.services}, status: $status")

        notifyServicesDiscovered(gatt.services, UUBluetoothError.gattStatusError("onServicesDiscovered", status))
    }

    // This is called on Android 12 and below, so it needs to be implemented.  When it gets a value,
    // we simply pipe into the new callback method
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int)
    {
        UULog.d(javaClass, "onCharacteristicRead", "characteristic: ${characteristic?.uuid}, value: ${characteristic?.value?.uuToHex()}, status: $status")

        val chr = characteristic ?: return
        notifyCharacteristicRead(chr, chr.value, UUBluetoothError.gattStatusError("onCharacteristicRead", status))

    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int)
    {
        UULog.d(javaClass, "onCharacteristicRead", "characteristic: ${characteristic.uuid}, value: ${value.uuToHex()}, status: $status")

        notifyCharacteristicRead(characteristic, value, UUBluetoothError.gattStatusError("onCharacteristicRead", status))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray)
    {
        UULog.d(javaClass, "onCharacteristicChanged", "characteristic: ${characteristic.uuid}, data: ${value.uuToHex()}")

        notifyCharacteristicChanged(characteristic, value)
    }

    // This is called on Android 12 and below, so it needs to be implemented.  When it gets a value,
    // we simply pipe into the new callback method
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?)
    {
        UULog.d(javaClass, "onCharacteristicChanged", "characteristic: ${characteristic?.uuid}, data: ${characteristic?.value?.uuToHex()}")

        val chr = characteristic ?: return
        notifyCharacteristicChanged(characteristic, chr.value)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int)
    {
        UULog.d(javaClass, "onDescriptorRead", "characteristic: ${characteristic?.uuid}, data: ${characteristic?.value?.uuToHex()} status: $status")

        val chr = characteristic ?: return
        notifyCharacteristicWrite(chr, UUBluetoothError.gattStatusError("onCharacteristicWrite", status))
    }

    // This is called on Android 12 and below, so it needs to be implemented.  When it gets a value,
    // we simply pipe into the new callback method
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int)
    {
        UULog.d(javaClass, "onDescriptorRead", "descriptor: ${descriptor?.uuid}, data: ${descriptor?.value?.uuToHex()} status: $status")

        val desc = descriptor ?: return
        notifyDescriptorRead(desc, desc.value, UUBluetoothError.gattStatusError("onDescriptorRead", status))
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray)
    {
        UULog.d(javaClass, "onDescriptorRead", "descriptor: ${descriptor.uuid}, data: ${value.uuToHex()} status: $status")

        notifyDescriptorRead(descriptor, value, UUBluetoothError.gattStatusError("onDescriptorRead", status))
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int)
    {
        UULog.d(javaClass, "onDescriptorWrite", "descriptor: ${descriptor?.uuid}, data: ${descriptor?.value?.uuToHex()} status: $status")

        val desc = descriptor ?: return
        notifyDescriptorWrite(desc, UUBluetoothError.gattStatusError("onDescriptorWrite", status))
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int)
    {
        UULog.d(javaClass, "onReliableWriteCompleted", "status: $status")

        notifyReliableWriteComplete(UUBluetoothError.gattStatusError("onReliableWriteCompleted", status))
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int)
    {
        UULog.d(javaClass, "onReadRemoteRssi", "rssi: $rssi, status: $status")

        notifyRemoteRssiRead(rssi, UUBluetoothError.gattStatusError("onReadRemoteRssi", status))
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int)
    {
        UULog.d(javaClass, "onMtuChanged", "mtu: $mtu, status: $status")

        notifyMtuChanged(mtu, UUBluetoothError.gattStatusError("onMtuChanged", status))
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int)
    {
        UULog.d(javaClass, "onPhyRead", "txPhy: $txPhy, rxPhy: $rxPhy, status: $status")

        notifyPhyRead(txPhy, rxPhy, UUBluetoothError.gattStatusError("onPhyRead", status))
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int)
    {
        UULog.d(javaClass, "onPhyUpdate", "txPhy: $txPhy, rxPhy: $rxPhy, status: $status")

        notifyPhyUpdate(txPhy, rxPhy, UUBluetoothError.gattStatusError("onPhyUpdate", status))
    }

    override fun onServiceChanged(gatt: BluetoothGatt)
    {
        UULog.d(javaClass, "onServiceChanged", "GATT database is out of sync, re-discover services.")

        notifyServiceChanged()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Callback Wrappers
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun notifyConnectionStateChanged(status: Int, newState: Int)
    {
        val block = connectionStateChangedCallback

        // Do not clear the delegate because the connection state can be invoked multiple times.

        block?.let()
        {
            uuDispatch()
            {
                block(status, newState)
            }
        }
    }

    fun notifyServicesDiscovered(services: List<BluetoothGattService>?, error: UUError?)
    {
        val block = servicesDiscoveredCallback
        servicesDiscoveredCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(services, error)
            }
        }
    }

    fun notifyCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?,
        error: UUError?)
    {
        val block = popReadCharacteristicCallback(characteristic)

        block?.let()
        {
            uuDispatch()
            {
                it(value, error)
            }
        }
    }

    private fun notifyCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?)
    {
        val block: UUCharacteristicDataCallback?
        synchronized(characteristicDataChangedCallbacks)
        {
            val id = characteristic.uuHashLookup()
            block = characteristicDataChangedCallbacks[id]
            // Do not clear the callback since this can be invoked multiple times
        }

        block?.let()
        {
            uuDispatch()
            {
                it(characteristic, value)
            }
        }
    }

    fun notifyCharacteristicWrite(
        characteristic: BluetoothGattCharacteristic,
        error: UUError?)
    {
        val block = popWriteCharacteristicCallback(characteristic)

        block?.let()
        {
            uuDispatch()
            {
                it(error)
            }
        }
    }

    fun notifyDescriptorRead(
        descriptor: BluetoothGattDescriptor,
        value: ByteArray?,
        error: UUError?)
    {
        val block = popReadDescriptorCallback(descriptor)

        block?.let()
        {
            uuDispatch()
            {
                it(value, error)
            }
        }
    }

    fun notifyDescriptorWrite(
        descriptor: BluetoothGattDescriptor,
        error: UUError?)
    {
        val block = popWriteDescriptorCallback(descriptor)

        block?.let()
        {
            uuDispatch()
            {
                it(error)
            }
        }
    }

    fun notifyRemoteRssiRead(
        value: Int?,
        error: UUError?)
    {
        val block = readRssiCallback
        readRssiCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(value, error)
            }
        }
    }

    fun notifyMtuChanged(
        value: Int?,
        error: UUError?)
    {
        val block = mtuChangedCallback
        mtuChangedCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(value, error)
            }
        }
    }

    fun notifyPhyRead(
        txPhy: Int?,
        rxPhy: Int?,
        error: UUError?)
    {
        val block = phyReadCallback
        phyReadCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(txPhy, rxPhy, error)
            }
        }
    }

    fun notifyPhyUpdate(
        txPhy: Int?,
        rxPhy: Int?,
        error: UUError?)
    {
        val block = phyUpdatedCallback
        phyUpdatedCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(txPhy, rxPhy, error)
            }
        }
    }

    private fun notifyReliableWriteComplete(error: UUError?)
    {
        val block = executeReliableWriteCallback
        executeReliableWriteCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(error)
            }
        }
    }

    private fun notifyServiceChanged()
    {
        val block = serviceChangedCallback

        // Don't null out block since this potentially could be invoked many times within a session

        block?.let()
        {
            uuDispatch(it)
        }
    }
}
