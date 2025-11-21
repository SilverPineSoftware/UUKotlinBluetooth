package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattCharacteristic
import com.silverpine.uu.bluetooth.mockCharacteristic
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import java.util.UUID

class UUCharacteristicRepresentationTests
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
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = listOf("Notify", "Read"),
                descriptors = listOf(descriptor)
            )

            assertEquals("2A37", characteristic.uuid)
            assertEquals("Heart Rate Measurement", characteristic.name)
            assertEquals(listOf("Notify", "Read"), characteristic.properties)
            assertEquals(1, characteristic.descriptors?.size)
            assertEquals("2902", characteristic.descriptors?.first()?.uuid)
        }

        @Test
        fun `primary constructor with minimal parameters`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = null,
                properties = null,
                descriptors = null
            )

            assertEquals("2A37", characteristic.uuid)
            assertNull(characteristic.name)
            assertNull(characteristic.properties)
            assertNull(characteristic.descriptors)
        }

        @Test
        fun `primary constructor with default values`()
        {
            val characteristic = UUCharacteristicRepresentation()

            assertEquals("", characteristic.uuid)
            assertNull(characteristic.name)
            assertNull(characteristic.properties)
            assertNull(characteristic.descriptors)
        }
    }

    @Nested
    inner class SecondaryConstructorTests
    {
        @Test
        fun `secondary constructor from BluetoothGattCharacteristic`()
        {
            val uuid = uuShortCodeToUuid("2A37")
            val characteristic = mockCharacteristic(uuid = uuid)
            `when`(characteristic.properties).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            `when`(characteristic.descriptors).thenReturn(null)

            val representation = UUCharacteristicRepresentation(characteristic)

            assertEquals(uuid.toString(), representation.uuid)
            // Properties should be parsed from the bitmask
            assert(representation.properties != null)
        }

        @Test
        fun `secondary constructor with empty descriptors`()
        {
            val uuid = uuShortCodeToUuid("2A37")
            val characteristic = mockCharacteristic(uuid = uuid)
            `when`(characteristic.properties).thenReturn(BluetoothGattCharacteristic.PROPERTY_READ)
            `when`(characteristic.descriptors).thenReturn(emptyList())

            val representation = UUCharacteristicRepresentation(characteristic)

            assertEquals(uuid.toString(), representation.uuid)
            assertNull(representation.descriptors)
        }
    }

    @Nested
    inner class SerializationTests
    {
        @Test
        fun `serialize characteristic with all fields`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = listOf("Notify"),
                descriptors = listOf(descriptor)
            )

            val jsonString = json.encodeToString(characteristic)
            assert(jsonString.contains("\"uuid\":\"2A37\""))
            assert(jsonString.contains("\"name\":\"Heart Rate Measurement\""))
            assert(jsonString.contains("\"properties\":[\"Notify\"]"))
            assert(jsonString.contains("\"descriptors\""))
        }

        @Test
        fun `serialize characteristic with null optional fields`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = null,
                properties = null,
                descriptors = null
            )

            val jsonString = json.encodeToString(characteristic)
            assert(jsonString.contains("\"uuid\":\"2A37\""))
            assert(jsonString.contains("\"name\":null"))
            assert(jsonString.contains("\"properties\":null"))
            assert(jsonString.contains("\"descriptors\":null"))
        }

        @Test
        fun `deserialize characteristic with all fields`()
        {
            val jsonString = """
            {
                "uuid": "2A37",
                "name": "Heart Rate Measurement",
                "properties": ["Notify", "Read"],
                "descriptors": [
                    {
                        "uuid": "2902",
                        "name": "Client Characteristic Configuration"
                    }
                ]
            }
            """.trimIndent()

            val characteristic = json.decodeFromString<UUCharacteristicRepresentation>(jsonString)

            assertEquals("2A37", characteristic.uuid)
            assertEquals("Heart Rate Measurement", characteristic.name)
            assertEquals(listOf("Notify", "Read"), characteristic.properties)
            assertEquals(1, characteristic.descriptors?.size)
            assertEquals("2902", characteristic.descriptors?.first()?.uuid)
        }

        @Test
        fun `deserialize characteristic with minimal fields`()
        {
            val jsonString = """{"uuid":"2A37"}"""
            val characteristic = json.decodeFromString<UUCharacteristicRepresentation>(jsonString)

            assertEquals("2A37", characteristic.uuid)
            assertNull(characteristic.name)
            assertNull(characteristic.properties)
            assertNull(characteristic.descriptors)
        }

        @Test
        fun `round trip serialization`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val original = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = listOf("Notify", "Read"),
                descriptors = listOf(descriptor)
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUCharacteristicRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(original.name, decoded.name)
            assertEquals(original.properties, decoded.properties)
            assertEquals(original.descriptors?.size, decoded.descriptors?.size)
            assertEquals(original.descriptors?.first()?.uuid, decoded.descriptors?.first()?.uuid)
        }

        @Test
        fun `round trip with empty properties list`()
        {
            val original = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Test",
                properties = emptyList(),
                descriptors = null
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUCharacteristicRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(original.name, decoded.name)
            assertEquals(emptyList<String>(), decoded.properties)
        }
    }

    @Nested
    inner class DataClassEqualityTests
    {
        @Test
        fun `equals with same values`()
        {
            val char1 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Notify"),
                descriptors = null
            )
            val char2 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Notify"),
                descriptors = null
            )

            assertEquals(char1, char2)
            assertEquals(char1.hashCode(), char2.hashCode())
        }

        @Test
        fun `equals with different properties`()
        {
            val char1 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Notify"),
                descriptors = null
            )
            val char2 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Read"),
                descriptors = null
            )

            assert(char1 != char2)
        }

        @Test
        fun `equals with different descriptors`()
        {
            val desc1 = UUDescriptorRepresentation("2902", "Desc 1")
            val desc2 = UUDescriptorRepresentation("2900", "Desc 2")

            val char1 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Notify"),
                descriptors = listOf(desc1)
            )
            val char2 = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate",
                properties = listOf("Notify"),
                descriptors = listOf(desc2)
            )

            assert(char1 != char2)
        }
    }

    @Nested
    inner class EdgeCaseTests
    {
        @Test
        fun `empty properties list`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Test",
                properties = emptyList(),
                descriptors = null
            )

            assertEquals(emptyList<String>(), characteristic.properties)
        }

        @Test
        fun `multiple properties`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Test",
                properties = listOf("Read", "Write", "Notify", "Indicate"),
                descriptors = null
            )

            assertEquals(4, characteristic.properties?.size)
            assert(characteristic.properties?.contains("Read") == true)
            assert(characteristic.properties?.contains("Write") == true)
        }

        @Test
        fun `multiple descriptors`()
        {
            val desc1 = UUDescriptorRepresentation("2902", "Desc 1")
            val desc2 = UUDescriptorRepresentation("2900", "Desc 2")
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Test",
                properties = null,
                descriptors = listOf(desc1, desc2)
            )

            assertEquals(2, characteristic.descriptors?.size)
        }
    }
}

