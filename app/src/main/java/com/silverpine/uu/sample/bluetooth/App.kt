package com.silverpine.uu.sample.bluetooth

import android.app.Application
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import com.silverpine.uu.bluetooth.uuUuidFromString
import com.silverpine.uu.core.UUResources
import com.silverpine.uu.logging.UUConsoleLogWriter
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogLevel
import com.silverpine.uu.logging.UULogger
import com.silverpine.uu.sample.bluetooth.ui.Strings
import java.util.UUID

class App: Application()
{
    override fun onCreate()
    {
        super.onCreate()

        UUResources.init(applicationContext)
        UUBluetooth.init(applicationContext)

        val logger = UULogger(UUConsoleLogWriter())
        logger.logLevel = UULogLevel.VERBOSE
        UULog.setLogger(logger)
        Strings.init(applicationContext)
        TiSensorTagConstants.addSpecNames()
    }
}

val Any.LOG_TAG: String
    get()
    {
        return javaClass.name
    }


object TiSensorTagConstants
{
    val TI_SIMPLE_KEYS_SERVICE = uuShortCodeToUuid("FFE0")
    val TI_SIMPLE_KEYS_KEY_PRESS_STATE = uuShortCodeToUuid("FFE1")

    val TI_OAD_SERVICE = uuUuidFromString("F000FFC0-0451-4000-B000-000000000000")
    val TI_OAD_IMAGE_NOTIFY = uuUuidFromString("F000FFC1-0451-4000-B000-000000000000")
    val TI_OAD_IMAGE_BLOCK_REQUEST = uuUuidFromString("F000FFC2-0451-4000-B000-000000000000")
    val TI_CONNECTION_CONTROL_SERVICE = uuUuidFromString("F000CCC0-0451-4000-B000-000000000000")
    val TI_CONNECTION_CONTROL_CURRENT_USED_PARAMETERS = uuUuidFromString("F000CCC1-0451-4000-B000-000000000000")
    val TI_CONNECTION_CONTROL_REQUEST_NEW_PARAMETERS = uuUuidFromString("F000CCC2-0451-4000-B000-000000000000")
    val TI_CONNECTION_CONTROL_DISCONNECT_REQUEST = uuUuidFromString("F000CCC3-0451-4000-B000-000000000000")
    val TI_CONNECTION_CONTROL_NAP_INTERVAL_SETTING = uuUuidFromString("F000CCC4-0451-4000-B000-000000000000")

    val TEMPERATURE_SERVICE = uuUuidFromString("F000AA00-0451-4000-B000-000000000000")
    val IR_TEMPERATURE_DATA = uuUuidFromString("F000AA01-0451-4000-B000-000000000000")
    val IR_TEMPERATURE_CONFIG = uuUuidFromString("F000AA02-0451-4000-B000-000000000000")
    val IR_TEMPERATURE_PERIOD = uuUuidFromString("F000AA03-0451-4000-B000-000000000000")
    val ACCELEROMETER_SERVICE = uuUuidFromString("F000AA10-0451-4000-B000-000000000000")
    val ACCELEROMETER_DATA = uuUuidFromString("F000AA11-0451-4000-B000-000000000000")
    val ACCELEROMETER_CONFIG = uuUuidFromString("F000AA12-0451-4000-B000-000000000000")
    val ACCELEROMETER_PERIOD = uuUuidFromString("F000AA13-0451-4000-B000-000000000000")
    val HUMIDITY_SERVICE = uuUuidFromString("F000AA20-0451-4000-B000-000000000000")
    val HUMIDITY_DATA = uuUuidFromString("F000AA21-0451-4000-B000-000000000000")
    val HUMIDITY_CONFIG = uuUuidFromString("F000AA22-0451-4000-B000-000000000000")
    val HUMIDITY_PERIOD = uuUuidFromString("F000AA23-0451-4000-B000-000000000000")
    val MAGNETOMETER_SERVICE = uuUuidFromString("F000AA30-0451-4000-B000-000000000000")
    val MAGNETOMETER_DATA = uuUuidFromString("F000AA31-0451-4000-B000-000000000000")
    val MAGNETOMETER_CONFIG = uuUuidFromString("F000AA32-0451-4000-B000-000000000000")
    val MAGNETOMETER_PERIOD = uuUuidFromString("F000AA33-0451-4000-B000-000000000000")
    val BAROMETER_SERVICE = uuUuidFromString("F000AA40-0451-4000-B000-000000000000")
    val BAROMETER_DATA = uuUuidFromString("F000AA41-0451-4000-B000-000000000000")
    val BAROMETER_CONFIG = uuUuidFromString("F000AA42-0451-4000-B000-000000000000")
    val BAROMETER_CALIBRATION = uuUuidFromString("F000AA43-0451-4000-B000-000000000000")
    val BAROMETER_PERIOD = uuUuidFromString("F000AA44-0451-4000-B000-000000000000")
    val GYROSCOPE_SERVICE = uuUuidFromString("F000AA50-0451-4000-B000-000000000000")
    val GYROSCOPE_DATA = uuUuidFromString("F000AA51-0451-4000-B000-000000000000")
    val GYROSCOPE_CONFIG = uuUuidFromString("F000AA52-0451-4000-B000-000000000000")
    val GYROSCOPE_PERIOD = uuUuidFromString("F000AA53-0451-4000-B000-000000000000")
    val IO_SERVICE = uuUuidFromString("F000AA64-0451-4000-B000-000000000000")
    val IO_DATA = uuUuidFromString("F000AA65-0451-4000-B000-000000000000")
    val IO_CONFIG = uuUuidFromString("F000AA66-0451-4000-B000-000000000000")

    val TWO_LIGHT_SENSOR_SERVICE = uuUuidFromString("F000AA70-0451-4000-B000-000000000000")
    val TWO_LIGHT_SENSOR_DATA = uuUuidFromString("F000AA71-0451-4000-B000-000000000000")
    val TWO_LIGHT_SENSOR_CONFIG = uuUuidFromString("F000AA72-0451-4000-B000-000000000000")
    val TWO_LIGHT_SENSOR_PERIOD = uuUuidFromString("F000AA73-0451-4000-B000-000000000000")
    val TWO_MOVEMENT_SERVICE = uuUuidFromString("F000AA80-0451-4000-B000-000000000000")
    val TWO_MOVEMENT_DATA = uuUuidFromString("F000AA81-0451-4000-B000-000000000000")
    val TWO_MOVEMENT_CONFIG = uuUuidFromString("F000AA82-0451-4000-B000-000000000000")
    val TWO_MOVEMENT_PERIOD = uuUuidFromString("F000AA83-0451-4000-B000-000000000000")
    val TWO_REGISTER_SERVICE = uuUuidFromString("F000AC00-0451-4000-B000-000000000000")
    val TWO_REGISTER_DATA = uuUuidFromString("F000AC01-0451-4000-B000-000000000000")
    val TWO_REGISTER_ADDRESS = uuUuidFromString("F000AC02-0451-4000-B000-000000000000")
    val TWO_REGISTER_DEVICE_ID = uuUuidFromString("F000AC03-0451-4000-B000-000000000000")
    val TWO_DISPLAY_SERVICE = uuUuidFromString("F000AD00-0451-4000-B000-000000000000")
    val TWO_DISPLAY_DATA = uuUuidFromString("F000AD01-0451-4000-B000-000000000000")
    val TWO_DISPLAY_CONTROL = uuUuidFromString("F000AD02-0451-4000-B000-000000000000")

    private fun appSpecName(uuid: UUID, name: String)
    {
        UUBluetooth.addBluetoothSpecName(uuid, name)
    }

    fun addSpecNames()
    {
        appSpecName(TI_SIMPLE_KEYS_SERVICE, "TI SimpleLink Keys Service")
        appSpecName(TI_SIMPLE_KEYS_KEY_PRESS_STATE, "TI SimpleLink Keys Key Press State")

        appSpecName(TI_OAD_SERVICE, "TI OAD Service")
        appSpecName(TI_OAD_IMAGE_NOTIFY, "TI OAD Image Notify")
        appSpecName(TI_OAD_IMAGE_BLOCK_REQUEST, "TI OAD Image Block Request")
        appSpecName(TI_CONNECTION_CONTROL_SERVICE, "TI Connection Control Service")
        appSpecName(TI_CONNECTION_CONTROL_CURRENT_USED_PARAMETERS, "TI Connection Control Current Used Parameters")
        appSpecName(TI_CONNECTION_CONTROL_REQUEST_NEW_PARAMETERS, "TI Connection Control Request New Parameters")
        appSpecName(TI_CONNECTION_CONTROL_DISCONNECT_REQUEST, "TI Connection Control Disconnect Request")
        appSpecName(TI_CONNECTION_CONTROL_NAP_INTERVAL_SETTING, "TI Connection Control Nap Interval Setting")

        appSpecName(TEMPERATURE_SERVICE, "TI SensorTag Temperature Service")
        appSpecName(IR_TEMPERATURE_DATA, "TI SensorTag Temperature Data")
        appSpecName(IR_TEMPERATURE_CONFIG, "TI SensorTag Temperature Config")
        appSpecName(IR_TEMPERATURE_PERIOD, "TI SensorTag Temperature Period")
        appSpecName(ACCELEROMETER_SERVICE, "TI SensorTag Accelerometer Service")
        appSpecName(ACCELEROMETER_DATA, "TI SensorTag Accelerometer Data")
        appSpecName(ACCELEROMETER_CONFIG, "TI SensorTag Accelerometer Config")
        appSpecName(ACCELEROMETER_PERIOD, "TI SensorTag Accelerometer Period")
        appSpecName(HUMIDITY_SERVICE, "TI SensorTag Humidity Service")
        appSpecName(HUMIDITY_DATA, "TI SensorTag Humidity Data")
        appSpecName(HUMIDITY_CONFIG, "TI SensorTag Humidity Config")
        appSpecName(HUMIDITY_PERIOD, "TI SensorTag Humidity Period")
        appSpecName(MAGNETOMETER_SERVICE, "TI SensorTag Magnetometer Service")
        appSpecName(MAGNETOMETER_DATA, "TI SensorTag Magnetometer Data")
        appSpecName(MAGNETOMETER_CONFIG, "TI SensorTag Magnetometer Config")
        appSpecName(MAGNETOMETER_PERIOD, "TI SensorTag Magnetometer Period")
        appSpecName(BAROMETER_SERVICE, "TI SensorTag Barometer Service")
        appSpecName(BAROMETER_DATA, "TI SensorTag Barometer Data")
        appSpecName(BAROMETER_CONFIG, "TI SensorTag Barometer Config")
        appSpecName(BAROMETER_CALIBRATION, "TI SensorTag Barometer Calibration")
        appSpecName(BAROMETER_PERIOD, "TI SensorTag Barometer Period")
        appSpecName(GYROSCOPE_SERVICE, "TI SensorTag Gyroscope Service")
        appSpecName(GYROSCOPE_DATA, "TI SensorTag Gyroscope Data")
        appSpecName(GYROSCOPE_CONFIG, "TI SensorTag Gyroscope Config")
        appSpecName(GYROSCOPE_PERIOD, "TI SensorTag Gyroscope Period")
        appSpecName(IO_SERVICE, "TI SensorTag IO Service")
        appSpecName(IO_DATA, "TI SensorTag IO Data")
        appSpecName(IO_CONFIG, "TI SensorTag IO Config")
        appSpecName(TWO_LIGHT_SENSOR_SERVICE, "TI SensorTag Light Sensor Service")
        appSpecName(TWO_LIGHT_SENSOR_DATA, "TI SensorTag Light Sensor Data")
        appSpecName(TWO_LIGHT_SENSOR_CONFIG, "TI SensorTag Light Sensor Config")
        appSpecName(TWO_LIGHT_SENSOR_PERIOD, "TI SensorTag Light Sensor Period")
        appSpecName(TWO_MOVEMENT_SERVICE, "TI SensorTag Movement Service")
        appSpecName(TWO_MOVEMENT_DATA, "TI SensorTag Movement Data")
        appSpecName(TWO_MOVEMENT_CONFIG, "TI SensorTag Movement Config")
        appSpecName(TWO_MOVEMENT_PERIOD, "TI SensorTag Movement Period")
        appSpecName(TWO_REGISTER_SERVICE, "TI SensorTag Register Service")
        appSpecName(TWO_REGISTER_DATA, "TI SensorTag Register Data")
        appSpecName(TWO_REGISTER_ADDRESS, "TI SensorTag Register Address")
        appSpecName(TWO_REGISTER_DEVICE_ID, "TI SensorTag Register Device ID")
        appSpecName(TWO_DISPLAY_SERVICE, "TI SensorTag Display Service")
        appSpecName(TWO_DISPLAY_DATA, "TI SensorTag Display Data")
        appSpecName(TWO_DISPLAY_CONTROL, "TI SensorTag Display Control")
    }
}
