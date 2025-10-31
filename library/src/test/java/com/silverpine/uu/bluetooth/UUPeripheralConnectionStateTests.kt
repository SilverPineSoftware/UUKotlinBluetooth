package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UUPeripheralConnectionStateTests
{
    // fromString tests

    @Test
    fun fromString_validEnumNames_returnsCorrectState()
    {
        assertEquals(UUPeripheralConnectionState.CONNECTING, UUPeripheralConnectionState.fromString("CONNECTING"))
        assertEquals(UUPeripheralConnectionState.CONNECTED, UUPeripheralConnectionState.fromString("CONNECTED"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTING, UUPeripheralConnectionState.fromString("DISCONNECTING"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString("DISCONNECTED"))
        assertEquals(UUPeripheralConnectionState.UNDETERMINED, UUPeripheralConnectionState.fromString("UNDETERMINED"))
    }

    @Test
    fun fromString_caseInsensitive_returnsCorrectState()
    {
        assertEquals(UUPeripheralConnectionState.CONNECTING, UUPeripheralConnectionState.fromString("connecting"))
        assertEquals(UUPeripheralConnectionState.CONNECTED, UUPeripheralConnectionState.fromString("Connected"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTING, UUPeripheralConnectionState.fromString("Disconnecting"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString("disconnected"))
        assertEquals(UUPeripheralConnectionState.UNDETERMINED, UUPeripheralConnectionState.fromString("UnDeTeRmInEd"))
    }

    @Test
    fun fromString_null_returnsDisconnected()
    {
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString(null))
    }

    @Test
    fun fromString_invalidString_returnsDisconnected()
    {
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString("INVALID"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString(""))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString("CONNECT"))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromString(" "))
    }

    // fromProfileConnectionState tests

    @Test
    fun fromProfileConnectionState_validStates_returnsCorrectState()
    {
        assertEquals(UUPeripheralConnectionState.CONNECTING, UUPeripheralConnectionState.fromProfileConnectionState(BluetoothProfile.STATE_CONNECTING))
        assertEquals(UUPeripheralConnectionState.CONNECTED, UUPeripheralConnectionState.fromProfileConnectionState(BluetoothProfile.STATE_CONNECTED))
        assertEquals(UUPeripheralConnectionState.DISCONNECTING, UUPeripheralConnectionState.fromProfileConnectionState(BluetoothProfile.STATE_DISCONNECTING))
        assertEquals(UUPeripheralConnectionState.DISCONNECTED, UUPeripheralConnectionState.fromProfileConnectionState(BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test
    fun fromProfileConnectionState_invalidState_returnsUndetermined()
    {
        assertEquals(UUPeripheralConnectionState.UNDETERMINED, UUPeripheralConnectionState.fromProfileConnectionState(-1))
        assertEquals(UUPeripheralConnectionState.UNDETERMINED, UUPeripheralConnectionState.fromProfileConnectionState(999))
        assertEquals(UUPeripheralConnectionState.UNDETERMINED, UUPeripheralConnectionState.fromProfileConnectionState(5))
    }

    @Test
    fun fromProfileConnectionState_allValidStates_coverAllEnumValues()
    {
        // Verify all enum values can be created from valid profile states
        val validStates = mapOf(
            BluetoothProfile.STATE_CONNECTING to UUPeripheralConnectionState.CONNECTING,
            BluetoothProfile.STATE_CONNECTED to UUPeripheralConnectionState.CONNECTED,
            BluetoothProfile.STATE_DISCONNECTING to UUPeripheralConnectionState.DISCONNECTING,
            BluetoothProfile.STATE_DISCONNECTED to UUPeripheralConnectionState.DISCONNECTED
        )

        validStates.forEach { (profileState, expectedState) ->
            assertEquals(expectedState, UUPeripheralConnectionState.fromProfileConnectionState(profileState))
        }
    }
}