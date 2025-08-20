package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.internal.uuHashLookup
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog

class UUBluetoothGattCallback : BluetoothGattCallback()
{
    var connectionStateChangedCallback: UUObjectBlock<Pair<Int,Int>>? = null
    var servicesDiscoveredCallback: UUListErrorBlock<BluetoothGattService>? = null
    private var readCharacteristicCallbacks: HashMap<String, UUObjectErrorBlock<ByteArray>> = hashMapOf()
    private var characteristicDataChangedCallbacks: HashMap<String, UUObjectBlock<ByteArray>> = hashMapOf()
    private var writeCharacteristicCallbacks: HashMap<String, UUErrorBlock> = hashMapOf()
    private var readDescriptorCallbacks: HashMap<String, UUObjectErrorBlock<ByteArray>> = hashMapOf()
    private var writeDescriptorCallbacks: HashMap<String, UUErrorBlock> = hashMapOf()
    private var setCharacteristicNotificationCallbacks: HashMap<String, UUErrorBlock> = hashMapOf()
    var readRssiCallback: UUObjectErrorBlock<Int>? = null
    var mtuChangedCallback: UUObjectErrorBlock<Int>? = null
    var phyReadCallback: UUObjectErrorBlock<Pair<Int,Int>>? = null
    var phyUpdatedCallback: UUObjectErrorBlock<Pair<Int,Int>>? = null

    var executeReliableWriteCallback: UUErrorBlock? = null
    var serviceChangedCallback: UUVoidBlock? = null

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

    fun registerReadCharacteristicCallback(identifier: String, callback: UUObjectErrorBlock<ByteArray>)
    {
        synchronized(readCharacteristicCallbacks)
        {
            readCharacteristicCallbacks[identifier] = callback
        }
    }

    fun clearReadCharacteristicCallback(identifier: String)
    {
        synchronized(readCharacteristicCallbacks)
        {
            readCharacteristicCallbacks.remove(identifier)
        }
    }

    private fun popReadCharacteristicCallback(identifier: String): UUObjectErrorBlock<ByteArray>?
    {
        val block: UUObjectErrorBlock<ByteArray>?
        synchronized(readCharacteristicCallbacks)
        {
            block = readCharacteristicCallbacks[identifier]
            readCharacteristicCallbacks.remove(identifier)
        }

        return block
    }

    fun registerSetCharacteristicNotificationCallback(identifier: String, callback: UUErrorBlock)
    {
        synchronized(setCharacteristicNotificationCallbacks)
        {
            setCharacteristicNotificationCallbacks[identifier] = callback
        }
    }

    fun clearSetCharacteristicNotificationCallback(identifier: String)
    {
        synchronized(setCharacteristicNotificationCallbacks)
        {
            setCharacteristicNotificationCallbacks.remove(identifier)
        }
    }

    private fun popSetCharacteristicNotificationCallback(identifier: String): UUErrorBlock?
    {
        val block: UUErrorBlock?
        synchronized(setCharacteristicNotificationCallbacks)
        {
            block = setCharacteristicNotificationCallbacks[identifier]
            setCharacteristicNotificationCallbacks.remove(identifier)
        }

        return block
    }

    fun notifyCharacteristicSetNotifyCallback(
        identifier: String,
        error: UUError?)
    {
        val block = popSetCharacteristicNotificationCallback(identifier)
        block?.dispatch(error)
    }

    fun registerCharacteristicDataChangedCallback(identifier: String, callback: UUObjectBlock<ByteArray>)
    {
        synchronized(characteristicDataChangedCallbacks)
        {
            characteristicDataChangedCallbacks[identifier] = callback
        }
    }

    fun clearCharacteristicDataChangedCallback(identifier: String)
    {
        synchronized(characteristicDataChangedCallbacks)
        {
            characteristicDataChangedCallbacks.remove(identifier)
        }
    }

    fun registerWriteCharacteristicCallback(identifier: String, callback: UUErrorBlock)
    {
        synchronized(writeCharacteristicCallbacks)
        {
            writeCharacteristicCallbacks[identifier] = callback
        }
    }

    fun clearWriteCharacteristicCallback(identifier: String)
    {
        synchronized(writeCharacteristicCallbacks)
        {
            writeCharacteristicCallbacks.remove(identifier)
        }
    }

    private fun popWriteCharacteristicCallback(identifier: String): UUErrorBlock?
    {
        val block: UUErrorBlock?
        synchronized(writeCharacteristicCallbacks)
        {
            block = writeCharacteristicCallbacks[identifier]
            writeCharacteristicCallbacks.remove(identifier)
        }

        return block
    }

    fun registerReadDescriptorCallback(identifier: String, callback: UUObjectErrorBlock<ByteArray>)
    {
        synchronized(readDescriptorCallbacks)
        {
            readDescriptorCallbacks[identifier] = callback
        }
    }

    fun clearReadDescriptorCallback(identifier: String)
    {
        synchronized(readDescriptorCallbacks)
        {
            readDescriptorCallbacks.remove(identifier)
        }
    }

    private fun popReadDescriptorCallback(identifier: String): UUObjectErrorBlock<ByteArray>?
    {
        val block: UUObjectErrorBlock<ByteArray>?
        synchronized(readDescriptorCallbacks)
        {
            block = readDescriptorCallbacks[identifier]
            readDescriptorCallbacks.remove(identifier)
        }

        return block
    }

    fun registerWriteDescriptorCallback(identifier: String, callback: UUErrorBlock)
    {
        synchronized(writeDescriptorCallbacks)
        {
            writeDescriptorCallbacks[identifier] = callback
        }
    }

    fun clearWriteDescriptorCallback(identifier: String)
    {
        synchronized(writeDescriptorCallbacks)
        {
            writeDescriptorCallbacks.remove(identifier)
        }
    }

    private fun popWriteDescriptorCallback(identifier: String): UUErrorBlock?
    {
        val block: UUErrorBlock?
        synchronized(writeDescriptorCallbacks)
        {
            block = writeDescriptorCallbacks[identifier]
            writeDescriptorCallbacks.remove(identifier)
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
        notifyCharacteristicRead(chr.uuHashLookup(), chr.value, UUBluetoothError.gattStatusError("onCharacteristicRead", status))

    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int)
    {
        UULog.d(javaClass, "onCharacteristicRead", "characteristic: ${characteristic.uuid}, value: ${value.uuToHex()}, status: $status")

        notifyCharacteristicRead(characteristic.uuHashLookup(), value, UUBluetoothError.gattStatusError("onCharacteristicRead", status))
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray)
    {
        UULog.d(javaClass, "onCharacteristicChanged", "characteristic: ${characteristic.uuid}, data: ${value.uuToHex()}")

        notifyCharacteristicChanged(characteristic.uuHashLookup(), value)
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
        notifyCharacteristicChanged(characteristic.uuHashLookup(), chr.value)
    }

    @Suppress("DEPRECATION")
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int)
    {
        UULog.d(javaClass, "onDescriptorRead", "characteristic: ${characteristic?.uuid}, data: ${characteristic?.value?.uuToHex()} status: $status")

        val chr = characteristic ?: return
        notifyCharacteristicWrite(chr.uuHashLookup(), UUBluetoothError.gattStatusError("onCharacteristicWrite", status))
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
        notifyDescriptorRead(desc.uuHashLookup(), desc.value, UUBluetoothError.gattStatusError("onDescriptorRead", status))
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray)
    {
        UULog.d(javaClass, "onDescriptorRead", "descriptor: ${descriptor.uuid}, data: ${value.uuToHex()} status: $status")

        notifyDescriptorRead(descriptor.uuHashLookup(), value, UUBluetoothError.gattStatusError("onDescriptorRead", status))
    }

    @Suppress("DEPRECATION")
    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int)
    {
        UULog.d(javaClass, "onDescriptorWrite", "descriptor: ${descriptor?.uuid}, data: ${descriptor?.value?.uuToHex()} status: $status")

        val desc = descriptor ?: return
        notifyDescriptorWrite(desc.uuHashLookup(), UUBluetoothError.gattStatusError("onDescriptorWrite", status))
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

        block?.dispatch(Pair(status, newState))
    }

    fun notifyServicesDiscovered(services: List<BluetoothGattService>?, error: UUError?)
    {
        val block = servicesDiscoveredCallback
        servicesDiscoveredCallback = null
        block?.dispatch(services, error)
    }

    fun notifyCharacteristicRead(
        identifier: String,
        value: ByteArray?,
        error: UUError?)
    {
        val block = popReadCharacteristicCallback(identifier)
        block?.dispatch(value, error)
    }

    private fun notifyCharacteristicChanged(
        identifier: String,
        value: ByteArray?)
    {
        val block: UUObjectBlock<ByteArray>?
        synchronized(characteristicDataChangedCallbacks)
        {
            block = characteristicDataChangedCallbacks[identifier]
            // Do not clear the callback since this can be invoked multiple times
        }

        value?.let()
        { actualValue ->
            block?.dispatch(actualValue)
        }
    }

    fun notifyCharacteristicWrite(
        identifier: String,
        error: UUError?)
    {
        val block = popWriteCharacteristicCallback(identifier)
        block?.dispatch(error)
    }

    fun notifyDescriptorRead(
        identifier: String,
        value: ByteArray?,
        error: UUError?)
    {
        val block = popReadDescriptorCallback(identifier)
        block?.dispatch(value, error)
    }

    fun notifyDescriptorWrite(
        identifier: String,
        error: UUError?)
    {
        val block = popWriteDescriptorCallback(identifier)
        block?.dispatch(error)
    }

    fun notifyRemoteRssiRead(
        value: Int?,
        error: UUError?)
    {
        val block = readRssiCallback
        readRssiCallback = null
        block?.dispatch(value, error)
    }

    fun notifyMtuChanged(
        value: Int?,
        error: UUError?)
    {
        val block = mtuChangedCallback
        mtuChangedCallback = null
        block?.dispatch(value, error)
    }

    fun notifyPhyRead(
        txPhy: Int?,
        rxPhy: Int?,
        error: UUError?)
    {
        val block = phyReadCallback
        phyReadCallback = null

        var result: Pair<Int, Int>? = null

        if (txPhy != null && rxPhy != null)
        {
            result = Pair(txPhy, rxPhy)
        }

        block?.dispatch(result, error)
    }

    fun notifyPhyUpdate(
        txPhy: Int?,
        rxPhy: Int?,
        error: UUError?)
    {
        val block = phyUpdatedCallback
        phyUpdatedCallback = null

        var result: Pair<Int, Int>? = null

        if (txPhy != null && rxPhy != null)
        {
            result = Pair(txPhy, rxPhy)
        }

        block?.dispatch(result, error)
    }

    private fun notifyReliableWriteComplete(error: UUError?)
    {
        val block = executeReliableWriteCallback
        executeReliableWriteCallback = null
        block?.dispatch(error)
    }

    private fun notifyServiceChanged()
    {
        val block = serviceChangedCallback

        // Don't null out block since this potentially could be invoked many times within a session

        block?.dispatch()
    }
}