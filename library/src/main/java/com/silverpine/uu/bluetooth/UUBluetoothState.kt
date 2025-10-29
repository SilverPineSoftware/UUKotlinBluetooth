package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothAdapter

/**
 * Bluetooth adapter power states.
 */
enum class UUBluetoothState
{
    /**
     * The Bluetooth adapter state is unknown.
     */
    UNKNOWN,

    /**
     * The Bluetooth adapter is on and ready for use.
     */
    ON,

    /**
     * The Bluetooth adapter is off.
     */
    OFF,

    /**
     * The Bluetooth adapter is currently turning on.
     */
    TURNING_ON,

    /**
     * The Bluetooth adapter is currently turning off.
     */
    TURNING_OFF;

    companion object
    {
        /**
         * Converts an Android BluetoothAdapter state integer to a UUBluetoothState enum value.
         *
         * @param state a BluetoothAdapter.STATE_* constant
         * @return the corresponding UUBluetoothState, or UNKNOWN if the state is not recognized
         */
        fun fromBluetoothState(state: Int): UUBluetoothState
        {
            return when (state)
            {
                BluetoothAdapter.STATE_ON -> ON
                BluetoothAdapter.STATE_OFF -> OFF
                BluetoothAdapter.STATE_TURNING_ON -> TURNING_ON
                BluetoothAdapter.STATE_TURNING_OFF -> TURNING_OFF
                else -> UNKNOWN
            }
        }
    }
}