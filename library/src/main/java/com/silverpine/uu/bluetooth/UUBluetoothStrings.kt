@file:Suppress("SpellCheckingInspection")
package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import com.silverpine.uu.bluetooth.UUBluetoothStrings.CharacteristicPermissions.fromBitmask
import com.silverpine.uu.bluetooth.UUBluetoothStrings.CharacteristicProperties.fromBitmask
import com.silverpine.uu.core.uuIsBitSet
import java.util.Locale

/**
 * Utility object for Bluetooth-related string constants and formatting functions.
 */
object UUBluetoothStrings
{
    /**
     * Characteristic property string constants and conversion utilities.
     */
    object CharacteristicProperties
    {
        /**
         * String representation for the BROADCAST property.
         */
        const val PROPERTY_BROADCAST = "Broadcast"

        /**
         * String representation for the READ property.
         */
        const val PROPERTY_READ = "Read"

        /**
         * String representation for the WRITE_NO_RESPONSE property.
         */
        const val PROPERTY_WRITE_NO_RESPONSE = "WriteWithoutResponse"

        /**
         * String representation for the WRITE property.
         */
        const val PROPERTY_WRITE = "Write"

        /**
         * String representation for the NOTIFY property.
         */
        const val PROPERTY_NOTIFY = "Notify"

        /**
         * String representation for the INDICATE property.
         */
        const val PROPERTY_INDICATE = "Indicate"

        /**
         * String representation for the SIGNED_WRITE property.
         */
        const val PROPERTY_SIGNED_WRITE = "SignedWrite"

        /**
         * String representation for the EXTENDED_PROPS property.
         */
        const val PROPERTY_EXTENDED_PROPS = "ExtendedProperties"

        /**
         * Converts a single BluetoothGattCharacteristic property flag to its string representation.
         * Note: This function expects a single property flag, not a combined bitmask.
         * For combined properties, use [fromBitmask] instead.
         *
         * @param property the property flag value (e.g., PROPERTY_READ, PROPERTY_WRITE)
         * @return the string representation of the property, or "UnknownCharacteristicProperty-{value}" if not recognized
         */
        fun name(property: Int): String
        {
            return when (property)
            {
                BluetoothGattCharacteristic.PROPERTY_BROADCAST -> PROPERTY_BROADCAST
                BluetoothGattCharacteristic.PROPERTY_READ -> PROPERTY_READ
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE -> PROPERTY_WRITE_NO_RESPONSE
                BluetoothGattCharacteristic.PROPERTY_WRITE -> PROPERTY_WRITE
                BluetoothGattCharacteristic.PROPERTY_NOTIFY -> PROPERTY_NOTIFY
                BluetoothGattCharacteristic.PROPERTY_INDICATE -> PROPERTY_INDICATE
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE -> PROPERTY_SIGNED_WRITE
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS -> PROPERTY_EXTENDED_PROPS
                else -> "UnknownCharacteristicProperty-$property"
            }
        }

        /**
         * Converts a BluetoothGattCharacteristic properties bitmask to a comma-separated string
         * containing all enabled property names.
         *
         * This function checks all known property flags in the bitmask and returns a formatted
         * string containing the names of all properties that are set. For example, a bitmask
         * with both READ and WRITE properties set would return "Read, Write".
         *
         * @param properties the properties bitmask from BluetoothGattCharacteristic.properties
         * @return a comma-separated string of property names, or an empty string if no properties are set
         */
        fun fromBitmask(properties: Int): String
        {
            val parts = ArrayList<String>()

            val bits = arrayOf(
                BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
            )

            for (bit in bits)
            {
                if (properties.uuIsBitSet(bit))
                {
                    parts.add(name(bit))
                }
            }

            return parts.joinToString(", ")
        }
    }

    /**
     * Characteristic permission string constants and conversion utilities.
     */
    object CharacteristicPermissions
    {
        /**
         * String representation for the READ permission.
         */
        const val PERMISSION_READ = "Read"

        /**
         * String representation for the READ_ENCRYPTED permission.
         */
        const val PERMISSION_READ_ENCRYPTED = "ReadEncrypted"

        /**
         * String representation for the READ_ENCRYPTED_MITM permission.
         */
        const val PERMISSION_READ_ENCRYPTED_MITM = "ReadEncryptedMITM"

        /**
         * String representation for the WRITE permission.
         */
        const val PERMISSION_WRITE = "Write"

        /**
         * String representation for the WRITE_ENCRYPTED permission.
         */
        const val PERMISSION_WRITE_ENCRYPTED = "WriteEncrypted"

        /**
         * String representation for the WRITE_ENCRYPTED_MITM permission.
         */
        const val PERMISSION_WRITE_ENCRYPTED_MITM = "WriteEncryptedMITM"

        /**
         * String representation for the WRITE_SIGNED permission.
         */
        const val PERMISSION_WRITE_SIGNED = "WriteSigned"

        /**
         * String representation for the WRITE_SIGNED_MITM permission.
         */
        const val PERMISSION_WRITE_SIGNED_MITM = "WriteSignedMITM"

        /**
         * Converts a single BluetoothGattCharacteristic permission flag to its string representation.
         * Note: This function expects a single permission flag, not a combined bitmask.
         * For combined permissions, use [fromBitmask] instead.
         *
         * @param permission the permission flag value (e.g., PERMISSION_READ, PERMISSION_WRITE)
         * @return the string representation of the permission, or "UnknownCharacteristicPermission-{value}" if not recognized
         */
        fun name(permission: Int): String
        {
            return when (permission)
            {
                BluetoothGattCharacteristic.PERMISSION_READ -> PERMISSION_READ
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED -> PERMISSION_READ_ENCRYPTED
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM -> PERMISSION_READ_ENCRYPTED_MITM
                BluetoothGattCharacteristic.PERMISSION_WRITE -> PERMISSION_WRITE
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED -> PERMISSION_WRITE_ENCRYPTED
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM -> PERMISSION_WRITE_ENCRYPTED_MITM
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED -> PERMISSION_WRITE_SIGNED
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM -> PERMISSION_WRITE_SIGNED_MITM
                else -> "UnknownCharacteristicPermission-$permission"
            }
        }

        /**
         * Converts a BluetoothGattCharacteristic permissions bitmask to a comma-separated string
         * containing all enabled permission names.
         *
         * This function checks all known permission flags in the bitmask and returns a formatted
         * string containing the names of all permissions that are set. For example, a bitmask
         * with both READ and WRITE permissions set would return "Read, Write".
         *
         * @param permissions the permissions bitmask from BluetoothGattCharacteristic.permissions
         * @return a comma-separated string of permission names, or an empty string if no permissions are set
         */
        fun fromBitmask(permissions: Int): String
        {
            val parts = ArrayList<String>()

            val bits = arrayOf(
                BluetoothGattCharacteristic.PERMISSION_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM,
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED,
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM,
            )

            for (bit in bits)
            {
                if (permissions.uuIsBitSet(bit))
                {
                    parts.add(name(bit))
                }
            }

            return parts.joinToString(", ")
        }
    }

    /**
     * Connection state string constants and conversion utilities.
     */
    object ConnectionStates
    {
        /**
         * String representation for the CONNECTED state.
         */
        const val STATE_CONNECTED = "Connected"

        /**
         * String representation for the CONNECTING state.
         */
        const val STATE_CONNECTING = "Connecting"

        /**
         * String representation for the DISCONNECTING state.
         */
        const val STATE_DISCONNECTING = "Disconnecting"

        /**
         * String representation for the DISCONNECTED state.
         */
        const val STATE_DISCONNECTED = "Disconnected"

        /**
         * Converts a BluetoothProfile connection state integer to its string representation.
         *
         * @param connectionState a BluetoothProfile.STATE_* constant
         * @return the string representation of the connection state, or "Unknown-{value}" if not recognized
         */
        fun from(connectionState: Int): String
        {
            return when (connectionState)
            {
                BluetoothProfile.STATE_CONNECTED -> STATE_CONNECTED
                BluetoothProfile.STATE_CONNECTING -> STATE_CONNECTING
                BluetoothProfile.STATE_DISCONNECTING -> STATE_DISCONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> STATE_DISCONNECTED
                else -> String.format(Locale.US, "Unknown-%d", connectionState)
            }
        }
    }

    /**
     * Device type string constants and conversion utilities.
     */
    object DeviceTypes
    {
        /**
         * String representation for the UNKNOWN device type.
         */
        const val DEVICE_TYPE_UNKNOWN = "Unknown"

        /**
         * String representation for the CLASSIC device type.
         */
        const val DEVICE_TYPE_CLASSIC = "Classic"

        /**
         * String representation for the LE device type.
         */
        const val DEVICE_TYPE_LE = "LE"

        /**
         * String representation for the DUAL device type.
         */
        const val DEVICE_TYPE_DUAL = "Dual"

        /**
         * Converts a BluetoothDevice device type integer to its string representation.
         *
         * @param deviceType a BluetoothDevice.DEVICE_TYPE_* constant
         * @return the string representation of the device type, or "Unknown-{value}" if not recognized
         */
        fun from(deviceType: Int): String
        {
            return when (deviceType)
            {
                BluetoothDevice.DEVICE_TYPE_UNKNOWN -> DEVICE_TYPE_UNKNOWN
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> DEVICE_TYPE_CLASSIC
                BluetoothDevice.DEVICE_TYPE_LE -> DEVICE_TYPE_LE
                BluetoothDevice.DEVICE_TYPE_DUAL -> DEVICE_TYPE_DUAL
                else -> String.format(Locale.US, "Unknown-%d", deviceType)
            }
        }
    }

    /**
     * Bond state string constants and conversion utilities.
     */
    object BondStates
    {
        /**
         * String representation for the NONE bond state.
         */
        const val BOND_NONE = "None"

        /**
         * String representation for the BONDED bond state.
         */
        const val BOND_BONDED = "Bonded"

        /**
         * String representation for the BONDING bond state.
         */
        const val BOND_BONDING = "Bonding"

        /**
         * Converts a BluetoothDevice bond state integer to its string representation.
         *
         * @param bondState a BluetoothDevice.BOND_* constant
         * @return the string representation of the bond state, or "Unknown-{value}" if not recognized
         */
        fun from(bondState: Int): String
        {
            return when (bondState)
            {
                BluetoothDevice.BOND_NONE -> BOND_NONE
                BluetoothDevice.BOND_BONDED -> BOND_BONDED
                BluetoothDevice.BOND_BONDING -> BOND_BONDING
                else -> String.format(Locale.US, "Unknown-%d", bondState)
            }
        }
    }

    /**
     * Power state string constants and conversion utilities.
     */
    object PowerStates
    {
        /**
         * String representation for the OFF power state.
         */
        const val STATE_OFF = "Off"

        /**
         * String representation for the ON power state.
         */
        const val STATE_ON = "On"

        /**
         * String representation for the TURNING_ON power state.
         */
        const val STATE_TURNING_ON = "TurningOn"

        /**
         * String representation for the TURNING_OFF power state.
         */
        const val STATE_TURNING_OFF = "TurningOff"

        /**
         * Converts a BluetoothAdapter power state integer to its string representation.
         *
         * @param powerState a BluetoothAdapter.STATE_* constant
         * @return the string representation of the power state, or "Unknown-{value}" if not recognized
         */
        fun from(powerState: Int): String
        {
            return when (powerState)
            {
                BluetoothAdapter.STATE_OFF -> STATE_OFF
                BluetoothAdapter.STATE_ON -> STATE_ON
                BluetoothAdapter.STATE_TURNING_ON -> STATE_TURNING_ON
                BluetoothAdapter.STATE_TURNING_OFF -> STATE_TURNING_OFF
                else -> String.format(Locale.US, "Unknown-%d", powerState)
            }
        }
    }

    /**
     * GATT status string constants and conversion utilities.
     */
    object GattStatuses
    {
        /**
         * String representation for the SUCCESS status.
         */
        const val GATT_SUCCESS = "Success"

        /**
         * String representation for the READ_NOT_PERMITTED status.
         */
        const val GATT_READ_NOT_PERMITTED = "ReadNotPermitted"

        /**
         * String representation for the WRITE_NOT_PERMITTED status.
         */
        const val GATT_WRITE_NOT_PERMITTED = "WriteNotPermitted"

        /**
         * String representation for the INSUFFICIENT_AUTHENTICATION status.
         */
        const val GATT_INSUFFICIENT_AUTHENTICATION = "InsufficientAuthentication"

        /**
         * String representation for the REQUEST_NOT_SUPPORTED status.
         */
        const val GATT_REQUEST_NOT_SUPPORTED = "RequestNotSupported"

        /**
         * String representation for the INSUFFICIENT_ENCRYPTION status.
         */
        const val GATT_INSUFFICIENT_ENCRYPTION = "InsufficientEncryption"

        /**
         * String representation for the INVALID_OFFSET status.
         */
        const val GATT_INVALID_OFFSET = "InvalidOffset"

        /**
         * String representation for the INVALID_ATTRIBUTE_LENGTH status.
         */
        const val GATT_INVALID_ATTRIBUTE_LENGTH = "InvalidAttributeLength"

        /**
         * String representation for the CONNECTION_CONGESTED status.
         */
        const val GATT_CONNECTION_CONGESTED = "ConnectionCongested"

        /**
         * String representation for the FAILURE status.
         */
        const val GATT_FAILURE = "Failure"

        /**
         * String representation for the GATT_ERROR status.
         */
        const val GATT_ERROR = "GattError"

        /**
         * String representation for the DISCONNECTED_BY_PERIPHERAL status.
         */
        const val GATT_DISCONNECTED_BY_PERIPHERAL = "DisconnectedByPeripheral"

        /**
         * Converts a BluetoothGatt GATT status integer to its string representation.
         *
         * @param gattStatus a BluetoothGatt.GATT_* constant or UUBluetoothConstants.GATT_* constant
         * @return the string representation of the GATT status, or "Unknown-{value}" if not recognized
         */
        fun from(gattStatus: Int): String
        {
            return when (gattStatus)
            {
                BluetoothGatt.GATT_SUCCESS -> GATT_SUCCESS
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> GATT_READ_NOT_PERMITTED
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> GATT_WRITE_NOT_PERMITTED
                BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> GATT_INSUFFICIENT_AUTHENTICATION
                BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> GATT_REQUEST_NOT_SUPPORTED
                BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> GATT_INSUFFICIENT_ENCRYPTION
                BluetoothGatt.GATT_INVALID_OFFSET -> GATT_INVALID_OFFSET
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> GATT_INVALID_ATTRIBUTE_LENGTH
                BluetoothGatt.GATT_CONNECTION_CONGESTED -> GATT_CONNECTION_CONGESTED
                BluetoothGatt.GATT_FAILURE -> GATT_FAILURE
                UUBluetoothConstants.GATT_ERROR -> GATT_ERROR
                UUBluetoothConstants.GATT_DISCONNECTED_BY_PERIPHERAL -> GATT_DISCONNECTED_BY_PERIPHERAL
                else -> String.format(Locale.US, "Unknown-%d", gattStatus)
            }
        }
    }
}
