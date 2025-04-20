package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothProfile

enum class UUPeripheralConnectionState
{
    Connecting,
    Connected,
    Disconnecting,
    Disconnected,
    Undetermined;

    companion object
    {
        fun fromString(string: String?): UUPeripheralConnectionState
        {
            for (s in entries)
            {
                if (s.toString().equals(string, ignoreCase = true))
                {
                    return s
                }
            }

            return Disconnected
        }

        fun fromProfileConnectionState(state: Int): UUPeripheralConnectionState
        {
            when (state)
            {
                BluetoothProfile.STATE_CONNECTED -> return Connected
                BluetoothProfile.STATE_CONNECTING -> return Connecting
                BluetoothProfile.STATE_DISCONNECTING -> return Disconnecting
                BluetoothProfile.STATE_DISCONNECTED -> return Disconnected
            }

            return Undetermined
        }
    }
}