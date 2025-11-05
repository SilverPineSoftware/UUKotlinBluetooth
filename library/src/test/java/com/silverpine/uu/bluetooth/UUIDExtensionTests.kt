package com.silverpine.uu.bluetooth

import com.silverpine.uu.bluetooth.extensions.uuBluetoothShortCode
import com.silverpine.uu.bluetooth.extensions.uuIsBluetoothShortCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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

    @Nested
    inner class UUBluetoothShortCodeTests
    {
        @Test
        fun uuBluetoothShortCode_validShortCodes_returnsCorrectCode()
        {
            // Test with valid short codes
            assertEquals("0000", uuShortCodeToUuid("0000").uuBluetoothShortCode)
            assertEquals("0001", uuShortCodeToUuid("0001").uuBluetoothShortCode)
            assertEquals("FFFF", uuShortCodeToUuid("FFFF").uuBluetoothShortCode)
            assertEquals("FFE0", uuShortCodeToUuid("FFE0").uuBluetoothShortCode)
            assertEquals("1800", uuShortCodeToUuid("1800").uuBluetoothShortCode)
            assertEquals("2A37", uuShortCodeToUuid("2A37").uuBluetoothShortCode)
            assertEquals("ABCD", uuShortCodeToUuid("ABCD").uuBluetoothShortCode)
            assertEquals("1234", uuShortCodeToUuid("1234").uuBluetoothShortCode)
        }

        @Test
        fun uuBluetoothShortCode_caseInsensitiveInput_returnsUppercase()
        {
            // Test that lowercase input is converted to uppercase
            assertEquals("FF00", uuShortCodeToUuid("ff00").uuBluetoothShortCode)
            assertEquals("FF00", uuShortCodeToUuid("FF00").uuBluetoothShortCode)
            assertEquals("FF00", uuShortCodeToUuid("Ff00").uuBluetoothShortCode)
            assertEquals("FF00", uuShortCodeToUuid("fF00").uuBluetoothShortCode)
            assertEquals("ABCD", uuShortCodeToUuid("abcd").uuBluetoothShortCode)
            assertEquals("ABCD", uuShortCodeToUuid("AbCd").uuBluetoothShortCode)
        }

        @Test
        fun uuBluetoothShortCode_explicitValidFormats_returnsCorrectCode()
        {
            // Test explicit UUID strings
            assertEquals("0000", uuUuidFromString("00000000-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertEquals("FFFF", uuUuidFromString("0000FFFF-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertEquals("ABCD", uuUuidFromString("0000ABCD-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertEquals("ABCD", uuUuidFromString("0000abcd-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertEquals("1234", uuUuidFromString("00001234-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
        }

        @Test
        fun uuBluetoothShortCode_invalidUuid_returnsNull()
        {
            // Test that invalid UUIDs return null
            assertNull(uuUuidFromString("10000000-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(uuUuidFromString("FFFF0000-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(UUID.randomUUID().uuBluetoothShortCode)
            assertNull(uuUuidFromString("F000FFC0-0451-4000-B000-000000000000").uuBluetoothShortCode)
            assertNull(uuUuidFromString("12345678-1234-1234-1234-123456789012").uuBluetoothShortCode)
        }

        @Test
        fun uuBluetoothShortCode_standardBluetoothServices_returnsCorrectCode()
        {
            // Test with real Bluetooth service UUIDs
            assertEquals("1800", uuShortCodeToUuid("1800").uuBluetoothShortCode) // Generic Access
            assertEquals("1801", uuShortCodeToUuid("1801").uuBluetoothShortCode) // Generic Attribute
            assertEquals("180D", uuShortCodeToUuid("180D").uuBluetoothShortCode) // Heart Rate
            assertEquals("180F", uuShortCodeToUuid("180F").uuBluetoothShortCode) // Battery Service
            assertEquals("1811", uuShortCodeToUuid("1811").uuBluetoothShortCode) // Alert Notification Service
            assertEquals("2A37", uuShortCodeToUuid("2A37").uuBluetoothShortCode) // Heart Rate Measurement
        }

        @Test
        fun uuBluetoothShortCode_boundaryValues_returnsCorrectCode()
        {
            // Test boundary values
            assertEquals("0000", uuShortCodeToUuid("0000").uuBluetoothShortCode) // Minimum
            assertEquals("FFFF", uuShortCodeToUuid("FFFF").uuBluetoothShortCode) // Maximum
            assertEquals("0001", uuShortCodeToUuid("0001").uuBluetoothShortCode) // Minimum + 1
            assertEquals("FFFE", uuShortCodeToUuid("FFFE").uuBluetoothShortCode) // Maximum - 1
        }

        @Test
        fun uuBluetoothShortCode_wrongPrefix_returnsNull()
        {
            // Test UUIDs that don't start with 0000
            assertNull(uuUuidFromString("10000000-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(uuUuidFromString("FFFF0000-0000-1000-8000-00805F9B34FB").uuBluetoothShortCode)
        }

        @Test
        fun uuBluetoothShortCode_wrongPattern_returnsNull()
        {
            // Test UUIDs with wrong middle sections or suffix
            assertNull(uuUuidFromString("00000000-0001-1000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(uuUuidFromString("00000000-0000-2000-8000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(uuUuidFromString("00000000-0000-1000-9000-00805F9B34FB").uuBluetoothShortCode)
            assertNull(uuUuidFromString("00000000-0000-1000-8000-00805F9B34FC").uuBluetoothShortCode)
        }
    }
}
