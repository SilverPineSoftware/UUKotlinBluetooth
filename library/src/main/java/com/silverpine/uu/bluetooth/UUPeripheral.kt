package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import java.util.Arrays
import java.util.Locale
import java.util.Objects

/**
 * Wrapper class around a BTLE scanning result.
 */
open class UUPeripheral() : Parcelable
{
    object Defaults
    {
        const val ConnectTimeout = 60000
        const val DisconnectTimeout = 10000
        const val ServiceDiscoveryTimeout = 60000
        const val OperationTimeout = 60000
    }

    enum class ConnectionState
    {
        Connecting, Connected, Disconnecting, Disconnected;

        companion object
        {
            fun fromString(string: String?): ConnectionState
            {
                for (s in values())
                {
                    if (s.toString().equals(string, ignoreCase = true))
                    {
                        return s
                    }
                }

                return Disconnected
            }

            fun fromProfileConnectionState(state: Int): ConnectionState
            {
                when (state)
                {
                    BluetoothProfile.STATE_CONNECTED -> return Connected
                    BluetoothProfile.STATE_CONNECTING -> return Connecting
                    BluetoothProfile.STATE_DISCONNECTING -> return Disconnecting
                    BluetoothProfile.STATE_DISCONNECTED -> return Disconnected
                }

                return Disconnected
            }
        }
    }

    private var device: BluetoothDevice? = null

    var scanRecord: ByteArray? = null
        private set

    var rssi = 0
        private set

    var lastRssiUpdateTime: Long = 0
        private set

    var manufacturingData: ByteArray? = null

    private var flags: ByteArray? = null

    private var localName: String? = null
    val serviceUuids: ArrayList<String> = ArrayList()

    var firstAdvertisementTime: Long = 0
        private set
    var lastAdvertisementTime: Long = 0
        private set
    private var totalBeaconCount: Long = 0
    private var bluetoothGatt: BluetoothGatt? = null
    var negotiatedMtuSize: Int? = null

    constructor(device: BluetoothDevice) : this()
    {
        this.device = device
        firstAdvertisementTime = 0
        totalBeaconCount = 0
    }

    val bluetoothDevice: BluetoothDevice
        get() = (device)!!

    val address: String?
        get() = device?.address

    open val name: String?
        @SuppressLint("MissingPermission")
        get() = localName ?: device?.name

    fun getServiceUuids(): Array<String?>
    {
        val list = arrayOfNulls<String>(serviceUuids!!.size)
        serviceUuids.toArray(list)
        return list
    }

    fun hasServiceUuid(uuidToCheck: String?): Boolean
    {
        if (uuidToCheck != null)
        {
            for (uuid in serviceUuids)
            {
                if (uuid.equals(uuidToCheck, ignoreCase = true))
                {
                    return true
                }
            }
        }

        return false
    }

    fun updateRssi(updatedRssi: Int)
    {
        rssi = updatedRssi
        lastRssiUpdateTime = System.currentTimeMillis()
    }

    open fun updateAdvertisement(rssi: Int, scanRecord: ByteArray?)
    {
        this.scanRecord = scanRecord
        if (firstAdvertisementTime == 0L)
        {
            firstAdvertisementTime = System.currentTimeMillis()
        }

        debugLog("updateAdvertisement", totalBeaconCount.toString() + ", timeSinceLastAdvertisement: " + timeSinceLastUpdate + ", scanRecord: ${scanRecord?.uuToHex()}")
        lastAdvertisementTime = System.currentTimeMillis()
        ++totalBeaconCount
        updateRssi(rssi)
        parseScanRecord()
    }

    fun totalBeaconCount(): Long
    {
        return totalBeaconCount
    }

    fun averageBeaconRate(): Double
    {
        val avg = 0.0
        val timeSinceFirstBeacon = lastAdvertisementTime - firstAdvertisementTime
        return totalBeaconCount.toDouble() / timeSinceFirstBeacon.toDouble() * 1000.0f
    }

    val connectionState: ConnectionState
        @SuppressLint("MissingPermission")
        get()
        {
            val bluetoothManager = UUBluetooth.requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            var state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)
            debugLog("getConnectionState", "Actual connection state is: $state (${ConnectionState.fromProfileConnectionState(state)})")
            val gatt = uuGatt

            if (gatt != null)
            {
                if ((state != BluetoothProfile.STATE_CONNECTING) && gatt.isConnecting)
                {
                    debugLog("getConnectionState", "Forcing state to connecting")
                    state = BluetoothProfile.STATE_CONNECTING
                }
                else if (state != BluetoothProfile.STATE_DISCONNECTED && gatt.bluetoothGatt == null)
                {
                    debugLog("getConnectionState", "Forcing state to disconnected")
                    state = BluetoothProfile.STATE_DISCONNECTED
                }
            }
            return ConnectionState.fromProfileConnectionState(state)
        }

    fun setBluetoothGatt(gatt: BluetoothGatt?)
    {
        bluetoothGatt = gatt
    }

    private val uuGatt: UUBluetoothGatt?
        get()
        {
            return UUBluetoothGattManager.gattForPeripheral(this)
        }

    fun requestHighPriority(delegate: UUPeripheralBoolDelegate)
    {
        uuGatt?.requestHighPriority(delegate)
    }

    fun requestMtuSize(timeout: Long, mtuSize: Int, delegate: UUPeripheralErrorDelegate)
    {
        uuGatt?.requestMtuSize(timeout, mtuSize)
        { peripheral, error ->

            if (error == null) {
                negotiatedMtuSize = peripheral.negotiatedMtuSize
            } else {
                negotiatedMtuSize = null
            }

            delegate.invoke(peripheral, error)
        }
    }

    fun connect(
        connectTimeout: Long,
        disconnectTimeout: Long,
        connected: ()->Unit,
        disconnected: (UUError?)->Unit)
    {
        uuGatt?.connect(false, connectTimeout, disconnectTimeout, object : UUConnectionDelegate
        {
            override fun onConnected(peripheral: UUPeripheral)
            {
                connected()
            }

            override fun onDisconnected(peripheral: UUPeripheral, error: UUError?)
            {
                disconnected(error)
            }
        })
    }

    fun disconnect(error: UUError?)
    {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.disconnect("peripheral.disconnect", error)
        }
    }

    fun discoverServices(
        timeout: Long,
        delegate: UUDiscoverServicesDelegate)
    {
        val gatt = uuGatt

        gatt?.discoverServices(timeout)
        { _, error ->
            delegate.invoke(
                discoveredServices(),
                error
            )
        }
    }

    fun discoveredServices(): ArrayList<BluetoothGattService>
    {
        acquireExistingGatt()

        val list = ArrayList<BluetoothGattService>()
        if (bluetoothGatt != null)
        {
            list.addAll(bluetoothGatt!!.services)
        }

        return list
    }

    fun getDiscoveredService(uuid: String): BluetoothGattService?
    {
        val list = discoveredServices()
        for (svc in list) {
            if (svc.uuid.toString().equals(uuid, ignoreCase = true)) {
                return svc
            }
        }
        return null
    }

    fun setNotifyState(
        characteristic: BluetoothGattCharacteristic,
        notifyState: Boolean,
        timeout: Long,
        notifyDelegate: UUCharacteristicDelegate?,
        delegate: UUCharacteristicDelegate
    ) {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.setNotifyState(characteristic, notifyState, timeout, notifyDelegate, delegate)
        }
    }

    fun readCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        timeout: Long,
        delegate: UUCharacteristicDelegate
    ) {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.readCharacteristic(characteristic, timeout, delegate)
        }
    }

    fun readDescriptor(
        descriptor: BluetoothGattDescriptor,
        timeout: Long,
        delegate: UUDescriptorDelegate
    )
    {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.readDescriptor(descriptor, timeout, delegate)
        }
    }

    fun writeDescriptor(
        descriptor: BluetoothGattDescriptor,
        data: ByteArray,
        timeout: Long,
        delegate: UUDescriptorDelegate
    )
    {
        val gatt = uuGatt
        if (gatt != null)
        {
            gatt.writeDescriptor(descriptor, data, timeout, delegate)
        }
    }

    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        timeout: Long,
        delegate: UUCharacteristicDelegate
    )
    {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.writeCharacteristic(characteristic, data, timeout, delegate)
        }
    }

    fun writeCharacteristicWithoutResponse(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        timeout: Long,
        delegate: UUCharacteristicDelegate
    )
    {
        val gatt = uuGatt
        if (gatt != null)
        {
            gatt.writeCharacteristicWithoutResponse(characteristic, data, timeout, delegate)
        }
    }

    fun readRssi(
        timeout: Long,
        delegate: UUPeripheralErrorDelegate
    )
    {
        val gatt = uuGatt
        if (gatt != null)
        {
            gatt.readRssi(timeout, delegate)
        }
    }

    fun startRssiPolling(context: Context, interval: Long, delegate: UUPeripheralDelegate)
    {
        val gatt = uuGatt

        if (gatt != null)
        {
            gatt.startRssiPolling(context, interval, delegate)
        }
    }

    fun stopRssiPolling()
    {
        val gatt = uuGatt
        gatt?.stopRssiPolling()
    }

    val isPollingForRssi: Boolean
        get()
        {
            var isPolling = false
            val gatt = uuGatt
            if (gatt != null)
            {
                isPolling = gatt.isPollingForRssi
            }

            return isPolling
        }

    val timeSinceLastUpdate: Long
        get() = System.currentTimeMillis() - lastAdvertisementTime

    private fun parseScanRecord()
    {
        serviceUuids.clear()

        val bytes = scanRecord ?: return

        var index = 0
        while (index < bytes.size)
        {
            val length = bytes.uuReadUInt8(index)
            index += Byte.SIZE_BYTES
            if (length == 0)
            {
                break
            }

            val dataType = bytes.uuReadUInt8(index)
            index += Byte.SIZE_BYTES

            val dataLength = length - 1
            val data = bytes.uuSubData(index, dataLength)
            if (data == null)
            {
                debugLog("parse", "Unable to get data chunk at index $index with count $dataLength")
                break
            }

            if (data.size != dataLength)
            {
                debugLog("parse", "Data length is wrong. Expected $dataLength but got ${data.size}")
            }

            index += data.size

            when (dataType.toByte())
            {
                DATA_TYPE_FLAGS ->
                {
                    parseFlags(data)
                }

                DATA_TYPE_MANUFACTURING_DATA ->
                {
                    manufacturingData = data
                }

                DATA_TYPE_COMPLETE_LOCAL_NAME ->
                {
                    localName = data.uuUtf8()
                }

                DATA_TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,
                DATA_TYPE_COMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS ->
                {
                    parseServiceUuid(data, 2)
                }

                DATA_TYPE_COMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                DATA_TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS ->
                {
                    parseServiceUuid(data, 16)
                }
            }
        }

        manufacturingData?.let()
        {
            parseManufacturingData(it)
        }

    }

    private fun parseFlags(data: ByteArray)
    {
        flags = data
        debugLog("parseFlags", "Flags are: ${flags?.uuToHex()}")
    }

    private fun parseServiceUuid(data: ByteArray, length: Int)
    {
        var index = 0
        while (index < data.size)
        {
            val subData = data.uuSubData(index, length)
            val hexChunk = subData?.uuToHex() ?: continue
            if (hexChunk.isNotEmpty())
            {
                serviceUuids.add(hexChunk)
            }

            index += length
        }
    }

    open fun parseManufacturingData(manufacturingData: ByteArray)
    {
        // Default does nothing
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // System.Object overrides
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun toString(): String
    {
        return try
        {
            java.lang.String.format(
                Locale.US,
                "%s, %s, %d, %s", address, name, rssi, manufacturingData?.uuToHex())
        }
        catch (ex: Exception)
        {
            super.toString()
        }
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other)
        {
            return true
        }

        if (other !is UUPeripheral)
        {
            return false
        }

        return ((((rssi == other.rssi) && lastRssiUpdateTime == other.lastRssiUpdateTime) && firstAdvertisementTime == other.firstAdvertisementTime) && lastAdvertisementTime == other.lastAdvertisementTime) && totalBeaconCount == other.totalBeaconCount &&
                (device == other.device) &&
                Arrays.equals(scanRecord, other.scanRecord) &&
                Arrays.equals(manufacturingData, other.manufacturingData) &&
                Arrays.equals(flags, other.flags) &&
                (localName == other.localName) && serviceUuids == other.serviceUuids &&
                (bluetoothGatt == other.bluetoothGatt)
    }

    override fun hashCode(): Int
    {
        var result = Objects.hash(
            device,
            rssi,
            lastRssiUpdateTime,
            localName,
            serviceUuids,
            firstAdvertisementTime,
            lastAdvertisementTime,
            totalBeaconCount,
            bluetoothGatt
        )

        result = 31 * result + Arrays.hashCode(scanRecord)
        result = 31 * result + Arrays.hashCode(manufacturingData)
        result = 31 * result + Arrays.hashCode(flags)
        return result
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // JSON
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    private static final String JSON_DEVICE_KEY = "device";
    private static final String JSON_RSSI_KEY = "rssi";
    private static final String JSON_RSSI_LAST_UPDATE_KEY = "rssi_last_updated";
    private static final String JSON_SCAN_RECORD_KEY = "scanRecord";
    private static final String JSON_FIRST_ADVERTISEMENT_KEY = "first_advertisement";
    private static final String JSON_LAST_ADVERTISEMENT_KEY = "last_advertisement";
    private static final String JSON_TOTAL_BEACON_COUNT_KEY = "total_beacon_count";

    @NonNull
    @Override
    public JSONObject toJsonObject()
    {
        JSONObject o = new JSONObject();

        UUJson.safePut(o, JSON_DEVICE_KEY, UUString.byteToHex(UUParcel.serializeParcel(device)));
        UUJson.safePut(o, JSON_SCAN_RECORD_KEY, UUString.byteToHex(scanRecord));
        UUJson.safePut(o, JSON_RSSI_KEY, rssi);
        UUJson.safePut(o, JSON_RSSI_LAST_UPDATE_KEY, lastRssiUpdateTime);
        UUJson.safePut(o, JSON_FIRST_ADVERTISEMENT_KEY, firstAdvertisementTime);
        UUJson.safePut(o, JSON_LAST_ADVERTISEMENT_KEY, lastAdvertisementTime);
        UUJson.safePut(o, JSON_TOTAL_BEACON_COUNT_KEY, totalBeaconCount);

        return o;
    }

    @Override
    public void fillFromJson(@NonNull final JSONObject json)
    {
        device = UUParcel.deserializeParcelable(BluetoothDevice.CREATOR, UUString.hexToByte(UUJson.safeGetString(json, JSON_DEVICE_KEY)));
        scanRecord = UUString.hexToByte(UUJson.safeGetString(json, JSON_SCAN_RECORD_KEY));
        rssi = UUJson.safeGetInt(json, JSON_RSSI_KEY);
        lastRssiUpdateTime = UUJson.safeGetLong(json, JSON_RSSI_LAST_UPDATE_KEY);
        firstAdvertisementTime = UUJson.safeGetLong(json, JSON_FIRST_ADVERTISEMENT_KEY);
        lastAdvertisementTime = UUJson.safeGetLong(json, JSON_LAST_ADVERTISEMENT_KEY);
        totalBeaconCount = UUJson.safeGetLong(json, JSON_TOTAL_BEACON_COUNT_KEY);

        // Fill in derived data from scan record
        parseScanRecord();
    }*/

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Parcelable
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun describeContents(): Int
    {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int)
    {
        dest.writeParcelable(device, flags)

        if (scanRecord != null)
        {
            dest.writeByte(1.toByte())
            dest.writeInt(scanRecord!!.size)
            dest.writeByteArray(scanRecord)
        }
        else
        {
            dest.writeByte(0.toByte())
        }
        dest.writeInt(rssi)
        dest.writeLong(lastAdvertisementTime)
        dest.writeLong(firstAdvertisementTime)
        dest.writeLong(lastAdvertisementTime)
        dest.writeLong(totalBeaconCount)
    }

    constructor(parcel: Parcel) : this()
    {
        device = parcel.readParcelable(BluetoothDevice::class.java.classLoader)
        if (parcel.readByte().toInt() == 1)
        {
            val scanRecordLength = parcel.readInt()
            scanRecord = ByteArray(scanRecordLength)
            parcel.readByteArray(scanRecord!!)
        }

        rssi = parcel.readInt()
        lastRssiUpdateTime = parcel.readLong()
        firstAdvertisementTime = parcel.readLong()
        lastAdvertisementTime = parcel.readLong()
        totalBeaconCount = parcel.readLong()

        // Fill in derived data from scan record
        parseScanRecord()
    }

    private fun acquireExistingGatt()
    {
        val gatt = uuGatt

        if (gatt != null)
        {
            setBluetoothGatt(gatt.bluetoothGatt)
        }
    }

    private fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, message)
        }
    }

    @Synchronized
    private fun debugLog(method: String, exception: Throwable)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, "", exception)
        }
    }

    /*override fun writeToParcel(parcel: Parcel, flags: Int)
    {
        parcel.writeParcelable(device, flags)
        parcel.writeByteArray(manufacturingData)
        parcel.writeByteArray(flags)
        parcel.writeString(localName)
        parcel.writeLong(totalBeaconCount)
        parcel.writeValue(negotiatedMtuSize)
    }

    override fun describeContents(): Int
    {
        return 0
    }*/

    companion object CREATOR : Parcelable.Creator<UUPeripheral>
    {
        private val LOGGING_ENABLED = true //BuildConfig.DEBUG
        private const val DATA_TYPE_FLAGS: Byte = 0x01
        private const val DATA_TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS: Byte = 0x02
        private const val DATA_TYPE_COMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS: Byte = 0x03
        private const val DATA_TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS: Byte = 0x06
        private const val DATA_TYPE_COMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS: Byte = 0x07
        private const val DATA_TYPE_COMPLETE_LOCAL_NAME: Byte = 0x09
        private const val DATA_TYPE_MANUFACTURING_DATA = 0xFF.toByte()

        // Number of overhead bytes that need to be accounted for when calculating the max read/write
        // size of a BLE characteristics
        private const val BLE_PACKET_SIZE_MIN = 23 // 20 + BLE_PACKET_OVERHEAD
        private const val BLE_PACKET_SIZE_MAX = 512
        private const val BLE_PACKET_OVERHEAD = 3

        override fun createFromParcel(parcel: Parcel): UUPeripheral
        {
            return UUPeripheral(parcel)
        }

        override fun newArray(size: Int): Array<UUPeripheral>
        {
            return newArray(size)
        }
    }
}