package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.mockService
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import java.util.UUID

class UUServiceRepresentationTests
{
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
    }

    @Nested
    inner class PrimaryConstructorTests
    {
        @Test
        fun `primary constructor with all parameters`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement"
            )
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                includedServices = null,
                characteristics = listOf(characteristic)
            )

            assertEquals("180D", service.uuid)
            assertEquals("Heart Rate", service.name)
            assertTrue(service.isPrimary == true)
            assertNull(service.includedServices)
            assertEquals(1, service.characteristics?.size)
        }

        @Test
        fun `primary constructor with minimal parameters`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = null,
                isPrimary = null,
                includedServices = null,
                characteristics = null
            )

            assertEquals("180D", service.uuid)
            assertNull(service.name)
            assertNull(service.isPrimary)
            assertNull(service.includedServices)
            assertNull(service.characteristics)
        }

        @Test
        fun `primary constructor with default values`()
        {
            val service = UUServiceRepresentation()

            assertEquals("", service.uuid)
            assertNull(service.name)
            assertNull(service.isPrimary)
            assertNull(service.includedServices)
            assertNull(service.characteristics)
        }

        @Test
        fun `primary constructor with secondary service`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = false
            )

            assertFalse(service.isPrimary == true)
        }
    }

    @Nested
    inner class SecondaryConstructorTests
    {
        @Test
        fun `secondary constructor from BluetoothGattService primary`()
        {
            val uuid = uuShortCodeToUuid("180D")
            val service = mockService(uuid = uuid, type = BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val representation = UUServiceRepresentation(service)

            assertEquals(uuid.toString(), representation.uuid)
            assertTrue(representation.isPrimary == true)
        }

        @Test
        fun `secondary constructor from BluetoothGattService secondary`()
        {
            val uuid = uuShortCodeToUuid("180D")
            val service = mockService(uuid = uuid, type = BluetoothGattService.SERVICE_TYPE_SECONDARY)

            val representation = UUServiceRepresentation(service)

            assertEquals(uuid.toString(), representation.uuid)
            assertFalse(representation.isPrimary == true)
        }

        @Test
        fun `secondary constructor with empty characteristics`()
        {
            val uuid = uuShortCodeToUuid("180D")
            val service = mockService(uuid = uuid, type = BluetoothGattService.SERVICE_TYPE_PRIMARY)
            `when`(service.characteristics).thenReturn(emptyList())

            val representation = UUServiceRepresentation(service)

            assertNull(representation.characteristics)
        }

        @Test
        fun `secondary constructor with included services`()
        {
            val uuid1 = uuShortCodeToUuid("180D")
            val uuid2 = uuShortCodeToUuid("180F")
            val service2 = mockService(uuid = uuid2, type = BluetoothGattService.SERVICE_TYPE_SECONDARY)

            val mainService = mockService(uuid = uuid1, type = BluetoothGattService.SERVICE_TYPE_PRIMARY)
            `when`(mainService.includedServices).thenReturn(listOf(service2))

            val representation = UUServiceRepresentation(mainService)

            assertEquals(1, representation.includedServices?.size)
            assertEquals(uuid2.toString(), representation.includedServices?.first()?.uuid)
        }
    }

    @Nested
    inner class SerializationTests
    {
        @Test
        fun `serialize service with all fields`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement"
            )
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                includedServices = null,
                characteristics = listOf(characteristic)
            )

            val jsonString = json.encodeToString(service)
            assert(jsonString.contains("\"uuid\":\"180D\""))
            assert(jsonString.contains("\"name\":\"Heart Rate\""))
            assert(jsonString.contains("\"isPrimary\":true"))
            assert(jsonString.contains("\"characteristics\""))
        }

        @Test
        fun `serialize service with null optional fields`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = null,
                isPrimary = null,
                includedServices = null,
                characteristics = null
            )

            val jsonString = json.encodeToString(service)
            assert(jsonString.contains("\"uuid\":\"180D\""))
            assert(jsonString.contains("\"name\":null"))
            assert(jsonString.contains("\"isPrimary\":null"))
        }

        @Test
        fun `deserialize service with all fields`()
        {
            val jsonString = """
            {
                "uuid": "180D",
                "name": "Heart Rate",
                "isPrimary": true,
                "includedServices": [],
                "characteristics": [
                    {
                        "uuid": "2A37",
                        "name": "Heart Rate Measurement",
                        "properties": ["Notify"],
                        "descriptors": null
                    }
                ]
            }
            """.trimIndent()

            val service = json.decodeFromString<UUServiceRepresentation>(jsonString)

            assertEquals("180D", service.uuid)
            assertEquals("Heart Rate", service.name)
            assertTrue(service.isPrimary == true)
            assertEquals(1, service.characteristics?.size)
            assertEquals("2A37", service.characteristics?.first()?.uuid)
        }

        @Test
        fun `deserialize service with minimal fields`()
        {
            val jsonString = """{"uuid":"180D"}"""
            val service = json.decodeFromString<UUServiceRepresentation>(jsonString)

            assertEquals("180D", service.uuid)
            assertNull(service.name)
            assertNull(service.isPrimary)
            assertNull(service.includedServices)
            assertNull(service.characteristics)
        }

        @Test
        fun `deserialize service with secondary type`()
        {
            val jsonString = """
            {
                "uuid": "180D",
                "name": "Heart Rate",
                "isPrimary": false
            }
            """.trimIndent()

            val service = json.decodeFromString<UUServiceRepresentation>(jsonString)

            assertFalse(service.isPrimary == true)
        }

        @Test
        fun `round trip serialization`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = listOf("Notify")
            )
            val original = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                includedServices = null,
                characteristics = listOf(characteristic)
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUServiceRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(original.name, decoded.name)
            assertEquals(original.isPrimary, decoded.isPrimary)
            assertEquals(original.characteristics?.size, decoded.characteristics?.size)
        }

        @Test
        fun `round trip with included services`()
        {
            val includedService = UUServiceRepresentation(
                uuid = "180F",
                name = "Battery Service",
                isPrimary = false
            )
            val original = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                includedServices = listOf(includedService),
                characteristics = null
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUServiceRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(1, decoded.includedServices?.size)
            assertEquals("180F", decoded.includedServices?.first()?.uuid)
        }
    }

    @Nested
    inner class DataClassEqualityTests
    {
        @Test
        fun `equals with same values`()
        {
            val service1 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )
            val service2 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )

            assertEquals(service1, service2)
            assertEquals(service1.hashCode(), service2.hashCode())
        }

        @Test
        fun `equals with different isPrimary`()
        {
            val service1 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )
            val service2 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = false
            )

            assert(service1 != service2)
        }

        @Test
        fun `equals with different characteristics`()
        {
            val char1 = UUCharacteristicRepresentation("2A37", "Char 1")
            val char2 = UUCharacteristicRepresentation("2A38", "Char 2")

            val service1 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = listOf(char1)
            )
            val service2 = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = listOf(char2)
            )

            assert(service1 != service2)
        }
    }

    @Nested
    inner class EdgeCaseTests
    {
        @Test
        fun `empty characteristics list`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = emptyList()
            )

            assertEquals(emptyList<UUCharacteristicRepresentation>(), service.characteristics)
        }

        @Test
        fun `multiple characteristics`()
        {
            val char1 = UUCharacteristicRepresentation("2A37", "Char 1")
            val char2 = UUCharacteristicRepresentation("2A38", "Char 2")
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = listOf(char1, char2)
            )

            assertEquals(2, service.characteristics?.size)
        }

        @Test
        fun `nested included services`()
        {
            val nestedService = UUServiceRepresentation(
                uuid = "180F",
                name = "Battery",
                isPrimary = false
            )
            val mainService = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                includedServices = listOf(nestedService)
            )

            assertEquals(1, mainService.includedServices?.size)
            assertEquals("180F", mainService.includedServices?.first()?.uuid)
        }
    }
}

