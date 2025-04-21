package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUBluetoothError
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuDispatch

internal class UUBluetoothGattCallback : BluetoothGattCallback()
{
    var connectionStateChangedCallback: UUIntIntCallback? = null
    var servicesDiscoveredCallback: UUServiceListCallback? = null
    private var readCharacteristicCallbacks: HashMap<String, UUDataErrorCallback> = hashMapOf()
    private var characteristicDataChangedCallbacks: HashMap<String, UUDataCallback> = hashMapOf()
    private var writeCharacteristicCallbacks: HashMap<String, UUErrorCallback> = hashMapOf()
    private var readDescriptorCallbacks: HashMap<String, UUDataErrorCallback> = hashMapOf()
    private var writeDescriptorCallbacks: HashMap<String, UUErrorCallback> = hashMapOf()
    var executeReliableWriteCallback: UUErrorCallback? = null
    var readRssiCallback: UUIntErrorCallback? = null
    var mtuChangedCallback: UUIntErrorCallback? = null
    var phyReadCallback: UUIntIntErrorCallback? = null
    var phyUpdatedCallback: UUIntIntErrorCallback? = null
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

    fun registerCharacteristicDataChangedCallback(characteristic: BluetoothGattCharacteristic, callback: UUDataCallback)
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Bluetooth Gatt Overrides
    ////////////////////////////////////////////////////////////////////////////////////////////////


    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
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

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
    {
        notifyServicesDiscovered(gatt.services, UUBluetoothError.gattStatusError("onServicesDiscovered", status))
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

    // This is called on Android 12 and below, so it needs to be implemented.  When it gets a value,
    // we simply pipe into the new callback method
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int)
    {
        val gtt = gatt ?: return
        val chr = characteristic ?: return
        val data = chr.value
        onCharacteristicRead(gtt, chr, data, status)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int)
    {
        notifyCharacteristicRead(characteristic, value, UUBluetoothError.gattStatusError("onCharacteristicRead", status))
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

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray)
    {
        val block: UUDataCallback?
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
                it(value)
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int)
    {
        val id = characteristic?.uuHashLookup()

        val block: UUErrorCallback?
        synchronized(writeCharacteristicCallbacks)
        {
            block = writeCharacteristicCallbacks[id]
            writeCharacteristicCallbacks.remove(id)
        }

        block?.let()
        {
            uuDispatch()
            {
                it(UUBluetoothError.gattStatusError("onCharacteristicWrite", status))
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

    // This is called on Android 12 and below, so it needs to be implemented.  When it gets a value,
    // we simply pipe into the new callback method
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int)
    {
        val gtt = gatt ?: return
        val desc = descriptor ?: return
        val data = desc.value
        onDescriptorRead(gtt, desc, status, data)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray)
    {
        notifyDescriptorRead(descriptor, value, UUBluetoothError.gattStatusError("onDescriptorRead", status))
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

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int)
    {
        val id = descriptor?.uuHashLookup()

        val block: UUErrorCallback?
        synchronized(writeDescriptorCallbacks)
        {
            block = writeDescriptorCallbacks[id]
            writeDescriptorCallbacks.remove(id)
        }

        block?.let()
        {
            uuDispatch()
            {
                it(UUBluetoothError.gattStatusError("onDescriptorWrite", status))
            }
        }
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int)
    {
        val block = executeReliableWriteCallback
        executeReliableWriteCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(UUBluetoothError.gattStatusError("onReliableWriteCompleted", status))
            }
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int)
    {
        val block = readRssiCallback
        readRssiCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(rssi, UUBluetoothError.gattStatusError("onReadRemoteRssi", status))
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int)
    {
        val block = mtuChangedCallback
        mtuChangedCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(mtu, UUBluetoothError.gattStatusError("onMtuChanged", status))
            }
        }
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int)
    {
        val block = phyReadCallback
        phyReadCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(txPhy, rxPhy, UUBluetoothError.gattStatusError("onPhyRead", status))
            }
        }
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int)
    {
        val block = phyUpdatedCallback
        phyUpdatedCallback = null

        block?.let()
        {
            uuDispatch()
            {
                it(txPhy, rxPhy, UUBluetoothError.gattStatusError("onPhyUpdate", status))
            }
        }
    }

    override fun onServiceChanged(gatt: BluetoothGatt)
    {
        val block = serviceChangedCallback

        // Don't null out block since this potentially could be invoked many times within a session

        block?.let()
        {
            uuDispatch(it)
        }
    }
}
