package com.silverpine.uu.sample.bluetooth.tisensortag

import android.util.Log
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralSession
import com.silverpine.uu.bluetooth.UUPeripheralSessionObjectErrorCallback
import com.silverpine.uu.bluetooth.readUByte
import com.silverpine.uu.bluetooth.writeUByte
import com.silverpine.uu.bluetooth.writeUShort
import com.silverpine.uu.core.UUError
import java.nio.ByteOrder
import java.util.UUID

/**
 * UUID constants for TI SensorTag
 */
object TiSensorTag {
    object Keys {
        val service: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val data: UUID    = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    }

    object Gyroscope {
        val service: UUID = UUID.fromString("f000aa50-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa51-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa52-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa53-0451-4000-b000-000000000000")
    }

    object Accelerometer {
        val service: UUID = UUID.fromString("f000aa10-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa11-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa12-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa13-0451-4000-b000-000000000000")
    }

    object Temperature {
        val service: UUID = UUID.fromString("f000aa00-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa01-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa02-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa03-0451-4000-b000-000000000000")
    }

    object Humidity {
        val service: UUID = UUID.fromString("f000aa20-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa21-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa22-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa23-0451-4000-b000-000000000000")
    }

    object Barometer {
        val service: UUID     = UUID.fromString("f000aa40-0451-4000-b000-000000000000")
        val data: UUID        = UUID.fromString("f000aa41-0451-4000-b000-000000000000")
        val config: UUID      = UUID.fromString("f000aa42-0451-4000-b000-000000000000")
        val calibration: UUID = UUID.fromString("f000aa43-0451-4000-b000-000000000000")
        val period: UUID      = UUID.fromString("f000aa44-0451-4000-b000-000000000000")
    }

    object Magnetometer {
        val service: UUID = UUID.fromString("f000aa30-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa31-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa32-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa33-0451-4000-b000-000000000000")
    }

    object Light {
        val service: UUID = UUID.fromString("f000aa70-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa71-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa72-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa73-0451-4000-b000-000000000000")
    }

    object Movement {
        val service: UUID = UUID.fromString("f000aa80-0451-4000-b000-000000000000")
        val data: UUID    = UUID.fromString("f000aa81-0451-4000-b000-000000000000")
        val config: UUID  = UUID.fromString("f000aa82-0451-4000-b000-000000000000")
        val period: UUID  = UUID.fromString("f000aa83-0451-4000-b000-000000000000")
    }

    object Services {
        val oad: UUID     = UUID.fromString("f000ffc0-0451-4000-b000-000000000000")
        val io: UUID      = UUID.fromString("f000aa64-0451-4000-b000-000000000000")
        val register2: UUID = UUID.fromString("f000ac00-0451-4000-b000-000000000000")
        val display: UUID = UUID.fromString("f000ad00-0451-4000-b000-000000000000")
    }

    val TI_OAD_IMAGE_NOTIFY: UUID = UUID.fromString("f000ffc1-0451-4000-b000-000000000000")
    val TI_OAD_IMAGE_BLOCK_REQUEST: UUID = UUID.fromString("f000ffc2-0451-4000-b000-000000000000")
    val TI_CONNECTION_CONTROL_SERVICE: UUID = UUID.fromString("f000ccc0-0451-4000-b000-000000000000")
    val TI_CONNECTION_CONTROL_CURRENT_USED_PARAMETERS: UUID = UUID.fromString("f000ccc1-0451-4000-b000-000000000000")
    val TI_CONNECTION_CONTROL_REQUEST_NEW_PARAMETERS: UUID = UUID.fromString("f000ccc2-0451-4000-b000-000000000000")
    val TI_CONNECTION_CONTROL_DISCONNECT_REQUEST: UUID = UUID.fromString("f000ccc3-0451-4000-b000-000000000000")
    val TI_CONNECTION_CONTROL_NAP_INTERVAL_SETTING: UUID = UUID.fromString("f000ccc4-0451-4000-b000-000000000000")
    val IO_DATA: UUID = UUID.fromString("f000aa65-0451-4000-b000-000000000000")
    val IO_CONFIG: UUID = UUID.fromString("f000aa66-0451-4000-b000-000000000000")
    val TWO_REGISTER_DATA: UUID = UUID.fromString("f000ac01-0451-4000-b000-000000000000")
    val TWO_REGISTER_ADDRESS: UUID = UUID.fromString("f000ac02-0451-4000-b000-000000000000")
    val TWO_REGISTER_DEVICE_ID: UUID = UUID.fromString("f000ac03-0451-4000-b000-000000000000")
    val TWO_DISPLAY_DATA: UUID = UUID.fromString("f000ad01-0451-4000-b000-000000000000")
    val TWO_DISPLAY_CONTROL: UUID = UUID.fromString("f000ad02-0451-4000-b000-000000000000")

    private fun appSpecName(uuid: UUID, name: String) {
        // Implement registration of human-readable names if needed
    }

    fun addSpecNames() {
        appSpecName(Keys.service, "TI SimpleLink Keys Service")
        appSpecName(Keys.data, "TI SimpleLink Keys Key Press State")
        appSpecName(Services.oad, "TI OAD Service")
        appSpecName(TI_OAD_IMAGE_NOTIFY, "TI OAD Image Notify")
        appSpecName(TI_OAD_IMAGE_BLOCK_REQUEST, "TI OAD Image Block Request")
        appSpecName(TI_CONNECTION_CONTROL_SERVICE, "TI Connection Control Service")
        appSpecName(TI_CONNECTION_CONTROL_CURRENT_USED_PARAMETERS, "TI Connection Control Current Used Parameters")
        appSpecName(TI_CONNECTION_CONTROL_REQUEST_NEW_PARAMETERS, "TI Connection Control Request New Parameters")
        appSpecName(TI_CONNECTION_CONTROL_DISCONNECT_REQUEST, "TI Connection Control Disconnect Request")
        appSpecName(TI_CONNECTION_CONTROL_NAP_INTERVAL_SETTING, "TI Connection Control Nap Interval Setting")
        appSpecName(Temperature.service, "TI SensorTag Temperature Service")
        appSpecName(Temperature.data, "TI SensorTag Temperature Data")
        appSpecName(Temperature.config, "TI SensorTag Temperature Config")
        appSpecName(Temperature.period, "TI SensorTag Temperature Period")
        appSpecName(Accelerometer.service, "TI SensorTag Accelerometer Service")
        appSpecName(Accelerometer.data, "TI SensorTag Accelerometer Data")
        appSpecName(Accelerometer.config, "TI SensorTag Accelerometer Config")
        appSpecName(Accelerometer.period, "TI SensorTag Accelerometer Period")
        appSpecName(Humidity.service, "TI SensorTag Humidity Service")
        appSpecName(Humidity.data, "TI SensorTag Humidity Data")
        appSpecName(Humidity.config, "TI SensorTag Humidity Config")
        appSpecName(Humidity.period, "TI SensorTag Humidity Period")
        appSpecName(Magnetometer.service, "TI SensorTag Magnetometer Service")
        appSpecName(Magnetometer.data, "TI SensorTag Magnetometer Data")
        appSpecName(Magnetometer.config, "TI SensorTag Magnetometer Config")
        appSpecName(Magnetometer.period, "TI SensorTag Magnetometer Period")
        appSpecName(Barometer.service, "TI SensorTag Barometer Service")
        appSpecName(Barometer.data, "TI SensorTag Barometer Data")
        appSpecName(Barometer.config, "TI SensorTag Barometer Config")
        appSpecName(Barometer.calibration, "TI SensorTag Barometer Calibration")
        appSpecName(Barometer.period, "TI SensorTag Barometer Period")
        appSpecName(Gyroscope.service, "TI SensorTag Gyroscope Service")
        appSpecName(Gyroscope.data, "TI SensorTag Gyroscope Data")
        appSpecName(Gyroscope.config, "TI SensorTag Gyroscope Config")
        appSpecName(Gyroscope.period, "TI SensorTag Gyroscope Period")
        appSpecName(Services.io, "TI SensorTag IO Service")
        appSpecName(IO_DATA, "TI SensorTag IO Data")
        appSpecName(IO_CONFIG, "TI SensorTag IO Config")
        appSpecName(Light.service, "TI SensorTag Light Sensor Service")
        appSpecName(Light.data, "TI SensorTag Light Sensor Data")
        appSpecName(Light.config, "TI SensorTag Light Sensor Config")
        appSpecName(Light.period, "TI SensorTag Light Sensor Period")
        appSpecName(Movement.service, "TI SensorTag Movement Service")
        appSpecName(Movement.data, "TI SensorTag Movement Data")
        appSpecName(Movement.config, "TI SensorTag Movement Config")
        appSpecName(Movement.period, "TI SensorTag Movement Period")
        appSpecName(Services.register2, "TI SensorTag Register Service")
        appSpecName(TWO_REGISTER_DATA, "TI SensorTag Register Data")
        appSpecName(TWO_REGISTER_ADDRESS, "TI SensorTag Register Address")
        appSpecName(TWO_REGISTER_DEVICE_ID, "TI SensorTag Register Device ID")
        appSpecName(Services.display, "TI SensorTag Display Service")
        appSpecName(TWO_DISPLAY_DATA, "TI SensorTag Display Data")
        appSpecName(TWO_DISPLAY_CONTROL, "TI SensorTag Display Control")
    }
}

/**
 * Session interface for TI SensorTag
 */
/*interface TiSensorTagSession : UUPeripheralSession
{
    fun readTemperature(completion: UUPeripheralSessionObjectErrorCallback<UByte>)
}*/

/**
 * Async extension function to read temperature
 */
/*suspend fun TiSensorTagSession.readTemperatureAsync(): Result<Byte> =
    suspendCancellableCoroutine { cont ->
        readTemperature { session, value, error ->
            if (error != null) cont.resumeWithException(error)
            else if (value != null) cont.resume(Result.success(value))
            else cont.resumeWithException(IllegalStateException("No data and no error"))
        }
    }*/

/**
 * CoreBluetooth-based session implementation
 */
class TiSensorTagSession(peripheral: UUPeripheral) : UUPeripheralSession(peripheral) //, TiSensorTagSession
{
    override fun finishSessionStart(completion: () -> Unit)
    {
        setupKeysService()
        {
            setupHumidityService()
            {
                setupTemperatureService()
                {
                    setupMovementService()
                    {
                        setupBarometerService()
                        {
                            completion()
                        }
                    }
                }
            }
        }
    }

    fun readTemperature(completion: UUPeripheralSessionObjectErrorCallback<UByte>)
    {
        readUByte(TiSensorTag.Temperature.data, completion)
    }

    private fun setupKeysService(completion: () -> Unit)
    {
        startListeningForDataChanges(
            TiSensorTag.Keys.data,
            this::handleKeysDataChanged,
            { session, error ->
                completion()
            }
        )
    }

    private fun setupGyroscopeService(completion: () -> Unit)
    {
        writeUByteConfiguration(
            TiSensorTag.Gyroscope.config,
            TiSensorTag.Gyroscope.period,
            TiSensorTag.Gyroscope.data,
            1.toUByte(),
            100.toUByte(),
            this::handleGyroscopeDataChanged,
            completion
        )
    }

    private fun setupAccelerometerService(completion: () -> Unit)
    {
        writeUByteConfiguration(
            TiSensorTag.Accelerometer.config,
            TiSensorTag.Accelerometer.period,
            TiSensorTag.Accelerometer.data,
            1.toUByte(),
            100.toUByte(),
            this::handleAccelerometerDataChanged,
            completion
        )
    }

    private fun setupTemperatureService(completion: () -> Unit)
    {
        writeUByte(
            1.toUByte(),
            TiSensorTag.Temperature.config,
            withResponse = true)
        { session, error ->
            startListeningForDataChanges(
                TiSensorTag.Temperature.data,
                this::handleTemperatureDataChanged,
                { s, e -> completion() }
            )
        }
    }

    private fun setupMovementService(completion: () -> Unit)
    {
        writeUShortConfiguration(
            TiSensorTag.Movement.config,
            TiSensorTag.Movement.period,
            TiSensorTag.Movement.data,
            0x00FF.toUShort(),
            100.toUByte(),
            this::handleMovementDataChanged,
            completion
        )
    }

    private fun setupBarometerService(completion: () -> Unit)
    {
        writeUByteConfiguration(
            TiSensorTag.Barometer.config,
            TiSensorTag.Barometer.period,
            TiSensorTag.Barometer.data,
            1.toUByte(),
            100.toUByte(),
            this::handleBarometerDataChanged,
            completion
        )
    }

    private fun setupHumidityService(completion: () -> Unit)
    {
        writeUByteConfiguration(
            TiSensorTag.Humidity.config,
            TiSensorTag.Humidity.period,
            TiSensorTag.Humidity.data,
            1.toUByte(),
            100.toUByte(),
            this::handleHumidityDataChanged,
            completion
        )
    }

    private fun setupMagnetometerService(completion: () -> Unit)
    {
        writeUByteConfiguration(
            TiSensorTag.Magnetometer.config,
            TiSensorTag.Magnetometer.period,
            TiSensorTag.Magnetometer.data,
            1.toUByte(),
            100.toUByte(),
            this::handleMagnetometerDataChanged,
            completion
        )
    }

    private fun writeUByteConfiguration(
        configCharacteristic: UUID,
        periodCharacteristic: UUID,
        dataCharacteristic: UUID,
        configValue: UByte,
        periodValue: UByte,
        dataChanged: (UUPeripheralSession, ByteArray?, UUError?) -> Unit,
        completion: () -> Unit)
    {
        writeUByte(configValue, configCharacteristic, withResponse = true)
        { s, e ->
            writeUByte(periodValue, periodCharacteristic, withResponse = true)
            { s2, e2 ->
                startListeningForDataChanges(
                    dataCharacteristic,
                    dataChanged,
                    { s3, e3 -> completion() }
                )
            }
        }
    }

    private fun writeUShortConfiguration(
        configCharacteristic: UUID,
        periodCharacteristic: UUID,
        dataCharacteristic: UUID,
        configValue: UShort,
        periodValue: UByte,
        dataChanged: (UUPeripheralSession, ByteArray?, UUError?) -> Unit,
        completion: () -> Unit)
    {
        writeUShort(configValue, configCharacteristic, ByteOrder.LITTLE_ENDIAN, withResponse = true)
        { s, e ->
            writeUByte(periodValue, periodCharacteristic, withResponse = true)
            { s2, e2 ->
                startListeningForDataChanges(
                    dataCharacteristic,
                    dataChanged,
                    { s3, e3 -> completion() }
                )
            }
        }
    }

    private fun handleKeysDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Keys data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleGyroscopeDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Gyroscope data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleAccelerometerDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Accelerometer data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleTemperatureDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Temperature data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleMovementDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Movement data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleBarometerDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Barometer data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleHumidityDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Humidity data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleLightDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Light data changed: ${data?.joinToString(",") ?: "null"}")
    }

    private fun handleMagnetometerDataChanged(session: UUPeripheralSession, data: ByteArray?, error: UUError?)
    {
        Log.d("TiSensorTag", "Magnetometer data changed: ${data?.joinToString(",") ?: "null"}")
    }
}
