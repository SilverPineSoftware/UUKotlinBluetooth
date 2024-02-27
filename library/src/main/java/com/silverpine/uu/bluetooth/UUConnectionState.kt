package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothProfile

enum class UUConnectionState
{
    Connecting,
    Connected,
    Disconnecting,
    Disconnected,
    Undetermined;

    companion object
    {
        internal fun fromString(string: String?): UUConnectionState
        {
            for (s in values())
            {
                if (s.toString().equals(string, ignoreCase = true))
                {
                    return s
                }
            }

            return Undetermined
        }

        internal fun fromProfileConnectionState(state: Int): UUConnectionState
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