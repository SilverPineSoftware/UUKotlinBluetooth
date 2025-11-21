package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.bluetooth.mockDescriptor
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class UUDescriptorRepresentationTests
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

            assertEquals("2902", descriptor.uuid)
            assertEquals("Client Characteristic Configuration", descriptor.name)
        }

        @Test
        fun `primary constructor with uuid only`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )

            assertEquals("2902", descriptor.uuid)
            assertNull(descriptor.name)
        }

        @Test
        fun `primary constructor with default values`()
        {
            val descriptor = UUDescriptorRepresentation()

            assertEquals("", descriptor.uuid)
            assertNull(descriptor.name)
        }
    }

    @Nested
    inner class SecondaryConstructorTests
    {
        @Test
        fun `secondary constructor from BluetoothGattDescriptor`()
        {
            val uuid = uuShortCodeToUuid("2902")
            val descriptor = mockDescriptor(uuid = uuid)
            val representation = UUDescriptorRepresentation(descriptor)

            assertEquals(uuid.toString(), representation.uuid)
            // Name will be the common name from the UUID
            assertEquals(descriptor.uuid.toString(), representation.uuid)
        }

        @Test
        fun `secondary constructor with random UUID`()
        {
            val uuid = UUID.randomUUID()
            val descriptor = mockDescriptor(uuid = uuid)
            val representation = UUDescriptorRepresentation(descriptor)

            assertEquals(uuid.toString(), representation.uuid)
        }
    }

    @Nested
    inner class SerializationTests
    {
        @Test
        fun `serialize descriptor with all fields`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )

            val jsonString = json.encodeToString(descriptor)
            assert(jsonString.contains("\"uuid\":\"2902\""))
            assert(jsonString.contains("\"name\":\"Client Characteristic Configuration\""))
        }

        @Test
        fun `serialize descriptor with null name`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )

            val jsonString = json.encodeToString(descriptor)
            assert(jsonString.contains("\"uuid\":\"2902\""))
            assert(jsonString.contains("\"name\":null"))
        }

        @Test
        fun `deserialize descriptor with all fields`()
        {
            val jsonString = """{"uuid":"2902","name":"Client Characteristic Configuration"}"""
            val descriptor = json.decodeFromString<UUDescriptorRepresentation>(jsonString)

            assertEquals("2902", descriptor.uuid)
            assertEquals("Client Characteristic Configuration", descriptor.name)
        }

        @Test
        fun `deserialize descriptor with null name`()
        {
            val jsonString = """{"uuid":"2902","name":null}"""
            val descriptor = json.decodeFromString<UUDescriptorRepresentation>(jsonString)

            assertEquals("2902", descriptor.uuid)
            assertNull(descriptor.name)
        }

        @Test
        fun `deserialize descriptor without name field`()
        {
            val jsonString = """{"uuid":"2902"}"""
            val descriptor = json.decodeFromString<UUDescriptorRepresentation>(jsonString)

            assertEquals("2902", descriptor.uuid)
            assertNull(descriptor.name)
        }

        @Test
        fun `round trip serialization`()
        {
            val original = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUDescriptorRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(original.name, decoded.name)
        }

        @Test
        fun `round trip with null name`()
        {
            val original = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUDescriptorRepresentation>(jsonString)

            assertEquals(original.uuid, decoded.uuid)
            assertEquals(original.name, decoded.name)
        }
    }

    @Nested
    inner class DataClassEqualityTests
    {
        @Test
        fun `equals with same values`()
        {
            val desc1 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val desc2 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )

            assertEquals(desc1, desc2)
            assertEquals(desc1.hashCode(), desc2.hashCode())
        }

        @Test
        fun `equals with different names`()
        {
            val desc1 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Name 1"
            )
            val desc2 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Name 2"
            )

            assert(desc1 != desc2)
        }

        @Test
        fun `equals with different UUIDs`()
        {
            val desc1 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val desc2 = UUDescriptorRepresentation(
                uuid = "2900",
                name = "Client Characteristic Configuration"
            )

            assert(desc1 != desc2)
        }

        @Test
        fun `equals with one null name`()
        {
            val desc1 = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val desc2 = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )

            assert(desc1 != desc2)
        }

        @Test
        fun `equals with both null names`()
        {
            val desc1 = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )
            val desc2 = UUDescriptorRepresentation(
                uuid = "2902",
                name = null
            )

            assertEquals(desc1, desc2)
        }
    }

    @Nested
    inner class EdgeCaseTests
    {
        @Test
        fun `empty UUID string`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "",
                name = "Test"
            )

            assertEquals("", descriptor.uuid)
            assertEquals("Test", descriptor.name)
        }

        @Test
        fun `long UUID string`()
        {
            val longUuid = "00002902-0000-1000-8000-00805F9B34FB"
            val descriptor = UUDescriptorRepresentation(
                uuid = longUuid,
                name = "Test"
            )

            assertEquals(longUuid, descriptor.uuid)
        }

        @Test
        fun `empty name string`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = ""
            )

            assertEquals("2902", descriptor.uuid)
            assertEquals("", descriptor.name)
        }
    }
}

