package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.util.Locale
import java.util.UUID

@SuppressLint("MissingPermission")
class UUBluetoothAdvertiser(context: Context)
{
    private val bluetoothAdapter: BluetoothAdapter
    private val advertiser: BluetoothLeAdvertiser
    private val advertisingSetCallback: AdvertisingSetCallback = InnerAdvertiseSetCallback()
    private val gattServer: BluetoothGattServer
    private val characteristicReadDelegates: HashMap<String, ()->ByteArray> = hashMapOf()
    private val characteristicWriteDelegates: HashMap<String, (ByteArray)->Unit> = hashMapOf()

    init
    {

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        gattServer = bluetoothManager.openGattServer(context, InnerGattServerCallback())
    }

    fun start(serviceUuid: UUID, friendlyName: String, beaconRate: Int)
    {
        safeStopAdvertising()
        val start = System.currentTimeMillis()
        setLocalDeviceName(friendlyName)
        { success ->

            if (LOGGING_ENABLED)
            {
                val duration = System.currentTimeMillis() - start
                debugLog("start", "Took $duration millis to set friendly name, succes: $success")
            }

            try
            {
                val advertiseData = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(serviceUuid))
                    .setIncludeDeviceName(false)
                    .build()

                val scanResponse = AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .build()

                val params = AdvertisingSetParameters.Builder()
                    .setLegacyMode(true)
                    .setConnectable(true)
                    .setScannable(true)
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                    .setInterval(beaconRate)
                    .build()

                if (LOGGING_ENABLED)
                {
                    debugLog("start", "Begin Advertising, serviceUuid: $serviceUuid, friendlyName: $friendlyName")
                }

                advertiser.startAdvertisingSet(
                    params,
                    advertiseData,
                    scanResponse,
                    null,
                    null,
                    advertisingSetCallback
                )
            }
            catch (ex: Exception)
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("start", ex)
                }
            }
        }
    }

    fun stop()
    {
        safeStopAdvertising()

        // Reset the friendly name
        setLocalDeviceName("") {  }
    }

    private fun safeStopAdvertising()
    {
        try
        {
            advertiser.stopAdvertisingSet(advertisingSetCallback)
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("safeStopAdvertising", ex)
            }
        }
    }

    fun clearServices()
    {
        try
        {
            gattServer.clearServices()
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("clearServices", ex)
            }
        }
    }

    fun addService(service: BluetoothGattService)
    {
        try
        {
            val result = gattServer.addService(service)

            if (LOGGING_ENABLED)
            {
                debugLog("addService", "gattServer.addService returned: $result")
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("addService", ex)
            }
        }
    }

    fun registerCharacteristic(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        delegate: (ByteArray)->Unit)
    {
        try
        {
            clearServices()
            registerCharacteristicWriteDelegate(characteristicUuid, delegate)

            val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val properties = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            val permissions = BluetoothGattCharacteristic.PERMISSION_WRITE
            val characteristic = BluetoothGattCharacteristic(characteristicUuid, properties, permissions)
            var result = service.addCharacteristic(characteristic)

            if (LOGGING_ENABLED)
            {
                debugLog("registerCharacteristic", "service.addCharacteristic returned: $result")
            }

            if (result)
            {
                result = gattServer.addService(service)

                if (LOGGING_ENABLED)
                {
                    debugLog("registerCharacteristic", "gattServer.addService returned: $result")
                }
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("registerCharacteristic", ex)
            }
        }
    }

    fun registerCharacteristicReadDelegate(characteristicUuid: UUID, delegate: ()->ByteArray)
    {
        characteristicReadDelegates[characteristicUuid.toString().lowercase(Locale.getDefault())] = delegate
    }

    private fun getCharacteristicReadDelegate(characteristic: BluetoothGattCharacteristic): (()->ByteArray)?
    {
        return characteristicReadDelegates[characteristic.uuid.toString().lowercase(Locale.getDefault())]
    }

    fun registerCharacteristicWriteDelegate(characteristicUuid: UUID, delegate: (ByteArray)->Unit)
    {
        characteristicWriteDelegates[characteristicUuid.toString().lowercase(Locale.getDefault())] = delegate
    }

    private fun getCharacteristicWriteDelegate(characteristic: BluetoothGattCharacteristic): ((ByteArray)->Unit)?
    {
        return characteristicWriteDelegates[characteristic.uuid.toString().lowercase(Locale.getDefault())]
    }

    private fun setLocalDeviceName(friendlyName: String, completion: (Boolean)->Unit)
    {
        uuDispatch()
        {
            var ok = false
            try {


                //val adapter = BluetoothAdapter.getDefaultAdapter()
                val result = bluetoothAdapter.setName(friendlyName)

                if (LOGGING_ENABLED)
                {
                    debugLog("start", "bluetoothAdapter.setName returned $result")
                }
                if (friendlyName.isNotEmpty())
                {
                    var actualFriendlyName: String?
                    do
                    {
                        uuSleep("setLocalDeviceName", 5)

                        actualFriendlyName = bluetoothAdapter.name
                        if (LOGGING_ENABLED)
                        {
                            debugLog("start", "bluetoothAdapter.getName returned $actualFriendlyName")
                        }

                    } while (actualFriendlyName != null && !actualFriendlyName.equals(friendlyName, ignoreCase = true))
                }

                ok = true
            }
            catch (ex: Exception)
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("setLocalDeviceName", ex)
                }
            }

            completion(ok)
        }
    }

    private inner class InnerAdvertiseSetCallback : AdvertisingSetCallback()
    {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet, txPower: Int, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onAdvertisingSetStarted", "txPower: $txPower, status: $status")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onAdvertisingSetStopped", "")
            }
        }

        override fun onAdvertisingEnabled(advertisingSet: AdvertisingSet, enable: Boolean, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onAdvertisingEnabled", "enable: $enable, status: $status")
            }
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onAdvertisingDataSet", "status: $status")
            }
        }

        override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onScanResponseDataSet", "status: $status")
            }
        }

        override fun onAdvertisingParametersUpdated(advertisingSet: AdvertisingSet, txPower: Int, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onAdvertisingParametersUpdated", "txPower: $txPower, status: $status")
            }
        }

        override fun onPeriodicAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onPeriodicAdvertisingDataSet", "status: $status")
            }
        }

        override fun onPeriodicAdvertisingEnabled(advertisingSet: AdvertisingSet, enable: Boolean, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onPeriodicAdvertisingEnabled", "enable: $enable, status: $status")
            }
        }

        override fun onPeriodicAdvertisingParametersUpdated(advertisingSet: AdvertisingSet, status: Int)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("onPeriodicAdvertisingParametersUpdated", "status: $status")
            }
        }
    }

    private inner class InnerGattServerCallback : BluetoothGattServerCallback()
    {
        private fun sendResponse(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            status: Int,
            response: ByteArray?)
        {
            try
            {
                gattServer.sendResponse(device, requestId, status, offset, response)
            }
            catch (ex: Exception)
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("sendResponse", ex)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic)
        {
            if (LOGGING_ENABLED)
            {
                debugLog(
                    "onCharacteristicReadRequest", "device: " + device.address +
                            ", requestId: " + requestId +
                            ", offset: " + offset +
                            ", characteristic: " + characteristic.uuid.toString()
                )
            }

            var response: ByteArray?
            var status = BluetoothGatt.GATT_SUCCESS

            try
            {
                val delegate = getCharacteristicReadDelegate(characteristic)
                response = delegate?.invoke()
            }
            catch (ex: Exception)
            {
                status = BluetoothGatt.GATT_FAILURE
                response = null

                if (LOGGING_ENABLED)
                {
                    debugLog("onCharacteristicReadRequest", ex)
                }
            }

            sendResponse(device, requestId, offset, status, response)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (LOGGING_ENABLED)
            {
                debugLog(
                    "onCharacteristicWriteRequest", "device: " + device.address +
                            ", requestId: " + requestId +
                            ", characteristic: " + characteristic.uuid.toString() +
                            ", preparedWrite: " + preparedWrite +
                            ", responseNeeded: " + responseNeeded +
                            ", offset: " + offset +
                            ", value: " + value.uuToHex()
                )
            }

            var response: ByteArray?
            var status = BluetoothGatt.GATT_SUCCESS

            try
            {
                val delegate = getCharacteristicWriteDelegate(characteristic)
                delegate?.invoke(value)
            }
            catch (ex: Exception)
            {
                status = BluetoothGatt.GATT_FAILURE

                if (LOGGING_ENABLED)
                {
                    debugLog("onCharacteristicWriteRequest", ex)
                }
            }

            sendResponse(device, requestId, offset, status, value)
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onConnectionStateChange", "device: " + device.address +
                            ", status: " + status +
                            ", newState: " + newState
                )
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onDescriptorReadRequest", "device: " + device.address +
                            ", requestId: " + requestId +
                            ", offset: " + offset +
                            ", descriptor: " + descriptor.uuid.toString()
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onDescriptorWriteRequest", "device: " + device.address +
                            ", requestId: " + requestId +
                            ", descriptor: " + descriptor.uuid.toString() +
                            ", preparedWrite: " + preparedWrite +
                            ", responseNeeded: " + responseNeeded +
                            ", offset: " + offset +
                            ", value: " + value.uuToHex()
                )
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onExecuteWrite",
                    "device: " + device.address + ", requestId: " + requestId + ", execute: " + execute
                )
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            if (LOGGING_ENABLED) {
                debugLog("onMtuChanged", "device: " + device.address + ", mtu: " + mtu)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (LOGGING_ENABLED) {
                debugLog("onNotificationSent", "device: " + device.address + ", status: " + status)
            }
        }

        override fun onPhyRead(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onPhyRead",
                    "device: " + device.address + ", txPhy: " + txPhy + ", rxPhy: " + rxPhy + ", status: " + status
                )
            }
        }

        override fun onPhyUpdate(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onPhyUpdate",
                    "device: " + device.address + ", txPhy: " + txPhy + ", rxPhy: " + rxPhy + ", status: " + status
                )
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            if (LOGGING_ENABLED) {
                debugLog(
                    "onServiceAdded",
                    "status: " + status + ", service: " + service.uuid.toString()
                )
            }
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

    companion object
    {
        private val LOGGING_ENABLED: Boolean = true
    }
}