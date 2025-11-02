package com.silverpine.uu.bluetooth

import com.silverpine.uu.bluetooth.extensions.uuIsBluetoothShortCode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class UUIDExtensionTests
{
    // uuIsBluetoothShortCode tests

    @Nested
    inner class IsBluetoothShortCodeTests
    {
        @Test
        fun uuIsBluetoothShortCode_validShortCodes_returnsTrue()
        {
            // Test with valid Bluetooth short code UUIDs using the helper function
            assertTrue(uuShortCodeToUuid("0000").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("0001").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("FFFF").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("FFE0").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("1800").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("2A37").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("ABCD").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("abcd").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("1234").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_caseInsensitiveHex_returnsTrue()
        {
            // Test that lowercase hex digits are accepted
            assertTrue(uuShortCodeToUuid("ff00").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("FF00").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("Ff00").uuIsBluetoothShortCode)
            assertTrue(uuShortCodeToUuid("fF00").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_explicitValidFormats_returnsTrue()
        {
            // Test explicit valid UUID strings
            assertTrue(uuUuidFromString("00000000-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertTrue(uuUuidFromString("0000FFFF-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertTrue(uuUuidFromString("0000ABCD-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertTrue(uuUuidFromString("0000abcd-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertTrue(uuUuidFromString("00001234-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_wrongPrefix_returnsFalse()
        {
            // Test UUIDs that don't start with 0000
            assertFalse(uuUuidFromString("10000000-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("FFFF0000-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("ABCD0000-0000-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_wrongMiddleSection_returnsFalse()
        {
            // Test UUIDs with wrong middle sections
            assertFalse(uuUuidFromString("00000000-0001-1000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("00000000-0000-2000-8000-00805F9B34FB").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("00000000-0000-1000-9000-00805F9B34FB").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_wrongSuffix_returnsFalse()
        {
            // Test UUIDs with wrong suffix
            assertFalse(uuUuidFromString("00000000-0000-1000-8000-00805F9B34FC").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("00000000-0000-1000-8000-00805F9B34FA").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("00000000-0000-1000-8000-10805F9B34FB").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_randomUuid_returnsFalse()
        {
            // Test with randomly generated UUIDs
            assertFalse(UUID.randomUUID().uuIsBluetoothShortCode)
            assertFalse(UUID.randomUUID().uuIsBluetoothShortCode)
            assertFalse(UUID.randomUUID().uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_fullCustomUuid_returnsFalse()
        {
            // Test with custom UUIDs that don't match the pattern
            assertFalse(uuUuidFromString("F000FFC0-0451-4000-B000-000000000000").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("12345678-1234-1234-1234-123456789012").uuIsBluetoothShortCode)
            assertFalse(uuUuidFromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF").uuIsBluetoothShortCode)
        }

        @Test
        fun uuIsBluetoothShortCode_standardBluetoothServices_returnsTrue()
        {
            // Test with real Bluetooth service UUIDs from the constants
            assertTrue(uuShortCodeToUuid("1800").uuIsBluetoothShortCode) // Generic Access
            assertTrue(uuShortCodeToUuid("1801").uuIsBluetoothShortCode) // Generic Attribute
            assertTrue(uuShortCodeToUuid("180D").uuIsBluetoothShortCode) // Heart Rate
            assertTrue(uuShortCodeToUuid("180F").uuIsBluetoothShortCode) // Battery Service
            assertTrue(uuShortCodeToUuid("1811").uuIsBluetoothShortCode) // Alert Notification Service
        }

        @Test
        fun uuIsBluetoothShortCode_boundaryValues_returnsCorrect()
        {
            // Test boundary values
            assertTrue(uuShortCodeToUuid("0000").uuIsBluetoothShortCode) // Minimum
            assertTrue(uuShortCodeToUuid("FFFF").uuIsBluetoothShortCode) // Maximum
            assertTrue(uuShortCodeToUuid("0001").uuIsBluetoothShortCode) // Minimum + 1
            assertTrue(uuShortCodeToUuid("FFFE").uuIsBluetoothShortCode) // Maximum - 1
        }
    }
}
