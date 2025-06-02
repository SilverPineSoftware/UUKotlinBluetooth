package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import com.silverpine.uu.bluetooth.internal.safeNotify
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimedMetric
import com.silverpine.uu.logging.UULog
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

open class UUBluetoothDevicePeripheralSession(
    override val peripheral: UUPeripheral,

) : UUPeripheralSession
{
    override var configuration: UUPeripheralSessionConfiguration = UUPeripheralSessionConfiguration()
    override var discoveredServices: ArrayList<BluetoothGattService> = arrayListOf()
        protected set

    override var discoveredCharacteristics: ConcurrentHashMap<UUID, List<BluetoothGattCharacteristic>> =
        ConcurrentHashMap()
        protected set

    override var discoveredDescriptors: ConcurrentHashMap<UUID, List<BluetoothGattDescriptor>> =
        ConcurrentHashMap()
        protected set

    override var sessionEndError: UUError? = null
        protected set

    override var started: UUPeripheralSessionStartedCallback? = null
    override var ended: UUPeripheralSessionErrorCallback? = null

    private val connectTimeMeasurement = UUTimedMetric("connectTime")
    private val disconnectTimeMeasurement = UUTimedMetric("disconnectTime")
    private val serviceDiscoveryTimeMeasurement = UUTimedMetric("serviceDiscoveryTime")
    private val characteristicDiscoveryTimeMeasurement = UUTimedMetric("characteristicDiscoveryTime")
    private val descriptorDiscoveryTimeMeasurement = UUTimedMetric("descriptorDiscoveryTime")

    override fun start()
    {
        connect()
    }

    override fun end(error: UUError?)
    {
        UULog.d(javaClass, "end", "Session ending with error: $error")

        sessionEndError = error
        disconnect()
    }

    /*
    public func startTimer(name: String, timeout: TimeInterval, block: @escaping ()->())
    {
        peripheral.startTimer(name: name, timeout: timeout, block: block)
    }

    public func cancelTimer(name: String)
    {
        peripheral.cancelTimer(name: name)
    }*/

    override fun startTimer(name: String, timeout: Long, block: () -> Unit)
    {

    }

    override fun cancelTimer(name: String)
    {

    }

    override fun read(
        characteristic: UUID,
        completion: UUPeripheralSessionObjectErrorCallback<ByteArray>)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, null, err)
            return
        }

        peripheral.read(
            characteristic = char,
            timeout = configuration.readTimeout,
            completion =
            { data, error ->
                completion.safeNotify(this, data, error)
            })
    }

    override fun write(
        data: ByteArray,
        characteristic: UUID,
        withResponse: Boolean,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, err)
            return
        }

        if (withResponse)
        {
            peripheral.write(
                data = data,
                characteristic = char,
                timeout = configuration.writeTimeout,
                completion =
                { error: UUError? ->
                    completion.safeNotify(this, error)
                })
        }
        else
        {
            peripheral.writeWithoutResponse(
                data = data,
                characteristic = char,
                completion =
                { error: UUError? ->
                    completion.safeNotify(this, error)
                })
        }
    }

    override fun startListeningForDataChanges(
        characteristic: UUID,
        dataChanged: UUPeripheralSessionObjectErrorCallback<ByteArray>,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion.safeNotify(this, err)
            return
        }

        peripheral.setNotifyValue(
            enabled = true,
            characteristic = char,
            timeout = configuration.readTimeout,
            notifyHandler =
            { updatedCharacteristic, updatedData ->

                //TODO: Update char in discovered map?

                dataChanged.safeNotify(this, updatedData, null)
            },
            completion =
            { updatedCharacteristic, error ->
                completion.safeNotify(this, error)
            }
        )
    }

    override fun stopListeningForDataChanges(
        characteristic: UUID,
        completion: UUPeripheralSessionErrorCallback)
    {
        val char = findDiscoveredCharacteristic(characteristic) ?: run()
        {
            val err = UUBluetoothError.missingRequiredCharacteristic(characteristic)
            completion(this, err)
            return
        }

        peripheral.setNotifyValue(
            enabled = true,
            characteristic = char,
            timeout = configuration.readTimeout,
            notifyHandler = null,
            completion =
            { updatedCharacteristic, error ->
                completion(this, error)
            }
        )
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection & Disconnection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun connect()
    {
        connectTimeMeasurement.start()

        peripheral.connect(
            timeout = configuration.connectTimeout,
            connected = this::handleConnected,
            disconnected = this::handleDisconnection)
    }

    private fun handleSessionStarted()
    {
        finishSessionStart()
        {
            started?.safeNotify(this)
        }
    }

    private fun disconnect()
    {
        disconnectTimeMeasurement.start()
        peripheral.disconnect(timeout = configuration.disconnectTimeout)
    }

    private fun handleConnected()
    {
        connectTimeMeasurement.end()
        startServiceDiscovery()
    }

    private fun handleDisconnection(disconnectError: UUError?)
    {
        disconnectTimeMeasurement.end()

        // Only set error if not already set.  In the case where end(error) forcefully ends the session, preserve that error.
        if (sessionEndError != null)
        {
            sessionEndError = disconnectError
        }

        ended?.safeNotify(this, sessionEndError)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Service Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startServiceDiscovery()
    {
        serviceDiscoveryTimeMeasurement.start()

        discoveredServices.clear()

        peripheral.discoverServices(timeout = configuration.serviceDiscoveryTimeout)
        { services, error ->

            serviceDiscoveryTimeMeasurement.end()

            error?.let()
            { err ->
                end(err)
                return@discoverServices
            }

            if (services == null)
            {
                val err = UUBluetoothError.makeError(UUBluetoothErrorCode.NoServicesDiscovered)
                end(err)
                return@discoverServices
            }

            discoveredServices.addAll(services)
            logDiscoveredServices()
            startCharacteristicDiscovery()
        }
    }

    private fun logDiscoveredServices()
    {
        UULog.d(javaClass, "discoverServices", "Discovered ${discoveredServices.size} services.")
        discoveredServices.forEach()
        { service ->
            //let serviceDescription = UUServiceRepresentation(from: service)
            //UULog.debug(tag: LOG_TAG, message: "Service: \(serviceDescription.uuToJsonString())")
            UULog.d(javaClass, "discoverServices", "Service: ${service.uuid}, ${service.uuid.uuCommonName}")
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Characteristic Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startCharacteristicDiscovery()
    {
        characteristicDiscoveryTimeMeasurement.start()
        discoveredCharacteristics.clear()

        discoveredServices.forEach()
        { service ->
            discoveredCharacteristics[service.uuid] = service.characteristics
        }

        characteristicDiscoveryTimeMeasurement.end()
        logDiscoveredCharacteristics()

        startDescriptorDiscovery()
    }

    private fun logDiscoveredCharacteristics()
    {
        discoveredCharacteristics.forEach()
        { service, characteristics ->
            UULog.d(javaClass, "discoverCharacteristics", "Discovered ${characteristics.size} characteristics on service $service")

            characteristics.forEach()
            { characteristic ->
                //let characteristicDescription = UUCharacteristicRepresentation(from: characteristic)
                //UULog.debug(tag: LOG_TAG, message: "Characteristic: \(characteristicDescription.uuToJsonString())")

                UULog.d(javaClass, "discoverCharacteristics", "Characteristic: ${characteristic.uuid}, ${characteristic.uuid.uuCommonName}")
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Descriptor Discovery
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun startDescriptorDiscovery()
    {
        descriptorDiscoveryTimeMeasurement.start()
        discoveredDescriptors.clear()

        discoveredCharacteristics.values.flatten().forEach()
        { characteristic ->
            discoveredDescriptors[characteristic.uuid] = characteristic.descriptors
        }

        descriptorDiscoveryTimeMeasurement.end()
        logDiscoveredDescriptors()

        handleSessionStarted()
    }

    private fun logDiscoveredDescriptors()
    {
        discoveredDescriptors.forEach()
        { characteristic, descriptors ->
            UULog.d(javaClass, "discoverDescriptors", "Discovered ${descriptors.size} descriptors on characteristic $characteristic")

            descriptors.forEach()
            { descriptor ->
                //let descriptorDescription = UUDescriptorRepresentation(from: descriptor)
                //UULog.debug(tag: LOG_TAG, message: "Descriptor: \(descriptorDescription.uuToJsonString())")

                UULog.d(javaClass, "discoverDescriptors", "Descriptor: ${descriptor.uuid}, ${descriptor.uuid.uuCommonName}")
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Additional Private Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun findDiscoveredCharacteristic(uuid: UUID): BluetoothGattCharacteristic?
    {
        return discoveredCharacteristics.values.flatten().firstOrNull { it.uuid == uuid }
    }

    open fun finishSessionStart(completion: ()->Unit)
    {
        completion()
    }
}