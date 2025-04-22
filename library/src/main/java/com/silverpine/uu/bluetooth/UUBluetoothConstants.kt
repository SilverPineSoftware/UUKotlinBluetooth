package com.silverpine.uu.bluetooth

import java.util.UUID

/**
 * Common bluetooth related constants
 */
object UUBluetoothConstants
{
    /**
     * String formatter for building full 128 UUID's from a 16 bit BTLE shortcode
     */
    const val BLUETOOTH_UUID_SHORTCODE_FORMAT = "0000%s-0000-1000-8000-00805F9B34FB"

    /**
     * This status code gets returned from BluetoothGattCallback.onConnectionStateChanged, but is not
     * defined anywhere.  The constant is defined here:
     *
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
     *
     * #define  GATT_ERROR                          0x85
     */
    const val GATT_ERROR = 0x85 // Status 133

    /**
     * When a peripheral terminates a connection, BluetoothGattCallback.onConnectionStateChanged is
     * called with status 19.  The constant is defined here:
     *
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h
     *
     * #define GATT_CONN_TERMINATE_PEER_USER       HCI_ERR_PEER_USER               0x13 connection terminate by peer user
     *
     * And HCI_ERR_PEER_USER is defined:
     *
     * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/hcidefs.h
     *
     * #define HCI_ERR_PEER_USER                               0x13
     */
    const val GATT_DISCONNECTED_BY_PERIPHERAL = 0x13

    /**
     * Per the Bluetooth spec, the default MTU size is 23 bytes
     */
    const val DEFAULT_MTU = 23

    val BLUETOOTH_SPEC_NAMES = HashMap<UUID?, String>()

    init
    {
        BLUETOOTH_SPEC_NAMES[Services.SERIAL_PORT_PROFILE_UUID] =
            "Serial Port Profile"
        BLUETOOTH_SPEC_NAMES[Services.ALERT_NOTIFICATION_SERVICE_UUID] =
            "Alert Notification Service"
        BLUETOOTH_SPEC_NAMES[Services.AUTOMATION_IO_UUID] =
            "Automation IO"
        BLUETOOTH_SPEC_NAMES[Services.BATTERY_SERVICE_UUID] =
            "Battery Service"
        BLUETOOTH_SPEC_NAMES[Services.BLOOD_PRESSURE_UUID] =
            "Blood Pressure"
        BLUETOOTH_SPEC_NAMES[Services.BODY_COMPOSITION_UUID] =
            "Body Composition"
        BLUETOOTH_SPEC_NAMES[Services.BOND_MANAGEMENT_UUID] =
            "Bond Management"
        BLUETOOTH_SPEC_NAMES[Services.CONTINUOUS_GLUCOSE_MONITORING_UUID] =
            "Continuous Glucose Monitoring"
        BLUETOOTH_SPEC_NAMES[Services.CURRENT_TIME_SERVICE_UUID] =
            "Current Time Service"
        BLUETOOTH_SPEC_NAMES[Services.CYCLING_POWER_UUID] =
            "Cycling Power"
        BLUETOOTH_SPEC_NAMES[Services.CYCLING_SPEED_AND_CADENCE_UUID] =
            "Cycling Speed and Cadence"
        BLUETOOTH_SPEC_NAMES[Services.DEVICE_INFORMATION_UUID] =
            "Device Information"
        BLUETOOTH_SPEC_NAMES[Services.ENVIRONMENTAL_SENSING_UUID] =
            "Environmental Sensing"
        BLUETOOTH_SPEC_NAMES[Services.GENERIC_ACCESS_UUID] =
            "Generic Access"
        BLUETOOTH_SPEC_NAMES[Services.GENERIC_ATTRIBUTE_UUID] =
            "Generic Attribute"
        BLUETOOTH_SPEC_NAMES[Services.GLUCOSE_UUID] =
            "Glucose"
        BLUETOOTH_SPEC_NAMES[Services.HEALTH_THERMOMETER_UUID] =
            "Health Thermometer"
        BLUETOOTH_SPEC_NAMES[Services.HEART_RATE_UUID] =
            "Heart Rate"
        BLUETOOTH_SPEC_NAMES[Services.HTTP_PROXY_UUID] =
            "HTTP Proxy"
        BLUETOOTH_SPEC_NAMES[Services.HUMAN_INTERFACE_DEVICE_UUID] =
            "Human Interface Device"
        BLUETOOTH_SPEC_NAMES[Services.IMMEDIATE_ALERT_UUID] =
            "Immediate Alert"
        BLUETOOTH_SPEC_NAMES[Services.INDOOR_POSITIONING_UUID] =
            "Indoor Positioning"
        BLUETOOTH_SPEC_NAMES[Services.INTERNET_PROTOCOL_SUPPORT_UUID] =
            "Internet Protocol Support"
        BLUETOOTH_SPEC_NAMES[Services.LINK_LOSS_UUID] =
            "Link Loss"
        BLUETOOTH_SPEC_NAMES[Services.LOCATION_AND_NAVIGATION_UUID] =
            "Location and Navigation"
        BLUETOOTH_SPEC_NAMES[Services.NEXT_DST_CHANGE_SERVICE_UUID] =
            "Next DST Change Service"
        BLUETOOTH_SPEC_NAMES[Services.OBJECT_TRANSFER_UUID] =
            "Object Transfer"
        BLUETOOTH_SPEC_NAMES[Services.PHONE_ALERT_STATUS_SERVICE_UUID] =
            "Phone Alert Status Service"
        BLUETOOTH_SPEC_NAMES[Services.PULSE_OXIMETER_UUID] =
            "Pulse Oximeter"
        BLUETOOTH_SPEC_NAMES[Services.REFERENCE_TIME_UPDATE_SERVICE_UUID] =
            "Reference Time Update Service"
        BLUETOOTH_SPEC_NAMES[Services.RUNNING_SPEED_AND_CADENCE_UUID] =
            "Running Speed and Cadence"
        BLUETOOTH_SPEC_NAMES[Services.SCAN_PARAMETERS_UUID] =
            "Scan Parameters"
        BLUETOOTH_SPEC_NAMES[Services.TRANSPORT_DISCOVERY_UUID] =
            "Transport Discovery"
        BLUETOOTH_SPEC_NAMES[Services.TX_POWER_UUID] =
            "Tx Power"
        BLUETOOTH_SPEC_NAMES[Services.USER_DATA_UUID] =
            "User Data"
        BLUETOOTH_SPEC_NAMES[Services.WEIGHT_SCALE_UUID] =
            "Weight Scale"
        BLUETOOTH_SPEC_NAMES[Characteristics.AEROBIC_HEART_RATE_LOWER_LIMIT_UUID] =
            "Aerobic Heart Rate Lower Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.AEROBIC_HEART_RATE_UPPER_LIMIT_UUID] =
            "Aerobic Heart Rate Upper Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.AEROBIC_THRESHOLD_UUID] =
            "Aerobic Threshold"
        BLUETOOTH_SPEC_NAMES[Characteristics.AGE_UUID] =
            "Age"
        BLUETOOTH_SPEC_NAMES[Characteristics.AGGREGATE_UUID] =
            "Aggregate"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALERT_CATEGORY_ID_UUID] =
            "Alert Category ID"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALERT_CATEGORY_ID_BIT_MASK_UUID] =
            "Alert Category ID Bit Mask"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALERT_LEVEL_UUID] =
            "Alert Level"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALERT_NOTIFICATION_CONTROL_POINT_UUID] =
            "Alert Notification Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALERT_STATUS_UUID] =
            "Alert Status"
        BLUETOOTH_SPEC_NAMES[Characteristics.ALTITUDE_UUID] =
            "Altitude"
        BLUETOOTH_SPEC_NAMES[Characteristics.ANAEROBIC_HEART_RATE_LOWER_LIMIT_UUID] =
            "Anaerobic Heart Rate Lower Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.ANAEROBIC_HEART_RATE_UPPER_LIMIT_UUID] =
            "Anaerobic Heart Rate Upper Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.ANAEROBIC_THRESHOLD_UUID] =
            "Anaerobic Threshold"
        BLUETOOTH_SPEC_NAMES[Characteristics.ANALOG_UUID] =
            "Analog"
        BLUETOOTH_SPEC_NAMES[Characteristics.APPARENT_WIND_DIRECTION_UUID] =
            "Apparent Wind Direction"
        BLUETOOTH_SPEC_NAMES[Characteristics.APPARENT_WIND_SPEED_UUID] =
            "Apparent Wind Speed"
        BLUETOOTH_SPEC_NAMES[Characteristics.APPEARANCE_UUID] =
            "Appearance"
        BLUETOOTH_SPEC_NAMES[Characteristics.BAROMETRIC_PRESSURE_TREND_UUID] =
            "Barometric Pressure Trend"
        BLUETOOTH_SPEC_NAMES[Characteristics.BATTERY_LEVEL_UUID] =
            "Battery Level"
        BLUETOOTH_SPEC_NAMES[Characteristics.BLOOD_PRESSURE_FEATURE_UUID] =
            "Blood Pressure Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.BLOOD_PRESSURE_MEASUREMENT_UUID] =
            "Blood Pressure Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.BODY_COMPOSITION_FEATURE_UUID] =
            "Body Composition Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.BODY_COMPOSITION_MEASUREMENT_UUID] =
            "Body Composition Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.BODY_SENSOR_LOCATION_UUID] =
            "Body Sensor Location"
        BLUETOOTH_SPEC_NAMES[Characteristics.BOND_MANAGEMENT_CONTROL_POINT_UUID] =
            "Bond Management Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.BOND_MANAGEMENT_FEATURE_UUID] =
            "Bond Management Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.BOOT_KEYBOARD_INPUT_REPORT_UUID] =
            "Boot Keyboard Input Report"
        BLUETOOTH_SPEC_NAMES[Characteristics.BOOT_KEYBOARD_OUTPUT_REPORT_UUID] =
            "Boot Keyboard Output Report"
        BLUETOOTH_SPEC_NAMES[Characteristics.BOOT_MOUSE_INPUT_REPORT_UUID] =
            "Boot Mouse Input Report"
        BLUETOOTH_SPEC_NAMES[Characteristics.CENTRAL_ADDRESS_RESOLUTION_UUID] =
            "Central Address Resolution"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_FEATURE_UUID] =
            "CGM Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_MEASUREMENT_UUID] =
            "CGM Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_SESSION_RUN_TIME_UUID] =
            "CGM Session Run Time"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_SESSION_START_TIME_UUID] =
            "CGM Session Start Time"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_SPECIFIC_OPS_CONTROL_POINT_UUID] =
            "CGM Specific Ops Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.CGM_STATUS_UUID] =
            "CGM Status"
        BLUETOOTH_SPEC_NAMES[Characteristics.CSC_FEATURE_UUID] =
            "CSC Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.CSC_MEASUREMENT_UUID] =
            "CSC Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.CURRENT_TIME_UUID] =
            "Current Time"
        BLUETOOTH_SPEC_NAMES[Characteristics.CYCLING_POWER_CONTROL_POINT_UUID] =
            "Cycling Power Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.CYCLING_POWER_FEATURE_UUID] =
            "Cycling Power Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.CYCLING_POWER_MEASUREMENT_UUID] =
            "Cycling Power Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.CYCLING_POWER_VECTOR_UUID] =
            "Cycling Power Vector"
        BLUETOOTH_SPEC_NAMES[Characteristics.DATABASE_CHANGE_INCREMENT_UUID] =
            "Database Change Increment"
        BLUETOOTH_SPEC_NAMES[Characteristics.DATE_OF_BIRTH_UUID] =
            "Date of Birth"
        BLUETOOTH_SPEC_NAMES[Characteristics.DATE_OF_THRESHOLD_ASSESSMENT_UUID] =
            "Date of Threshold Assessment"
        BLUETOOTH_SPEC_NAMES[Characteristics.DATE_TIME_UUID] =
            "Date Time"
        BLUETOOTH_SPEC_NAMES[Characteristics.DAY_DATE_TIME_UUID] =
            "Day Date Time"
        BLUETOOTH_SPEC_NAMES[Characteristics.DAY_OF_WEEK_UUID] =
            "Day of Week"
        BLUETOOTH_SPEC_NAMES[Characteristics.DESCRIPTOR_VALUE_CHANGED_UUID] =
            "Descriptor Value Changed"
        BLUETOOTH_SPEC_NAMES[Characteristics.DEVICE_NAME_UUID] =
            "Device Name"
        BLUETOOTH_SPEC_NAMES[Characteristics.DEW_POINT_UUID] =
            "Dew Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.DIGITAL_UUID] =
            "Digital"
        BLUETOOTH_SPEC_NAMES[Characteristics.DST_OFFSET_UUID] =
            "DST Offset"
        BLUETOOTH_SPEC_NAMES[Characteristics.ELEVATION_UUID] =
            "Elevation"
        BLUETOOTH_SPEC_NAMES[Characteristics.EMAIL_ADDRESS_UUID] =
            "Email Address"
        BLUETOOTH_SPEC_NAMES[Characteristics.EXACT_TIME_256_UUID] =
            "Exact Time 256"
        BLUETOOTH_SPEC_NAMES[Characteristics.FAT_BURN_HEART_RATE_LOWER_LIMIT_UUID] =
            "Fat Burn Heart Rate Lower Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.FAT_BURN_HEART_RATE_UPPER_LIMIT_UUID] =
            "Fat Burn Heart Rate Upper Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.FIRMWARE_REVISION_STRING_UUID] =
            "Firmware Revision String"
        BLUETOOTH_SPEC_NAMES[Characteristics.FIRST_NAME_UUID] =
            "First Name"
        BLUETOOTH_SPEC_NAMES[Characteristics.FIVE_ZONE_HEART_RATE_LIMITS_UUID] =
            "Five Zone Heart Rate Limits"
        BLUETOOTH_SPEC_NAMES[Characteristics.FLOOR_NUMBER_UUID] =
            "Floor Number"
        BLUETOOTH_SPEC_NAMES[Characteristics.GENDER_UUID] =
            "Gender"
        BLUETOOTH_SPEC_NAMES[Characteristics.GLUCOSE_FEATURE_UUID] =
            "Glucose Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.GLUCOSE_MEASUREMENT_UUID] =
            "Glucose Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.GLUCOSE_MEASUREMENT_CONTEXT_UUID] =
            "Glucose Measurement Context"
        BLUETOOTH_SPEC_NAMES[Characteristics.GUST_FACTOR_UUID] =
            "Gust Factor"
        BLUETOOTH_SPEC_NAMES[Characteristics.HARDWARE_REVISION_STRING_UUID] =
            "Hardware Revision String"
        BLUETOOTH_SPEC_NAMES[Characteristics.HEART_RATE_CONTROL_POINT_UUID] =
            "Heart Rate Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.HEART_RATE_MAX_UUID] =
            "Heart Rate Max"
        BLUETOOTH_SPEC_NAMES[Characteristics.HEART_RATE_MEASUREMENT_UUID] =
            "Heart Rate Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.HEAT_INDEX_UUID] =
            "Heat Index"
        BLUETOOTH_SPEC_NAMES[Characteristics.HEIGHT_UUID] =
            "Height"
        BLUETOOTH_SPEC_NAMES[Characteristics.HID_CONTROL_POINT_UUID] =
            "HID Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.HID_INFORMATION_UUID] =
            "HID Information"
        BLUETOOTH_SPEC_NAMES[Characteristics.HIP_CIRCUMFERENCE_UUID] =
            "Hip Circumference"
        BLUETOOTH_SPEC_NAMES[Characteristics.HTTP_CONTROL_POINT_UUID] =
            "HTTP Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.HTTP_ENTITY_BODY_UUID] =
            "HTTP Entity Body"
        BLUETOOTH_SPEC_NAMES[Characteristics.HTTP_HEADERS_UUID] =
            "HTTP Headers"
        BLUETOOTH_SPEC_NAMES[Characteristics.HTTP_STATUS_CODE_UUID] =
            "HTTP Status Code"
        BLUETOOTH_SPEC_NAMES[Characteristics.HTTPS_SECURITY_UUID] =
            "HTTPS Security"
        BLUETOOTH_SPEC_NAMES[Characteristics.HUMIDITY_UUID] =
            "Humidity"
        BLUETOOTH_SPEC_NAMES[Characteristics.IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST_UUID] =
            "IEEE 11073-20601 Regulatory Certification Data List"
        BLUETOOTH_SPEC_NAMES[Characteristics.INDOOR_POSITIONING_CONFIGURATION_UUID] =
            "Indoor Positioning Configuration"
        BLUETOOTH_SPEC_NAMES[Characteristics.INTERMEDIATE_CUFF_PRESSURE_UUID] =
            "Intermediate Cuff Pressure"
        BLUETOOTH_SPEC_NAMES[Characteristics.INTERMEDIATE_TEMPERATURE_UUID] =
            "Intermediate Temperature"
        BLUETOOTH_SPEC_NAMES[Characteristics.IRRADIANCE_UUID] =
            "Irradiance"
        BLUETOOTH_SPEC_NAMES[Characteristics.LANGUAGE_UUID] =
            "Language"
        BLUETOOTH_SPEC_NAMES[Characteristics.LAST_NAME_UUID] =
            "Last Name"
        BLUETOOTH_SPEC_NAMES[Characteristics.LATITUDE_UUID] =
            "Latitude"
        BLUETOOTH_SPEC_NAMES[Characteristics.LN_CONTROL_POINT_UUID] =
            "LN Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.LN_FEATURE_UUID] =
            "LN Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.LOCAL_EAST_COORDINATE_UUID] =
            "Local East Coordinate"
        BLUETOOTH_SPEC_NAMES[Characteristics.LOCAL_NORTH_COORDINATE_UUID] =
            "Local North Coordinate"
        BLUETOOTH_SPEC_NAMES[Characteristics.LOCAL_TIME_INFORMATION_UUID] =
            "Local Time Information"
        BLUETOOTH_SPEC_NAMES[Characteristics.LOCATION_AND_SPEED_UUID] =
            "Location and Speed"
        BLUETOOTH_SPEC_NAMES[Characteristics.LOCATION_NAME_UUID] =
            "Location Name"
        BLUETOOTH_SPEC_NAMES[Characteristics.LONGITUDE_UUID] =
            "Longitude"
        BLUETOOTH_SPEC_NAMES[Characteristics.MAGNETIC_DECLINATION_UUID] =
            "Magnetic Declination"
        BLUETOOTH_SPEC_NAMES[Characteristics.MAGNETIC_FLUX_DENSITY_2D_UUID] =
            "Magnetic Flux Density - 2D"
        BLUETOOTH_SPEC_NAMES[Characteristics.MAGNETIC_FLUX_DENSITY_3D_UUID] =
            "Magnetic Flux Density - 3D"
        BLUETOOTH_SPEC_NAMES[Characteristics.MANUFACTURER_NAME_STRING_UUID] =
            "Manufacturer Name String"
        BLUETOOTH_SPEC_NAMES[Characteristics.MAXIMUM_RECOMMENDED_HEART_RATE_UUID] =
            "Maximum Recommended Heart Rate"
        BLUETOOTH_SPEC_NAMES[Characteristics.MEASUREMENT_INTERVAL_UUID] =
            "Measurement Interval"
        BLUETOOTH_SPEC_NAMES[Characteristics.MODEL_NUMBER_STRING_UUID] =
            "Model Number String"
        BLUETOOTH_SPEC_NAMES[Characteristics.NAVIGATION_UUID] =
            "Navigation"
        BLUETOOTH_SPEC_NAMES[Characteristics.NEW_ALERT_UUID] =
            "New Alert"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_ACTION_CONTROL_POINT_UUID] =
            "Object Action Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_CHANGED_UUID] =
            "Object Changed"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_FIRST_CREATED_UUID] =
            "Object First-Created"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_ID_UUID] =
            "Object ID"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_LAST_MODIFIED_UUID] =
            "Object Last-Modified"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_LIST_CONTROL_POINT_UUID] =
            "Object List Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_LIST_FILTER_UUID] =
            "Object List Filter"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_NAME_UUID] =
            "Object Name"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_PROPERTIES_UUID] =
            "Object Properties"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_SIZE_UUID] =
            "Object Size"
        BLUETOOTH_SPEC_NAMES[Characteristics.OBJECT_TYPE_UUID] =
            "Object Type"
        BLUETOOTH_SPEC_NAMES[Characteristics.OTS_FEATURE_UUID] =
            "OTS Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS_UUID] =
            "Peripheral Preferred Connection Parameters"
        BLUETOOTH_SPEC_NAMES[Characteristics.PERIPHERAL_PRIVACY_FLAG_UUID] =
            "Peripheral Privacy Flag"
        BLUETOOTH_SPEC_NAMES[Characteristics.PLX_CONTINUOUS_MEASUREMENT_UUID] =
            "PLX Continuous Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.PLX_FEATURES_UUID] =
            "PLX Features"
        BLUETOOTH_SPEC_NAMES[Characteristics.PLX_SPOT_CHECK_MEASUREMENT_UUID] =
            "PLX Spot-Check Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.PNP_ID_UUID] =
            "PnP ID"
        BLUETOOTH_SPEC_NAMES[Characteristics.POLLEN_CONCENTRATION_UUID] =
            "Pollen Concentration"
        BLUETOOTH_SPEC_NAMES[Characteristics.POSITION_QUALITY_UUID] =
            "Position Quality"
        BLUETOOTH_SPEC_NAMES[Characteristics.PRESSURE_UUID] =
            "Pressure"
        BLUETOOTH_SPEC_NAMES[Characteristics.PROTOCOL_MODE_UUID] =
            "Protocol Mode"
        BLUETOOTH_SPEC_NAMES[Characteristics.RAINFALL_UUID] =
            "Rainfall"
        BLUETOOTH_SPEC_NAMES[Characteristics.RECONNECTION_ADDRESS_UUID] =
            "Reconnection Address"
        BLUETOOTH_SPEC_NAMES[Characteristics.RECORD_ACCESS_CONTROL_POINT_UUID] =
            "Record Access Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.REFERENCE_TIME_INFORMATION_UUID] =
            "Reference Time Information"
        BLUETOOTH_SPEC_NAMES[Characteristics.REPORT_UUID] =
            "Report"
        BLUETOOTH_SPEC_NAMES[Characteristics.REPORT_MAP_UUID] =
            "Report Map"
        BLUETOOTH_SPEC_NAMES[Characteristics.RESOLVABLE_PRIVATE_ADDRESS_ONLY_UUID] =
            "Resolvable Private Address Only"
        BLUETOOTH_SPEC_NAMES[Characteristics.RESTING_HEART_RATE_UUID] =
            "Resting Heart Rate"
        BLUETOOTH_SPEC_NAMES[Characteristics.RINGER_CONTROL_POINT_UUID] =
            "Ringer Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.RINGER_SETTING_UUID] =
            "Ringer Setting"
        BLUETOOTH_SPEC_NAMES[Characteristics.RSC_FEATURE_UUID] =
            "RSC Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.RSC_MEASUREMENT_UUID] =
            "RSC Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.SC_CONTROL_POINT_UUID] =
            "SC Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.SCAN_INTERVAL_WINDOW_UUID] =
            "Scan Interval Window"
        BLUETOOTH_SPEC_NAMES[Characteristics.SCAN_REFRESH_UUID] =
            "Scan Refresh"
        BLUETOOTH_SPEC_NAMES[Characteristics.SENSOR_LOCATION_UUID] =
            "Sensor Location"
        BLUETOOTH_SPEC_NAMES[Characteristics.SERIAL_NUMBER_STRING_UUID] =
            "Serial Number String"
        BLUETOOTH_SPEC_NAMES[Characteristics.SERVICE_CHANGED_UUID] =
            "Service Changed"
        BLUETOOTH_SPEC_NAMES[Characteristics.SOFTWARE_REVISION_STRING_UUID] =
            "Software Revision String"
        BLUETOOTH_SPEC_NAMES[Characteristics.SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS_UUID] =
            "Sport Type for Aerobic and Anaerobic Thresholds"
        BLUETOOTH_SPEC_NAMES[Characteristics.SUPPORTED_NEW_ALERT_CATEGORY_UUID] =
            "Supported New Alert Category"
        BLUETOOTH_SPEC_NAMES[Characteristics.SUPPORTED_UNREAD_ALERT_CATEGORY_UUID] =
            "Supported Unread Alert Category"
        BLUETOOTH_SPEC_NAMES[Characteristics.SYSTEM_ID_UUID] =
            "System ID"
        BLUETOOTH_SPEC_NAMES[Characteristics.TDS_CONTROL_POINT_UUID] =
            "TDS Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.TEMPERATURE_UUID] =
            "Temperature"
        BLUETOOTH_SPEC_NAMES[Characteristics.TEMPERATURE_MEASUREMENT_UUID] =
            "Temperature Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.TEMPERATURE_TYPE_UUID] =
            "Temperature Type"
        BLUETOOTH_SPEC_NAMES[Characteristics.THREE_ZONE_HEART_RATE_LIMITS_UUID] =
            "Three Zone Heart Rate Limits"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_ACCURACY_UUID] =
            "Time Accuracy"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_SOURCE_UUID] =
            "Time Source"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_UPDATE_CONTROL_POINT_UUID] =
            "Time Update Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_UPDATE_STATE_UUID] =
            "Time Update State"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_WITH_DST_UUID] =
            "Time with DST"
        BLUETOOTH_SPEC_NAMES[Characteristics.TIME_ZONE_UUID] =
            "Time Zone"
        BLUETOOTH_SPEC_NAMES[Characteristics.TRUE_WIND_DIRECTION_UUID] =
            "True Wind Direction"
        BLUETOOTH_SPEC_NAMES[Characteristics.TRUE_WIND_SPEED_UUID] =
            "True Wind Speed"
        BLUETOOTH_SPEC_NAMES[Characteristics.TWO_ZONE_HEART_RATE_LIMIT_UUID] =
            "Two Zone Heart Rate Limit"
        BLUETOOTH_SPEC_NAMES[Characteristics.TX_POWER_LEVEL_UUID] =
            "Tx Power Level"
        BLUETOOTH_SPEC_NAMES[Characteristics.UNCERTAINTY_UUID] =
            "Uncertainty"
        BLUETOOTH_SPEC_NAMES[Characteristics.UNREAD_ALERT_STATUS_UUID] =
            "Unread Alert Status"
        BLUETOOTH_SPEC_NAMES[Characteristics.URI_UUID] =
            "URI"
        BLUETOOTH_SPEC_NAMES[Characteristics.USER_CONTROL_POINT_UUID] =
            "User Control Point"
        BLUETOOTH_SPEC_NAMES[Characteristics.USER_INDEX_UUID] =
            "User Index"
        BLUETOOTH_SPEC_NAMES[Characteristics.UV_INDEX_UUID] =
            "UV Index"
        BLUETOOTH_SPEC_NAMES[Characteristics.VO2_MAX_UUID] =
            "VO2 Max"
        BLUETOOTH_SPEC_NAMES[Characteristics.WAIST_CIRCUMFERENCE_UUID] =
            "Waist Circumference"
        BLUETOOTH_SPEC_NAMES[Characteristics.WEIGHT_UUID] =
            "Weight"
        BLUETOOTH_SPEC_NAMES[Characteristics.WEIGHT_MEASUREMENT_UUID] =
            "Weight Measurement"
        BLUETOOTH_SPEC_NAMES[Characteristics.WEIGHT_SCALE_FEATURE_UUID] =
            "Weight Scale Feature"
        BLUETOOTH_SPEC_NAMES[Characteristics.WIND_CHILL_UUID] =
            "Wind Chill"
        BLUETOOTH_SPEC_NAMES[Descriptors.CHARACTERISTIC_AGGREGATE_FORMAT_UUID] =
            "Characteristic Aggregate Format"
        BLUETOOTH_SPEC_NAMES[Descriptors.CHARACTERISTIC_EXTENDED_PROPERTIES_UUID] =
            "Characteristic Extended Properties"
        BLUETOOTH_SPEC_NAMES[Descriptors.CHARACTERISTIC_PRESENTATION_FORMAT_UUID] =
            "Characteristic Presentation Format"
        BLUETOOTH_SPEC_NAMES[Descriptors.CHARACTERISTIC_USER_DESCRIPTION_UUID] =
            "Characteristic User Description"
        BLUETOOTH_SPEC_NAMES[Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID] =
            "Client Characteristic Configuration"
        BLUETOOTH_SPEC_NAMES[Descriptors.ENVIRONMENTAL_SENSING_CONFIGURATION_UUID] =
            "Environmental Sensing Configuration"
        BLUETOOTH_SPEC_NAMES[Descriptors.ENVIRONMENTAL_SENSING_MEASUREMENT_UUID] =
            "Environmental Sensing Measurement"
        BLUETOOTH_SPEC_NAMES[Descriptors.ENVIRONMENTAL_SENSING_TRIGGER_SETTING_UUID] =
            "Environmental Sensing Trigger Setting"
        BLUETOOTH_SPEC_NAMES[Descriptors.EXTERNAL_REPORT_REFERENCE_UUID] =
            "External Report Reference"
        BLUETOOTH_SPEC_NAMES[Descriptors.NUMBER_OF_DIGITALS_UUID] = "Number of Digitals"
        BLUETOOTH_SPEC_NAMES[Descriptors.REPORT_REFERENCE_UUID] = "Report Reference"
        BLUETOOTH_SPEC_NAMES[Descriptors.SERVER_CHARACTERISTIC_CONFIGURATION_UUID] =
            "Server Characteristic Configuration"
        BLUETOOTH_SPEC_NAMES[Descriptors.TIME_TRIGGER_SETTING_UUID] = "Time Trigger Setting"
        BLUETOOTH_SPEC_NAMES[Descriptors.VALID_RANGE_UUID] = "Valid Range"
        BLUETOOTH_SPEC_NAMES[Descriptors.VALUE_TRIGGER_SETTING_UUID] = "Value Trigger Setting"
    }

    object Services
    {
        val SERIAL_PORT_PROFILE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /**
         * SpecificationName: Alert Notification Service
         * SpecificationType: org.bluetooth.service.alert_notification
         * AssignedNumber: 0x1811
         */
        val ALERT_NOTIFICATION_SERVICE_UUID = uuShortCodeToUuid("1811")

        /**
         * SpecificationName: Automation IO
         * SpecificationType: org.bluetooth.service.automation_io
         * AssignedNumber: 0x1815
         */
        val AUTOMATION_IO_UUID = uuShortCodeToUuid("1815")

        /**
         * SpecificationName: Battery Service
         * SpecificationType: org.bluetooth.service.battery_service
         * AssignedNumber: 0x180F
         */
        val BATTERY_SERVICE_UUID = uuShortCodeToUuid("180F")

        /**
         * SpecificationName: Blood Pressure
         * SpecificationType: org.bluetooth.service.blood_pressure
         * AssignedNumber: 0x1810
         */
        val BLOOD_PRESSURE_UUID = uuShortCodeToUuid("1810")

        /**
         * SpecificationName: Body Composition
         * SpecificationType: org.bluetooth.service.body_composition
         * AssignedNumber: 0x181B
         */
        val BODY_COMPOSITION_UUID = uuShortCodeToUuid("181B")

        /**
         * SpecificationName: Bond Management
         * SpecificationType: org.bluetooth.service.bond_management
         * AssignedNumber: 0x181E
         */
        val BOND_MANAGEMENT_UUID = uuShortCodeToUuid("181E")

        /**
         * SpecificationName: Continuous Glucose Monitoring
         * SpecificationType: org.bluetooth.service.continuous_glucose_monitoring
         * AssignedNumber: 0x181F
         */
        val CONTINUOUS_GLUCOSE_MONITORING_UUID = uuShortCodeToUuid("181F")

        /**
         * SpecificationName: Current Time Service
         * SpecificationType: org.bluetooth.service.current_time
         * AssignedNumber: 0x1805
         */
        val CURRENT_TIME_SERVICE_UUID = uuShortCodeToUuid("1805")

        /**
         * SpecificationName: Cycling Power
         * SpecificationType: org.bluetooth.service.cycling_power
         * AssignedNumber: 0x1818
         */
        val CYCLING_POWER_UUID = uuShortCodeToUuid("1818")

        /**
         * SpecificationName: Cycling Speed and Cadence
         * SpecificationType: org.bluetooth.service.cycling_speed_and_cadence
         * AssignedNumber: 0x1816
         */
        val CYCLING_SPEED_AND_CADENCE_UUID = uuShortCodeToUuid("1816")

        /**
         * SpecificationName: Device Information
         * SpecificationType: org.bluetooth.service.device_information
         * AssignedNumber: 0x180A
         */
        val DEVICE_INFORMATION_UUID = uuShortCodeToUuid("180A")

        /**
         * SpecificationName: Environmental Sensing
         * SpecificationType: org.bluetooth.service.environmental_sensing
         * AssignedNumber: 0x181A
         */
        val ENVIRONMENTAL_SENSING_UUID = uuShortCodeToUuid("181A")

        /**
         * SpecificationName: Generic Access
         * SpecificationType: org.bluetooth.service.generic_access
         * AssignedNumber: 0x1800
         */
        val GENERIC_ACCESS_UUID = uuShortCodeToUuid("1800")

        /**
         * SpecificationName: Generic Attribute
         * SpecificationType: org.bluetooth.service.generic_attribute
         * AssignedNumber: 0x1801
         */
        val GENERIC_ATTRIBUTE_UUID = uuShortCodeToUuid("1801")

        /**
         * SpecificationName: Glucose
         * SpecificationType: org.bluetooth.service.glucose
         * AssignedNumber: 0x1808
         */
        val GLUCOSE_UUID = uuShortCodeToUuid("1808")

        /**
         * SpecificationName: Health Thermometer
         * SpecificationType: org.bluetooth.service.health_thermometer
         * AssignedNumber: 0x1809
         */
        val HEALTH_THERMOMETER_UUID = uuShortCodeToUuid("1809")

        /**
         * SpecificationName: Heart Rate
         * SpecificationType: org.bluetooth.service.heart_rate
         * AssignedNumber: 0x180D
         */
        val HEART_RATE_UUID = uuShortCodeToUuid("180D")

        /**
         * SpecificationName: HTTP Proxy
         * SpecificationType: org.bluetooth.service.http_proxy
         * AssignedNumber: 0x1823
         */
        val HTTP_PROXY_UUID = uuShortCodeToUuid("1823")

        /**
         * SpecificationName: Human Interface Device
         * SpecificationType: org.bluetooth.service.human_interface_device
         * AssignedNumber: 0x1812
         */
        val HUMAN_INTERFACE_DEVICE_UUID = uuShortCodeToUuid("1812")

        /**
         * SpecificationName: Immediate Alert
         * SpecificationType: org.bluetooth.service.immediate_alert
         * AssignedNumber: 0x1802
         */
        val IMMEDIATE_ALERT_UUID = uuShortCodeToUuid("1802")

        /**
         * SpecificationName: Indoor Positioning
         * SpecificationType: org.bluetooth.service.indoor_positioning
         * AssignedNumber: 0x1821
         */
        val INDOOR_POSITIONING_UUID = uuShortCodeToUuid("1821")

        /**
         * SpecificationName: Internet Protocol Support
         * SpecificationType: org.bluetooth.service.internet_protocol_support
         * AssignedNumber: 0x1820
         */
        val INTERNET_PROTOCOL_SUPPORT_UUID = uuShortCodeToUuid("1820")

        /**
         * SpecificationName: Link Loss
         * SpecificationType: org.bluetooth.service.link_loss
         * AssignedNumber: 0x1803
         */
        val LINK_LOSS_UUID = uuShortCodeToUuid("1803")

        /**
         * SpecificationName: Location and Navigation
         * SpecificationType: org.bluetooth.service.location_and_navigation
         * AssignedNumber: 0x1819
         */
        val LOCATION_AND_NAVIGATION_UUID = uuShortCodeToUuid("1819")

        /**
         * SpecificationName: Next DST Change Service
         * SpecificationType: org.bluetooth.service.next_dst_change
         * AssignedNumber: 0x1807
         */
        val NEXT_DST_CHANGE_SERVICE_UUID = uuShortCodeToUuid("1807")

        /**
         * SpecificationName: Object Transfer
         * SpecificationType: org.bluetooth.service.object_transfer
         * AssignedNumber: 0x1825
         */
        val OBJECT_TRANSFER_UUID = uuShortCodeToUuid("1825")

        /**
         * SpecificationName: Phone Alert Status Service
         * SpecificationType: org.bluetooth.service.phone_alert_status
         * AssignedNumber: 0x180E
         */
        val PHONE_ALERT_STATUS_SERVICE_UUID = uuShortCodeToUuid("180E")

        /**
         * SpecificationName: Pulse Oximeter
         * SpecificationType: org.bluetooth.service.pulse_oximeter
         * AssignedNumber: 0x1822
         */
        val PULSE_OXIMETER_UUID = uuShortCodeToUuid("1822")

        /**
         * SpecificationName: Reference Time Update Service
         * SpecificationType: org.bluetooth.service.reference_time_update
         * AssignedNumber: 0x1806
         */
        val REFERENCE_TIME_UPDATE_SERVICE_UUID = uuShortCodeToUuid("1806")

        /**
         * SpecificationName: Running Speed and Cadence
         * SpecificationType: org.bluetooth.service.running_speed_and_cadence
         * AssignedNumber: 0x1814
         */
        val RUNNING_SPEED_AND_CADENCE_UUID = uuShortCodeToUuid("1814")

        /**
         * SpecificationName: Scan Parameters
         * SpecificationType: org.bluetooth.service.scan_parameters
         * AssignedNumber: 0x1813
         */
        val SCAN_PARAMETERS_UUID = uuShortCodeToUuid("1813")

        /**
         * SpecificationName: Transport Discovery
         * SpecificationType: org.bluetooth.service.transport_discovery
         * AssignedNumber: 0x1824
         */
        val TRANSPORT_DISCOVERY_UUID = uuShortCodeToUuid("1824")

        /**
         * SpecificationName: Tx Power
         * SpecificationType: org.bluetooth.service.tx_power
         * AssignedNumber: 0x1804
         */
        val TX_POWER_UUID = uuShortCodeToUuid("1804")

        /**
         * SpecificationName: User Data
         * SpecificationType: org.bluetooth.service.user_data
         * AssignedNumber: 0x181C
         */
        val USER_DATA_UUID = uuShortCodeToUuid("181C")

        /**
         * SpecificationName: Weight Scale
         * SpecificationType: org.bluetooth.service.weight_scale
         * AssignedNumber: 0x181D
         */
        val WEIGHT_SCALE_UUID = uuShortCodeToUuid("181D")
    }

    object Characteristics
    {
        /**
         * SpecificationName: Aerobic Heart Rate Lower Limit
         * SpecificationType: org.bluetooth.characteristic.aerobic_heart_rate_lower_limit
         * AssignedNumber: 0x2A7E
         */
        val AEROBIC_HEART_RATE_LOWER_LIMIT_UUID = uuShortCodeToUuid("2A7E")

        /**
         * SpecificationName: Aerobic Heart Rate Upper Limit
         * SpecificationType: org.bluetooth.characteristic.aerobic_heart_rate_upper_limit
         * AssignedNumber: 0x2A84
         */
        val AEROBIC_HEART_RATE_UPPER_LIMIT_UUID = uuShortCodeToUuid("2A84")

        /**
         * SpecificationName: Aerobic Threshold
         * SpecificationType: org.bluetooth.characteristic.aerobic_threshold
         * AssignedNumber: 0x2A7F
         */
        val AEROBIC_THRESHOLD_UUID = uuShortCodeToUuid("2A7F")

        /**
         * SpecificationName: Age
         * SpecificationType: org.bluetooth.characteristic.age
         * AssignedNumber: 0x2A80
         */
        val AGE_UUID = uuShortCodeToUuid("2A80")

        /**
         * SpecificationName: Aggregate
         * SpecificationType: org.bluetooth.characteristic.aggregate
         * AssignedNumber: 0x2A5A
         */
        val AGGREGATE_UUID = uuShortCodeToUuid("2A5A")

        /**
         * SpecificationName: Alert Category ID
         * SpecificationType: org.bluetooth.characteristic.alert_category_id
         * AssignedNumber: 0x2A43
         */
        val ALERT_CATEGORY_ID_UUID = uuShortCodeToUuid("2A43")

        /**
         * SpecificationName: Alert Category ID Bit Mask
         * SpecificationType: org.bluetooth.characteristic.alert_category_id_bit_mask
         * AssignedNumber: 0x2A42
         */
        val ALERT_CATEGORY_ID_BIT_MASK_UUID = uuShortCodeToUuid("2A42")

        /**
         * SpecificationName: Alert Level
         * SpecificationType: org.bluetooth.characteristic.alert_level
         * AssignedNumber: 0x2A06
         */
        val ALERT_LEVEL_UUID = uuShortCodeToUuid("2A06")

        /**
         * SpecificationName: Alert Notification Control Point
         * SpecificationType: org.bluetooth.characteristic.alert_notification_control_point
         * AssignedNumber: 0x2A44
         */
        val ALERT_NOTIFICATION_CONTROL_POINT_UUID = uuShortCodeToUuid("2A44")

        /**
         * SpecificationName: Alert Status
         * SpecificationType: org.bluetooth.characteristic.alert_status
         * AssignedNumber: 0x2A3F
         */
        val ALERT_STATUS_UUID = uuShortCodeToUuid("2A3F")

        /**
         * SpecificationName: Altitude
         * SpecificationType: org.bluetooth.characteristic.altitude
         * AssignedNumber: 0x2AB3
         */
        val ALTITUDE_UUID = uuShortCodeToUuid("2AB3")

        /**
         * SpecificationName: Anaerobic Heart Rate Lower Limit
         * SpecificationType: org.bluetooth.characteristic.anaerobic_heart_rate_lower_limit
         * AssignedNumber: 0x2A81
         */
        val ANAEROBIC_HEART_RATE_LOWER_LIMIT_UUID = uuShortCodeToUuid("2A81")

        /**
         * SpecificationName: Anaerobic Heart Rate Upper Limit
         * SpecificationType: org.bluetooth.characteristic.anaerobic_heart_rate_upper_limit
         * AssignedNumber: 0x2A82
         */
        val ANAEROBIC_HEART_RATE_UPPER_LIMIT_UUID = uuShortCodeToUuid("2A82")

        /**
         * SpecificationName: Anaerobic Threshold
         * SpecificationType: org.bluetooth.characteristic.anaerobic_threshold
         * AssignedNumber: 0x2A83
         */
        val ANAEROBIC_THRESHOLD_UUID = uuShortCodeToUuid("2A83")

        /**
         * SpecificationName: Analog
         * SpecificationType: org.bluetooth.characteristic.analog
         * AssignedNumber: 0x2A58
         */
        val ANALOG_UUID = uuShortCodeToUuid("2A58")

        /**
         * SpecificationName: Apparent Wind Direction
         * SpecificationType: org.bluetooth.characteristic.apparent_wind_direction
         * AssignedNumber: 0x2A73
         */
        val APPARENT_WIND_DIRECTION_UUID = uuShortCodeToUuid("2A73")

        /**
         * SpecificationName: Apparent Wind Speed
         * SpecificationType: org.bluetooth.characteristic.apparent_wind_speed
         * AssignedNumber: 0x2A72
         */
        val APPARENT_WIND_SPEED_UUID = uuShortCodeToUuid("2A72")

        /**
         * SpecificationName: Appearance
         * SpecificationType: org.bluetooth.characteristic.gap.appearance
         * AssignedNumber: 0x2A01
         */
        val APPEARANCE_UUID = uuShortCodeToUuid("2A01")

        /**
         * SpecificationName: Barometric Pressure Trend
         * SpecificationType: org.bluetooth.characteristic.barometric_pressure_trend
         * AssignedNumber: 0x2AA3
         */
        val BAROMETRIC_PRESSURE_TREND_UUID = uuShortCodeToUuid("2AA3")

        /**
         * SpecificationName: Battery Level
         * SpecificationType: org.bluetooth.characteristic.battery_level
         * AssignedNumber: 0x2A19
         */
        val BATTERY_LEVEL_UUID = uuShortCodeToUuid("2A19")

        /**
         * SpecificationName: Blood Pressure Feature
         * SpecificationType: org.bluetooth.characteristic.blood_pressure_feature
         * AssignedNumber: 0x2A49
         */
        val BLOOD_PRESSURE_FEATURE_UUID = uuShortCodeToUuid("2A49")

        /**
         * SpecificationName: Blood Pressure Measurement
         * SpecificationType: org.bluetooth.characteristic.blood_pressure_measurement
         * AssignedNumber: 0x2A35
         */
        val BLOOD_PRESSURE_MEASUREMENT_UUID = uuShortCodeToUuid("2A35")

        /**
         * SpecificationName: Body Composition Feature
         * SpecificationType: org.bluetooth.characteristic.body_composition_feature
         * AssignedNumber: 0x2A9B
         */
        val BODY_COMPOSITION_FEATURE_UUID = uuShortCodeToUuid("2A9B")

        /**
         * SpecificationName: Body Composition Measurement
         * SpecificationType: org.bluetooth.characteristic.body_composition_measurement
         * AssignedNumber: 0x2A9C
         */
        val BODY_COMPOSITION_MEASUREMENT_UUID = uuShortCodeToUuid("2A9C")

        /**
         * SpecificationName: Body Sensor Location
         * SpecificationType: org.bluetooth.characteristic.body_sensor_location
         * AssignedNumber: 0x2A38
         */
        val BODY_SENSOR_LOCATION_UUID = uuShortCodeToUuid("2A38")

        /**
         * SpecificationName: Bond Management Control Point
         * SpecificationType: org.bluetooth.characteristic.bond_management_control_point
         * AssignedNumber: 0x2AA4
         */
        val BOND_MANAGEMENT_CONTROL_POINT_UUID = uuShortCodeToUuid("2AA4")

        /**
         * SpecificationName: Bond Management Feature
         * SpecificationType: org.bluetooth.characteristic.bond_management_feature
         * AssignedNumber: 0x2AA5
         */
        val BOND_MANAGEMENT_FEATURE_UUID = uuShortCodeToUuid("2AA5")

        /**
         * SpecificationName: Boot Keyboard Input Report
         * SpecificationType: org.bluetooth.characteristic.boot_keyboard_input_report
         * AssignedNumber: 0x2A22
         */
        val BOOT_KEYBOARD_INPUT_REPORT_UUID = uuShortCodeToUuid("2A22")

        /**
         * SpecificationName: Boot Keyboard Output Report
         * SpecificationType: org.bluetooth.characteristic.boot_keyboard_output_report
         * AssignedNumber: 0x2A32
         */
        val BOOT_KEYBOARD_OUTPUT_REPORT_UUID = uuShortCodeToUuid("2A32")

        /**
         * SpecificationName: Boot Mouse Input Report
         * SpecificationType: org.bluetooth.characteristic.boot_mouse_input_report
         * AssignedNumber: 0x2A33
         */
        val BOOT_MOUSE_INPUT_REPORT_UUID = uuShortCodeToUuid("2A33")

        /**
         * SpecificationName: Central Address Resolution
         * SpecificationType: org.bluetooth.characteristic.gap.central_address_resolution_support
         * AssignedNumber: 0x2AA6
         */
        val CENTRAL_ADDRESS_RESOLUTION_UUID = uuShortCodeToUuid("2AA6")

        /**
         * SpecificationName: CGM Feature
         * SpecificationType: org.bluetooth.characteristic.cgm_feature
         * AssignedNumber: 0x2AA8
         */
        val CGM_FEATURE_UUID = uuShortCodeToUuid("2AA8")

        /**
         * SpecificationName: CGM Measurement
         * SpecificationType: org.bluetooth.characteristic.cgm_measurement
         * AssignedNumber: 0x2AA7
         */
        val CGM_MEASUREMENT_UUID = uuShortCodeToUuid("2AA7")

        /**
         * SpecificationName: CGM Session Run Time
         * SpecificationType: org.bluetooth.characteristic.cgm_session_run_time
         * AssignedNumber: 0x2AAB
         */
        val CGM_SESSION_RUN_TIME_UUID = uuShortCodeToUuid("2AAB")

        /**
         * SpecificationName: CGM Session Start Time
         * SpecificationType: org.bluetooth.characteristic.cgm_session_start_time
         * AssignedNumber: 0x2AAA
         */
        val CGM_SESSION_START_TIME_UUID = uuShortCodeToUuid("2AAA")

        /**
         * SpecificationName: CGM Specific Ops Control Point
         * SpecificationType: org.bluetooth.characteristic.cgm_specific_ops_control_point
         * AssignedNumber: 0x2AAC
         */
        val CGM_SPECIFIC_OPS_CONTROL_POINT_UUID = uuShortCodeToUuid("2AAC")

        /**
         * SpecificationName: CGM Status
         * SpecificationType: org.bluetooth.characteristic.cgm_status
         * AssignedNumber: 0x2AA9
         */
        val CGM_STATUS_UUID = uuShortCodeToUuid("2AA9")

        /**
         * SpecificationName: CSC Feature
         * SpecificationType: org.bluetooth.characteristic.csc_feature
         * AssignedNumber: 0x2A5C
         */
        val CSC_FEATURE_UUID = uuShortCodeToUuid("2A5C")

        /**
         * SpecificationName: CSC Measurement
         * SpecificationType: org.bluetooth.characteristic.csc_measurement
         * AssignedNumber: 0x2A5B
         */
        val CSC_MEASUREMENT_UUID = uuShortCodeToUuid("2A5B")

        /**
         * SpecificationName: Current Time
         * SpecificationType: org.bluetooth.characteristic.current_time
         * AssignedNumber: 0x2A2B
         */
        val CURRENT_TIME_UUID = uuShortCodeToUuid("2A2B")

        /**
         * SpecificationName: Cycling Power Control Point
         * SpecificationType: org.bluetooth.characteristic.cycling_power_control_point
         * AssignedNumber: 0x2A66
         */
        val CYCLING_POWER_CONTROL_POINT_UUID = uuShortCodeToUuid("2A66")

        /**
         * SpecificationName: Cycling Power Feature
         * SpecificationType: org.bluetooth.characteristic.cycling_power_feature
         * AssignedNumber: 0x2A65
         */
        val CYCLING_POWER_FEATURE_UUID = uuShortCodeToUuid("2A65")

        /**
         * SpecificationName: Cycling Power Measurement
         * SpecificationType: org.bluetooth.characteristic.cycling_power_measurement
         * AssignedNumber: 0x2A63
         */
        val CYCLING_POWER_MEASUREMENT_UUID = uuShortCodeToUuid("2A63")

        /**
         * SpecificationName: Cycling Power Vector
         * SpecificationType: org.bluetooth.characteristic.cycling_power_vector
         * AssignedNumber: 0x2A64
         */
        val CYCLING_POWER_VECTOR_UUID = uuShortCodeToUuid("2A64")

        /**
         * SpecificationName: Database Change Increment
         * SpecificationType: org.bluetooth.characteristic.database_change_increment
         * AssignedNumber: 0x2A99
         */
        val DATABASE_CHANGE_INCREMENT_UUID = uuShortCodeToUuid("2A99")

        /**
         * SpecificationName: Date of Birth
         * SpecificationType: org.bluetooth.characteristic.date_of_birth
         * AssignedNumber: 0x2A85
         */
        val DATE_OF_BIRTH_UUID = uuShortCodeToUuid("2A85")

        /**
         * SpecificationName: Date of Threshold Assessment
         * SpecificationType: org.bluetooth.characteristic.date_of_threshold_assessment
         * AssignedNumber: 0x2A86
         */
        val DATE_OF_THRESHOLD_ASSESSMENT_UUID = uuShortCodeToUuid("2A86")

        /**
         * SpecificationName: Date Time
         * SpecificationType: org.bluetooth.characteristic.date_time
         * AssignedNumber: 0x2A08
         */
        val DATE_TIME_UUID = uuShortCodeToUuid("2A08")

        /**
         * SpecificationName: Day Date Time
         * SpecificationType: org.bluetooth.characteristic.day_date_time
         * AssignedNumber: 0x2A0A
         */
        val DAY_DATE_TIME_UUID = uuShortCodeToUuid("2A0A")

        /**
         * SpecificationName: Day of Week
         * SpecificationType: org.bluetooth.characteristic.day_of_week
         * AssignedNumber: 0x2A09
         */
        val DAY_OF_WEEK_UUID = uuShortCodeToUuid("2A09")

        /**
         * SpecificationName: Descriptor Value Changed
         * SpecificationType: org.bluetooth.characteristic.descriptor_value_changed
         * AssignedNumber: 0x2A7D
         */
        val DESCRIPTOR_VALUE_CHANGED_UUID = uuShortCodeToUuid("2A7D")

        /**
         * SpecificationName: Device Name
         * SpecificationType: org.bluetooth.characteristic.gap.device_name
         * AssignedNumber: 0x2A00
         */
        val DEVICE_NAME_UUID = uuShortCodeToUuid("2A00")

        /**
         * SpecificationName: Dew Point
         * SpecificationType: org.bluetooth.characteristic.dew_point
         * AssignedNumber: 0x2A7B
         */
        val DEW_POINT_UUID = uuShortCodeToUuid("2A7B")

        /**
         * SpecificationName: Digital
         * SpecificationType: org.bluetooth.characteristic.digital
         * AssignedNumber: 0x2A56
         */
        val DIGITAL_UUID = uuShortCodeToUuid("2A56")

        /**
         * SpecificationName: DST Offset
         * SpecificationType: org.bluetooth.characteristic.dst_offset
         * AssignedNumber: 0x2A0D
         */
        val DST_OFFSET_UUID = uuShortCodeToUuid("2A0D")

        /**
         * SpecificationName: Elevation
         * SpecificationType: org.bluetooth.characteristic.elevation
         * AssignedNumber: 0x2A6C
         */
        val ELEVATION_UUID = uuShortCodeToUuid("2A6C")

        /**
         * SpecificationName: Email Address
         * SpecificationType: org.bluetooth.characteristic.email_address
         * AssignedNumber: 0x2A87
         */
        val EMAIL_ADDRESS_UUID = uuShortCodeToUuid("2A87")

        /**
         * SpecificationName: Exact Time 256
         * SpecificationType: org.bluetooth.characteristic.exact_time_256
         * AssignedNumber: 0x2A0C
         */
        val EXACT_TIME_256_UUID = uuShortCodeToUuid("2A0C")

        /**
         * SpecificationName: Fat Burn Heart Rate Lower Limit
         * SpecificationType: org.bluetooth.characteristic.fat_burn_heart_rate_lower_limit
         * AssignedNumber: 0x2A88
         */
        val FAT_BURN_HEART_RATE_LOWER_LIMIT_UUID = uuShortCodeToUuid("2A88")

        /**
         * SpecificationName: Fat Burn Heart Rate Upper Limit
         * SpecificationType: org.bluetooth.characteristic.fat_burn_heart_rate_upper_limit
         * AssignedNumber: 0x2A89
         */
        val FAT_BURN_HEART_RATE_UPPER_LIMIT_UUID = uuShortCodeToUuid("2A89")

        /**
         * SpecificationName: Firmware Revision String
         * SpecificationType: org.bluetooth.characteristic.firmware_revision_string
         * AssignedNumber: 0x2A26
         */
        val FIRMWARE_REVISION_STRING_UUID = uuShortCodeToUuid("2A26")

        /**
         * SpecificationName: First Name
         * SpecificationType: org.bluetooth.characteristic.first_name
         * AssignedNumber: 0x2A8A
         */
        val FIRST_NAME_UUID = uuShortCodeToUuid("2A8A")

        /**
         * SpecificationName: Five Zone Heart Rate Limits
         * SpecificationType: org.bluetooth.characteristic.five_zone_heart_rate_limits
         * AssignedNumber: 0x2A8B
         */
        val FIVE_ZONE_HEART_RATE_LIMITS_UUID = uuShortCodeToUuid("2A8B")

        /**
         * SpecificationName: Floor Number
         * SpecificationType: org.bluetooth.characteristic.floor_number
         * AssignedNumber: 0x2AB2
         */
        val FLOOR_NUMBER_UUID = uuShortCodeToUuid("2AB2")

        /**
         * SpecificationName: Gender
         * SpecificationType: org.bluetooth.characteristic.gender
         * AssignedNumber: 0x2A8C
         */
        val GENDER_UUID = uuShortCodeToUuid("2A8C")

        /**
         * SpecificationName: Glucose Feature
         * SpecificationType: org.bluetooth.characteristic.glucose_feature
         * AssignedNumber: 0x2A51
         */
        val GLUCOSE_FEATURE_UUID = uuShortCodeToUuid("2A51")

        /**
         * SpecificationName: Glucose Measurement
         * SpecificationType: org.bluetooth.characteristic.glucose_measurement
         * AssignedNumber: 0x2A18
         */
        val GLUCOSE_MEASUREMENT_UUID = uuShortCodeToUuid("2A18")

        /**
         * SpecificationName: Glucose Measurement Context
         * SpecificationType: org.bluetooth.characteristic.glucose_measurement_context
         * AssignedNumber: 0x2A34
         */
        val GLUCOSE_MEASUREMENT_CONTEXT_UUID = uuShortCodeToUuid("2A34")

        /**
         * SpecificationName: Gust Factor
         * SpecificationType: org.bluetooth.characteristic.gust_factor
         * AssignedNumber: 0x2A74
         */
        val GUST_FACTOR_UUID = uuShortCodeToUuid("2A74")

        /**
         * SpecificationName: Hardware Revision String
         * SpecificationType: org.bluetooth.characteristic.hardware_revision_string
         * AssignedNumber: 0x2A27
         */
        val HARDWARE_REVISION_STRING_UUID = uuShortCodeToUuid("2A27")

        /**
         * SpecificationName: Heart Rate Control Point
         * SpecificationType: org.bluetooth.characteristic.heart_rate_control_point
         * AssignedNumber: 0x2A39
         */
        val HEART_RATE_CONTROL_POINT_UUID = uuShortCodeToUuid("2A39")

        /**
         * SpecificationName: Heart Rate Max
         * SpecificationType: org.bluetooth.characteristic.heart_rate_max
         * AssignedNumber: 0x2A8D
         */
        val HEART_RATE_MAX_UUID = uuShortCodeToUuid("2A8D")

        /**
         * SpecificationName: Heart Rate Measurement
         * SpecificationType: org.bluetooth.characteristic.heart_rate_measurement
         * AssignedNumber: 0x2A37
         */
        val HEART_RATE_MEASUREMENT_UUID = uuShortCodeToUuid("2A37")

        /**
         * SpecificationName: Heat Index
         * SpecificationType: org.bluetooth.characteristic.heat_index
         * AssignedNumber: 0x2A7A
         */
        val HEAT_INDEX_UUID = uuShortCodeToUuid("2A7A")

        /**
         * SpecificationName: Height
         * SpecificationType: org.bluetooth.characteristic.height
         * AssignedNumber: 0x2A8E
         */
        val HEIGHT_UUID = uuShortCodeToUuid("2A8E")

        /**
         * SpecificationName: HID Control Point
         * SpecificationType: org.bluetooth.characteristic.hid_control_point
         * AssignedNumber: 0x2A4C
         */
        val HID_CONTROL_POINT_UUID = uuShortCodeToUuid("2A4C")

        /**
         * SpecificationName: HID Information
         * SpecificationType: org.bluetooth.characteristic.hid_information
         * AssignedNumber: 0x2A4A
         */
        val HID_INFORMATION_UUID = uuShortCodeToUuid("2A4A")

        /**
         * SpecificationName: Hip Circumference
         * SpecificationType: org.bluetooth.characteristic.hip_circumference
         * AssignedNumber: 0x2A8F
         */
        val HIP_CIRCUMFERENCE_UUID = uuShortCodeToUuid("2A8F")

        /**
         * SpecificationName: HTTP Control Point
         * SpecificationType: org.bluetooth.characteristic.http_control_point
         * AssignedNumber: 0x2ABA
         */
        val HTTP_CONTROL_POINT_UUID = uuShortCodeToUuid("2ABA")

        /**
         * SpecificationName: HTTP Entity Body
         * SpecificationType: org.bluetooth.characteristic.http_entity_body
         * AssignedNumber: 0x2AB9
         */
        val HTTP_ENTITY_BODY_UUID = uuShortCodeToUuid("2AB9")

        /**
         * SpecificationName: HTTP Headers
         * SpecificationType: org.bluetooth.characteristic.http_headers
         * AssignedNumber: 0x2AB7
         */
        val HTTP_HEADERS_UUID = uuShortCodeToUuid("2AB7")

        /**
         * SpecificationName: HTTP Status Code
         * SpecificationType: org.bluetooth.characteristic.http_status_code
         * AssignedNumber: 0x2AB8
         */
        val HTTP_STATUS_CODE_UUID = uuShortCodeToUuid("2AB8")

        /**
         * SpecificationName: HTTPS Security
         * SpecificationType: org.bluetooth.characteristic.https_security
         * AssignedNumber: 0x2ABB
         */
        val HTTPS_SECURITY_UUID = uuShortCodeToUuid("2ABB")

        /**
         * SpecificationName: Humidity
         * SpecificationType: org.bluetooth.characteristic.humidity
         * AssignedNumber: 0x2A6F
         */
        val HUMIDITY_UUID = uuShortCodeToUuid("2A6F")

        /**
         * SpecificationName: IEEE 11073-20601 Regulatory Certification Data List
         * SpecificationType: org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list
         * AssignedNumber: 0x2A2A
         */
        val IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST_UUID = uuShortCodeToUuid("2A2A")

        /**
         * SpecificationName: Indoor Positioning Configuration
         * SpecificationType: org.bluetooth.characteristic.indoor_positioning_configuration
         * AssignedNumber: 0x2AAD
         */
        val INDOOR_POSITIONING_CONFIGURATION_UUID = uuShortCodeToUuid("2AAD")

        /**
         * SpecificationName: Intermediate Cuff Pressure
         * SpecificationType: org.bluetooth.characteristic.intermediate_cuff_pressure
         * AssignedNumber: 0x2A36
         */
        val INTERMEDIATE_CUFF_PRESSURE_UUID = uuShortCodeToUuid("2A36")

        /**
         * SpecificationName: Intermediate Temperature
         * SpecificationType: org.bluetooth.characteristic.intermediate_temperature
         * AssignedNumber: 0x2A1E
         */
        val INTERMEDIATE_TEMPERATURE_UUID = uuShortCodeToUuid("2A1E")

        /**
         * SpecificationName: Irradiance
         * SpecificationType: org.bluetooth.characteristic.irradiance
         * AssignedNumber: 0x2A77
         */
        val IRRADIANCE_UUID = uuShortCodeToUuid("2A77")

        /**
         * SpecificationName: Language
         * SpecificationType: org.bluetooth.characteristic.language
         * AssignedNumber: 0x2AA2
         */
        val LANGUAGE_UUID = uuShortCodeToUuid("2AA2")

        /**
         * SpecificationName: Last Name
         * SpecificationType: org.bluetooth.characteristic.last_name
         * AssignedNumber: 0x2A90
         */
        val LAST_NAME_UUID = uuShortCodeToUuid("2A90")

        /**
         * SpecificationName: Latitude
         * SpecificationType: org.bluetooth.characteristic.latitude
         * AssignedNumber: 0x2AAE
         */
        val LATITUDE_UUID = uuShortCodeToUuid("2AAE")

        /**
         * SpecificationName: LN Control Point
         * SpecificationType: org.bluetooth.characteristic.ln_control_point
         * AssignedNumber: 0x2A6B
         */
        val LN_CONTROL_POINT_UUID = uuShortCodeToUuid("2A6B")

        /**
         * SpecificationName: LN Feature
         * SpecificationType: org.bluetooth.characteristic.ln_feature
         * AssignedNumber: 0x2A6A
         */
        val LN_FEATURE_UUID = uuShortCodeToUuid("2A6A")

        /**
         * SpecificationName: Local East Coordinate
         * SpecificationType: org.bluetooth.characteristic.local_east_coordinate
         * AssignedNumber: 0x2AB1
         */
        val LOCAL_EAST_COORDINATE_UUID = uuShortCodeToUuid("2AB1")

        /**
         * SpecificationName: Local North Coordinate
         * SpecificationType: org.bluetooth.characteristic.local_north_coordinate
         * AssignedNumber: 0x2AB0
         */
        val LOCAL_NORTH_COORDINATE_UUID = uuShortCodeToUuid("2AB0")

        /**
         * SpecificationName: Local Time Information
         * SpecificationType: org.bluetooth.characteristic.local_time_information
         * AssignedNumber: 0x2A0F
         */
        val LOCAL_TIME_INFORMATION_UUID = uuShortCodeToUuid("2A0F")

        /**
         * SpecificationName: Location and Speed
         * SpecificationType: org.bluetooth.characteristic.location_and_speed
         * AssignedNumber: 0x2A67
         */
        val LOCATION_AND_SPEED_UUID = uuShortCodeToUuid("2A67")

        /**
         * SpecificationName: Location Name
         * SpecificationType: org.bluetooth.characteristic.location_name
         * AssignedNumber: 0x2AB5
         */
        val LOCATION_NAME_UUID = uuShortCodeToUuid("2AB5")

        /**
         * SpecificationName: Longitude
         * SpecificationType: org.bluetooth.characteristic.longitude
         * AssignedNumber: 0x2AAF
         */
        val LONGITUDE_UUID = uuShortCodeToUuid("2AAF")

        /**
         * SpecificationName: Magnetic Declination
         * SpecificationType: org.bluetooth.characteristic.magnetic_declination
         * AssignedNumber: 0x2A2C
         */
        val MAGNETIC_DECLINATION_UUID = uuShortCodeToUuid("2A2C")

        /**
         * SpecificationName: Magnetic Flux Density - 2D
         * SpecificationType: org.bluetooth.characteristic.magnetic_flux_density_2D
         * AssignedNumber: 0x2AA0
         */
        val MAGNETIC_FLUX_DENSITY_2D_UUID = uuShortCodeToUuid("2AA0")

        /**
         * SpecificationName: Magnetic Flux Density - 3D
         * SpecificationType: org.bluetooth.characteristic.magnetic_flux_density_3D
         * AssignedNumber: 0x2AA1
         */
        val MAGNETIC_FLUX_DENSITY_3D_UUID = uuShortCodeToUuid("2AA1")

        /**
         * SpecificationName: Manufacturer Name String
         * SpecificationType: org.bluetooth.characteristic.manufacturer_name_string
         * AssignedNumber: 0x2A29
         */
        val MANUFACTURER_NAME_STRING_UUID = uuShortCodeToUuid("2A29")

        /**
         * SpecificationName: Maximum Recommended Heart Rate
         * SpecificationType: org.bluetooth.characteristic.maximum_recommended_heart_rate
         * AssignedNumber: 0x2A91
         */
        val MAXIMUM_RECOMMENDED_HEART_RATE_UUID = uuShortCodeToUuid("2A91")

        /**
         * SpecificationName: Measurement Interval
         * SpecificationType: org.bluetooth.characteristic.measurement_interval
         * AssignedNumber: 0x2A21
         */
        val MEASUREMENT_INTERVAL_UUID = uuShortCodeToUuid("2A21")

        /**
         * SpecificationName: Model Number String
         * SpecificationType: org.bluetooth.characteristic.model_number_string
         * AssignedNumber: 0x2A24
         */
        val MODEL_NUMBER_STRING_UUID = uuShortCodeToUuid("2A24")

        /**
         * SpecificationName: Navigation
         * SpecificationType: org.bluetooth.characteristic.navigation
         * AssignedNumber: 0x2A68
         */
        val NAVIGATION_UUID = uuShortCodeToUuid("2A68")

        /**
         * SpecificationName: New Alert
         * SpecificationType: org.bluetooth.characteristic.new_alert
         * AssignedNumber: 0x2A46
         */
        val NEW_ALERT_UUID = uuShortCodeToUuid("2A46")

        /**
         * SpecificationName: Object Action Control Point
         * SpecificationType: org.bluetooth.characteristic.object_action_control_point
         * AssignedNumber: 0x2AC5
         */
        val OBJECT_ACTION_CONTROL_POINT_UUID = uuShortCodeToUuid("2AC5")

        /**
         * SpecificationName: Object Changed
         * SpecificationType: org.bluetooth.characteristic.object_changed
         * AssignedNumber: 0x2AC8
         */
        val OBJECT_CHANGED_UUID = uuShortCodeToUuid("2AC8")

        /**
         * SpecificationName: Object First-Created
         * SpecificationType: org.bluetooth.characteristic.object_first_created
         * AssignedNumber: 0x2AC1
         */
        val OBJECT_FIRST_CREATED_UUID = uuShortCodeToUuid("2AC1")

        /**
         * SpecificationName: Object ID
         * SpecificationType: org.bluetooth.characteristic.object_id
         * AssignedNumber: 0x2AC3
         */
        val OBJECT_ID_UUID = uuShortCodeToUuid("2AC3")

        /**
         * SpecificationName: Object Last-Modified
         * SpecificationType: org.bluetooth.characteristic.object_last_modified
         * AssignedNumber: 0x2AC2
         */
        val OBJECT_LAST_MODIFIED_UUID = uuShortCodeToUuid("2AC2")

        /**
         * SpecificationName: Object List Control Point
         * SpecificationType: org.bluetooth.characteristic.object_list_control_point
         * AssignedNumber: 0x2AC6
         */
        val OBJECT_LIST_CONTROL_POINT_UUID = uuShortCodeToUuid("2AC6")

        /**
         * SpecificationName: Object List Filter
         * SpecificationType: org.bluetooth.characteristic.object_list_filter
         * AssignedNumber: 0x2AC7
         */
        val OBJECT_LIST_FILTER_UUID = uuShortCodeToUuid("2AC7")

        /**
         * SpecificationName: Object Name
         * SpecificationType: org.bluetooth.characteristic.object_name
         * AssignedNumber: 0x2ABE
         */
        val OBJECT_NAME_UUID = uuShortCodeToUuid("2ABE")

        /**
         * SpecificationName: Object Properties
         * SpecificationType: org.bluetooth.characteristic.object_properties
         * AssignedNumber: 0x2AC4
         */
        val OBJECT_PROPERTIES_UUID = uuShortCodeToUuid("2AC4")

        /**
         * SpecificationName: Object Size
         * SpecificationType: org.bluetooth.characteristic.object_size
         * AssignedNumber: 0x2AC0
         */
        val OBJECT_SIZE_UUID = uuShortCodeToUuid("2AC0")

        /**
         * SpecificationName: Object Type
         * SpecificationType: org.bluetooth.characteristic.object_type
         * AssignedNumber: 0x2ABF
         */
        val OBJECT_TYPE_UUID = uuShortCodeToUuid("2ABF")

        /**
         * SpecificationName: OTS Feature
         * SpecificationType: org.bluetooth.characteristic.ots_feature
         * AssignedNumber: 0x2ABD
         */
        val OTS_FEATURE_UUID = uuShortCodeToUuid("2ABD")

        /**
         * SpecificationName: Peripheral Preferred Connection Parameters
         * SpecificationType: org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters
         * AssignedNumber: 0x2A04
         */
        val PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS_UUID = uuShortCodeToUuid("2A04")

        /**
         * SpecificationName: Peripheral Privacy Flag
         * SpecificationType: org.bluetooth.characteristic.gap.peripheral_privacy_flag
         * AssignedNumber: 0x2A02
         */
        val PERIPHERAL_PRIVACY_FLAG_UUID = uuShortCodeToUuid("2A02")

        /**
         * SpecificationName: PLX Continuous Measurement
         * SpecificationType: org.bluetooth.characteristic.plx_continuous_measurement
         * AssignedNumber: 0x2A5F
         */
        val PLX_CONTINUOUS_MEASUREMENT_UUID = uuShortCodeToUuid("2A5F")

        /**
         * SpecificationName: PLX Features
         * SpecificationType: org.bluetooth.characteristic.plx_features
         * AssignedNumber: 0x2A60
         */
        val PLX_FEATURES_UUID = uuShortCodeToUuid("2A60")

        /**
         * SpecificationName: PLX Spot-Check Measurement
         * SpecificationType: org.bluetooth.characteristic.plx_spot_check_measurement
         * AssignedNumber: 0x2A5E
         */
        val PLX_SPOT_CHECK_MEASUREMENT_UUID = uuShortCodeToUuid("2A5E")

        /**
         * SpecificationName: PnP ID
         * SpecificationType: org.bluetooth.characteristic.pnp_id
         * AssignedNumber: 0x2A50
         */
        val PNP_ID_UUID = uuShortCodeToUuid("2A50")

        /**
         * SpecificationName: Pollen Concentration
         * SpecificationType: org.bluetooth.characteristic.pollen_concentration
         * AssignedNumber: 0x2A75
         */
        val POLLEN_CONCENTRATION_UUID = uuShortCodeToUuid("2A75")

        /**
         * SpecificationName: Position Quality
         * SpecificationType: org.bluetooth.characteristic.position_quality
         * AssignedNumber: 0x2A69
         */
        val POSITION_QUALITY_UUID = uuShortCodeToUuid("2A69")

        /**
         * SpecificationName: Pressure
         * SpecificationType: org.bluetooth.characteristic.pressure
         * AssignedNumber: 0x2A6D
         */
        val PRESSURE_UUID = uuShortCodeToUuid("2A6D")

        /**
         * SpecificationName: Protocol Mode
         * SpecificationType: org.bluetooth.characteristic.protocol_mode
         * AssignedNumber: 0x2A4E
         */
        val PROTOCOL_MODE_UUID = uuShortCodeToUuid("2A4E")

        /**
         * SpecificationName: Rainfall
         * SpecificationType: org.bluetooth.characteristic.rainfall
         * AssignedNumber: 0x2A78
         */
        val RAINFALL_UUID = uuShortCodeToUuid("2A78")

        /**
         * SpecificationName: Reconnection Address
         * SpecificationType: org.bluetooth.characteristic.gap.reconnection_address
         * AssignedNumber: 0x2A03
         */
        val RECONNECTION_ADDRESS_UUID = uuShortCodeToUuid("2A03")

        /**
         * SpecificationName: Record Access Control Point
         * SpecificationType: org.bluetooth.characteristic.record_access_control_point
         * AssignedNumber: 0x2A52
         */
        val RECORD_ACCESS_CONTROL_POINT_UUID = uuShortCodeToUuid("2A52")

        /**
         * SpecificationName: Reference Time Information
         * SpecificationType: org.bluetooth.characteristic.reference_time_information
         * AssignedNumber: 0x2A14
         */
        val REFERENCE_TIME_INFORMATION_UUID = uuShortCodeToUuid("2A14")

        /**
         * SpecificationName: Report
         * SpecificationType: org.bluetooth.characteristic.report
         * AssignedNumber: 0x2A4D
         */
        val REPORT_UUID = uuShortCodeToUuid("2A4D")

        /**
         * SpecificationName: Report Map
         * SpecificationType: org.bluetooth.characteristic.report_map
         * AssignedNumber: 0x2A4B
         */
        val REPORT_MAP_UUID = uuShortCodeToUuid("2A4B")

        /**
         * SpecificationName: Resolvable Private Address Only
         * SpecificationType: org.bluetooth.characteristic.resolvable_private_address_only
         * AssignedNumber: 2AC9
         */
        val RESOLVABLE_PRIVATE_ADDRESS_ONLY_UUID = uuShortCodeToUuid("2AC9")

        /**
         * SpecificationName: Resting Heart Rate
         * SpecificationType: org.bluetooth.characteristic.resting_heart_rate
         * AssignedNumber: 0x2A92
         */
        val RESTING_HEART_RATE_UUID = uuShortCodeToUuid("2A92")

        /**
         * SpecificationName: Ringer Control Point
         * SpecificationType: org.bluetooth.characteristic.ringer_control_point
         * AssignedNumber: 0x2A40
         */
        val RINGER_CONTROL_POINT_UUID = uuShortCodeToUuid("2A40")

        /**
         * SpecificationName: Ringer Setting
         * SpecificationType: org.bluetooth.characteristic.ringer_setting
         * AssignedNumber: 0x2A41
         */
        val RINGER_SETTING_UUID = uuShortCodeToUuid("2A41")

        /**
         * SpecificationName: RSC Feature
         * SpecificationType: org.bluetooth.characteristic.rsc_feature
         * AssignedNumber: 0x2A54
         */
        val RSC_FEATURE_UUID = uuShortCodeToUuid("2A54")

        /**
         * SpecificationName: RSC Measurement
         * SpecificationType: org.bluetooth.characteristic.rsc_measurement
         * AssignedNumber: 0x2A53
         */
        val RSC_MEASUREMENT_UUID = uuShortCodeToUuid("2A53")

        /**
         * SpecificationName: SC Control Point
         * SpecificationType: org.bluetooth.characteristic.sc_control_point
         * AssignedNumber: 0x2A55
         */
        val SC_CONTROL_POINT_UUID = uuShortCodeToUuid("2A55")

        /**
         * SpecificationName: Scan Interval Window
         * SpecificationType: org.bluetooth.characteristic.scan_interval_window
         * AssignedNumber: 0x2A4F
         */
        val SCAN_INTERVAL_WINDOW_UUID = uuShortCodeToUuid("2A4F")

        /**
         * SpecificationName: Scan Refresh
         * SpecificationType: org.bluetooth.characteristic.scan_refresh
         * AssignedNumber: 0x2A31
         */
        val SCAN_REFRESH_UUID = uuShortCodeToUuid("2A31")

        /**
         * SpecificationName: Sensor Location
         * SpecificationType: org.blueooth.characteristic.sensor_location
         * AssignedNumber: 0x2A5D
         */
        val SENSOR_LOCATION_UUID = uuShortCodeToUuid("2A5D")

        /**
         * SpecificationName: Serial Number String
         * SpecificationType: org.bluetooth.characteristic.serial_number_string
         * AssignedNumber: 0x2A25
         */
        val SERIAL_NUMBER_STRING_UUID = uuShortCodeToUuid("2A25")

        /**
         * SpecificationName: Service Changed
         * SpecificationType: org.bluetooth.characteristic.gatt.service_changed
         * AssignedNumber: 0x2A05
         */
        val SERVICE_CHANGED_UUID = uuShortCodeToUuid("2A05")

        /**
         * SpecificationName: Software Revision String
         * SpecificationType: org.bluetooth.characteristic.software_revision_string
         * AssignedNumber: 0x2A28
         */
        val SOFTWARE_REVISION_STRING_UUID = uuShortCodeToUuid("2A28")

        /**
         * SpecificationName: Sport Type for Aerobic and Anaerobic Thresholds
         * SpecificationType: org.bluetooth.characteristic.sport_type_for_aerobic_and_anaerobic_thresholds
         * AssignedNumber: 0x2A93
         */
        val SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS_UUID = uuShortCodeToUuid("2A93")

        /**
         * SpecificationName: Supported New Alert Category
         * SpecificationType: org.bluetooth.characteristic.supported_new_alert_category
         * AssignedNumber: 0x2A47
         */
        val SUPPORTED_NEW_ALERT_CATEGORY_UUID = uuShortCodeToUuid("2A47")

        /**
         * SpecificationName: Supported Unread Alert Category
         * SpecificationType: org.bluetooth.characteristic.supported_unread_alert_category
         * AssignedNumber: 0x2A48
         */
        val SUPPORTED_UNREAD_ALERT_CATEGORY_UUID = uuShortCodeToUuid("2A48")

        /**
         * SpecificationName: System ID
         * SpecificationType: org.bluetooth.characteristic.system_id
         * AssignedNumber: 0x2A23
         */
        val SYSTEM_ID_UUID = uuShortCodeToUuid("2A23")

        /**
         * SpecificationName: TDS Control Point
         * SpecificationType: org.bluetooth.characteristic.tds_control_point
         * AssignedNumber: 0x2ABC
         */
        val TDS_CONTROL_POINT_UUID = uuShortCodeToUuid("2ABC")

        /**
         * SpecificationName: Temperature
         * SpecificationType: org.bluetooth.characteristic.temperature
         * AssignedNumber: 0x2A6E
         */
        val TEMPERATURE_UUID = uuShortCodeToUuid("2A6E")

        /**
         * SpecificationName: Temperature Measurement
         * SpecificationType: org.bluetooth.characteristic.temperature_measurement
         * AssignedNumber: 0x2A1C
         */
        val TEMPERATURE_MEASUREMENT_UUID = uuShortCodeToUuid("2A1C")

        /**
         * SpecificationName: Temperature Type
         * SpecificationType: org.bluetooth.characteristic.temperature_type
         * AssignedNumber: 0x2A1D
         */
        val TEMPERATURE_TYPE_UUID = uuShortCodeToUuid("2A1D")

        /**
         * SpecificationName: Three Zone Heart Rate Limits
         * SpecificationType: org.bluetooth.characteristic.three_zone_heart_rate_limits
         * AssignedNumber: 0x2A94
         */
        val THREE_ZONE_HEART_RATE_LIMITS_UUID = uuShortCodeToUuid("2A94")

        /**
         * SpecificationName: Time Accuracy
         * SpecificationType: org.bluetooth.characteristic.time_accuracy
         * AssignedNumber: 0x2A12
         */
        val TIME_ACCURACY_UUID = uuShortCodeToUuid("2A12")

        /**
         * SpecificationName: Time Source
         * SpecificationType: org.bluetooth.characteristic.time_source
         * AssignedNumber: 0x2A13
         */
        val TIME_SOURCE_UUID = uuShortCodeToUuid("2A13")

        /**
         * SpecificationName: Time Update Control Point
         * SpecificationType: org.bluetooth.characteristic.time_update_control_point
         * AssignedNumber: 0x2A16
         */
        val TIME_UPDATE_CONTROL_POINT_UUID = uuShortCodeToUuid("2A16")

        /**
         * SpecificationName: Time Update State
         * SpecificationType: org.bluetooth.characteristic.time_update_state
         * AssignedNumber: 0x2A17
         */
        val TIME_UPDATE_STATE_UUID = uuShortCodeToUuid("2A17")

        /**
         * SpecificationName: Time with DST
         * SpecificationType: org.bluetooth.characteristic.time_with_dst
         * AssignedNumber: 0x2A11
         */
        val TIME_WITH_DST_UUID = uuShortCodeToUuid("2A11")

        /**
         * SpecificationName: Time Zone
         * SpecificationType: org.bluetooth.characteristic.time_zone
         * AssignedNumber: 0x2A0E
         */
        val TIME_ZONE_UUID = uuShortCodeToUuid("2A0E")

        /**
         * SpecificationName: True Wind Direction
         * SpecificationType: org.bluetooth.characteristic.true_wind_direction
         * AssignedNumber: 0x2A71
         */
        val TRUE_WIND_DIRECTION_UUID = uuShortCodeToUuid("2A71")

        /**
         * SpecificationName: True Wind Speed
         * SpecificationType: org.bluetooth.characteristic.true_wind_speed
         * AssignedNumber: 0x2A70
         */
        val TRUE_WIND_SPEED_UUID = uuShortCodeToUuid("2A70")

        /**
         * SpecificationName: Two Zone Heart Rate Limit
         * SpecificationType: org.bluetooth.characteristic.two_zone_heart_rate_limit
         * AssignedNumber: 0x2A95
         */
        val TWO_ZONE_HEART_RATE_LIMIT_UUID = uuShortCodeToUuid("2A95")

        /**
         * SpecificationName: Tx Power Level
         * SpecificationType: org.bluetooth.characteristic.tx_power_level
         * AssignedNumber: 0x2A07
         */
        val TX_POWER_LEVEL_UUID = uuShortCodeToUuid("2A07")

        /**
         * SpecificationName: Uncertainty
         * SpecificationType: org.bluetooth.characteristic.uncertainty
         * AssignedNumber: 0x2AB4
         */
        val UNCERTAINTY_UUID = uuShortCodeToUuid("2AB4")

        /**
         * SpecificationName: Unread Alert Status
         * SpecificationType: org.bluetooth.characteristic.unread_alert_status
         * AssignedNumber: 0x2A45
         */
        val UNREAD_ALERT_STATUS_UUID = uuShortCodeToUuid("2A45")

        /**
         * SpecificationName: URI
         * SpecificationType: org.bluetooth.characteristic.uri
         * AssignedNumber: 0x2AB6
         */
        val URI_UUID = uuShortCodeToUuid("2AB6")

        /**
         * SpecificationName: User Control Point
         * SpecificationType: org.bluetooth.characteristic.user_control_point
         * AssignedNumber: 0x2A9F
         */
        val USER_CONTROL_POINT_UUID = uuShortCodeToUuid("2A9F")

        /**
         * SpecificationName: User Index
         * SpecificationType: org.bluetooth.characteristic.user_index
         * AssignedNumber: 0x2A9A
         */
        val USER_INDEX_UUID = uuShortCodeToUuid("2A9A")

        /**
         * SpecificationName: UV Index
         * SpecificationType: org.bluetooth.characteristic.uv_index
         * AssignedNumber: 0x2A76
         */
        val UV_INDEX_UUID = uuShortCodeToUuid("2A76")

        /**
         * SpecificationName: VO2 Max
         * SpecificationType: org.bluetooth.characteristic.vo2_max
         * AssignedNumber: 0x2A96
         */
        val VO2_MAX_UUID = uuShortCodeToUuid("2A96")

        /**
         * SpecificationName: Waist Circumference
         * SpecificationType: org.bluetooth.characteristic.waist_circumference
         * AssignedNumber: 0x2A97
         */
        val WAIST_CIRCUMFERENCE_UUID = uuShortCodeToUuid("2A97")

        /**
         * SpecificationName: Weight
         * SpecificationType: org.bluetooth.characteristic.weight
         * AssignedNumber: 0x2A98
         */
        val WEIGHT_UUID = uuShortCodeToUuid("2A98")

        /**
         * SpecificationName: Weight Measurement
         * SpecificationType: org.bluetooth.characteristic.weight_measurement
         * AssignedNumber: 0x2A9D
         */
        val WEIGHT_MEASUREMENT_UUID = uuShortCodeToUuid("2A9D")

        /**
         * SpecificationName: Weight Scale Feature
         * SpecificationType: org.bluetooth.characteristic.weight_scale_feature
         * AssignedNumber: 0x2A9E
         */
        val WEIGHT_SCALE_FEATURE_UUID = uuShortCodeToUuid("2A9E")

        /**
         * SpecificationName: Wind Chill
         * SpecificationType: org.bluetooth.characteristic.wind_chill
         * AssignedNumber: 0x2A79
         */
        val WIND_CHILL_UUID = uuShortCodeToUuid("2A79")
    }

    object Descriptors
    {
        /**
         * SpecificationName: Characteristic Aggregate Format
         * SpecificationType: org.bluetooth.descriptor.gatt.characteristic_aggregate_format
         * AssignedNumber: 0x2905
         */
        val CHARACTERISTIC_AGGREGATE_FORMAT_UUID = uuShortCodeToUuid("2905")

        /**
         * SpecificationName: Characteristic Extended Properties
         * SpecificationType: org.bluetooth.descriptor.gatt.characteristic_extended_properties
         * AssignedNumber: 0x2900
         */
        val CHARACTERISTIC_EXTENDED_PROPERTIES_UUID = uuShortCodeToUuid("2900")

        /**
         * SpecificationName: Characteristic Presentation Format
         * SpecificationType: org.bluetooth.descriptor.gatt.characteristic_presentation_format
         * AssignedNumber: 0x2904
         */
        val CHARACTERISTIC_PRESENTATION_FORMAT_UUID = uuShortCodeToUuid("2904")

        /**
         * SpecificationName: Characteristic User Description
         * SpecificationType: org.bluetooth.descriptor.gatt.characteristic_user_description
         * AssignedNumber: 0x2901
         */
        val CHARACTERISTIC_USER_DESCRIPTION_UUID = uuShortCodeToUuid("2901")

        /**
         * SpecificationName: Client Characteristic Configuration
         * SpecificationType: org.bluetooth.descriptor.gatt.client_characteristic_configuration
         * AssignedNumber: 0x2902
         */
        val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = uuShortCodeToUuid("2902")

        /**
         * SpecificationName: Environmental Sensing Configuration
         * SpecificationType: org.bluetooth.descriptor.es_configuration
         * AssignedNumber: 0x290B
         */
        val ENVIRONMENTAL_SENSING_CONFIGURATION_UUID = uuShortCodeToUuid("290B")

        /**
         * SpecificationName: Environmental Sensing Measurement
         * SpecificationType: org.bluetooth.descriptor.es_measurement
         * AssignedNumber: 0x290C
         */
        val ENVIRONMENTAL_SENSING_MEASUREMENT_UUID = uuShortCodeToUuid("290C")

        /**
         * SpecificationName: Environmental Sensing Trigger Setting
         * SpecificationType: org.bluetooth.descriptor.es_trigger_setting
         * AssignedNumber: 0x290D
         */
        val ENVIRONMENTAL_SENSING_TRIGGER_SETTING_UUID = uuShortCodeToUuid("290D")

        /**
         * SpecificationName: External Report Reference
         * SpecificationType: org.bluetooth.descriptor.external_report_reference
         * AssignedNumber: 0x2907
         */
        val EXTERNAL_REPORT_REFERENCE_UUID = uuShortCodeToUuid("2907")

        /**
         * SpecificationName: Number of Digitals
         * SpecificationType: org.bluetooth.descriptor.number_of_digitals
         * AssignedNumber: 0x2909
         */
        val NUMBER_OF_DIGITALS_UUID = uuShortCodeToUuid("2909")

        /**
         * SpecificationName: Report Reference
         * SpecificationType: org.bluetooth.descriptor.report_reference
         * AssignedNumber: 0x2908
         */
        val REPORT_REFERENCE_UUID = uuShortCodeToUuid("2908")

        /**
         * SpecificationName: Server Characteristic Configuration
         * SpecificationType: org.bluetooth.descriptor.gatt.server_characteristic_configuration
         * AssignedNumber: 0x2903
         */
        val SERVER_CHARACTERISTIC_CONFIGURATION_UUID = uuShortCodeToUuid("2903")

        /**
         * SpecificationName: Time Trigger Setting
         * SpecificationType: org.bluetooth.descriptor.time_trigger_setting
         * AssignedNumber: 0x290E
         */
        val TIME_TRIGGER_SETTING_UUID = uuShortCodeToUuid("290E")

        /**
         * SpecificationName: Valid Range
         * SpecificationType: org.bluetooth.descriptor.valid_range
         * AssignedNumber: 0x2906
         */
        val VALID_RANGE_UUID = uuShortCodeToUuid("2906")

        /**
         * SpecificationName: Value Trigger Setting
         * SpecificationType: org.bluetooth.descriptor.value_trigger_setting
         * AssignedNumber: 0x290A
         */
        val VALUE_TRIGGER_SETTING_UUID = uuShortCodeToUuid("290A")
    }
}