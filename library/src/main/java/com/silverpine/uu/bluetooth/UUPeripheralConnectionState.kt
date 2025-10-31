package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothProfile

/**
 * Bluetooth peripheral connection states. This enum mimics the Android BluetoothProfile.STATE_* constants.
 */
enum class UUPeripheralConnectionState
{
    /**
     * The peripheral is currently connecting.
     */
    CONNECTING,

    /**
     * The peripheral is connected.
     */
    CONNECTED,

    /**
     * The peripheral is currently disconnecting.
     */
    DISCONNECTING,

    /**
     * The peripheral is disconnected.
     */
    DISCONNECTED,

    /**
     * The peripheral connection state is undetermined.
     */
    UNDETERMINED;

    companion object
    {
        /**
         * Converts a string representation of the connection state to a UUPeripheralConnectionState enum value.
         *
         * @param string a string representation of the connection state (case-insensitive)
         * @return the corresponding UUPeripheralConnectionState, or DISCONNECTED if the string is null or not recognized
         */
        fun fromString(string: String?): UUPeripheralConnectionState
        {
            return entries.firstOrNull { it.name.equals(string, ignoreCase = true) } ?: DISCONNECTED
        }

        /**
         * Converts an Android BluetoothProfile connection state integer to a UUPeripheralConnectionState enum value.
         *
         * @param state a BluetoothProfile.STATE_* constant
         * @return the corresponding UUPeripheralConnectionState, or UNDETERMINED if the state is not recognized
         */
        fun fromProfileConnectionState(state: Int): UUPeripheralConnectionState
        {
            when (state)
            {
                BluetoothProfile.STATE_CONNECTED -> return CONNECTED
                BluetoothProfile.STATE_CONNECTING -> return CONNECTING
                BluetoothProfile.STATE_DISCONNECTING -> return DISCONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> return DISCONNECTED
            }

            return UNDETERMINED
        }
    }
}