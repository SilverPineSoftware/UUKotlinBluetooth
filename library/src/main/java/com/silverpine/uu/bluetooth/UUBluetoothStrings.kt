package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import com.silverpine.uu.core.uuIsBitSet

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
}
