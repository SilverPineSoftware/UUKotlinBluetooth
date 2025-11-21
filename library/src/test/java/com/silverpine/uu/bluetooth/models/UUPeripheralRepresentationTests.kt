package com.silverpine.uu.bluetooth.models

import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.mockUUBluetoothContext
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class UUPeripheralRepresentationTests
{
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
    }

    @BeforeEach
    fun setup()
    {
        mockUUBluetoothContext()
        mockkObject(UUBluetooth)
        every { UUBluetooth.registerSpecName(any(), any()) } returns Unit
    }

    @Nested
    inner class PrimaryConstructorTests
    {
        @Test
        fun `primary constructor with all parameters`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            assertEquals("My Device", peripheral.name)
            assertEquals(1, peripheral.services?.size)
            assertEquals("180D", peripheral.services?.first()?.uuid)
        }

        @Test
        fun `primary constructor with minimal parameters`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "",
                services = null
            )

            assertEquals("", peripheral.name)
            assertNull(peripheral.services)
        }

        @Test
        fun `primary constructor with default values`()
        {
            val peripheral = UUPeripheralRepresentation()

            assertEquals("", peripheral.name)
            assertNull(peripheral.services)
        }
    }

    @Nested
    inner class SecondaryConstructorTests
    {
        @Test
        fun `secondary constructor from UUPeripheral with services`()
        {
            val peripheral = mock(UUPeripheral::class.java)
            `when`(peripheral.name).thenReturn("My Device")

            val serviceUuid = uuShortCodeToUuid("180D")
            val service = mock(android.bluetooth.BluetoothGattService::class.java)
            `when`(service.uuid).thenReturn(serviceUuid)
            `when`(service.type).thenReturn(android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY)
            `when`(service.includedServices).thenReturn(null)
            `when`(service.characteristics).thenReturn(null)

            `when`(peripheral.services).thenReturn(listOf(service))

            val representation = UUPeripheralRepresentation(peripheral)

            assertEquals("My Device", representation.name)
            assertEquals(1, representation.services?.size)
            assertEquals(serviceUuid.toString(), representation.services?.first()?.uuid)
        }

        @Test
        fun `secondary constructor from UUPeripheral with null services`()
        {
            val peripheral = mock(UUPeripheral::class.java)
            `when`(peripheral.name).thenReturn("My Device")
            `when`(peripheral.services).thenReturn(null)

            val representation = UUPeripheralRepresentation(peripheral)

            assertEquals("My Device", representation.name)
            assertNull(representation.services)
        }

        @Test
        fun `secondary constructor from UUPeripheral with empty services`()
        {
            val peripheral = mock(UUPeripheral::class.java)
            `when`(peripheral.name).thenReturn("My Device")
            `when`(peripheral.services).thenReturn(emptyList())

            val representation = UUPeripheralRepresentation(peripheral)

            assertEquals("My Device", representation.name)
            assertNull(representation.services)
        }
    }

    @Nested
    inner class SerializationTests
    {
        @Test
        fun `serialize peripheral with all fields`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            val jsonString = json.encodeToString(peripheral)
            assert(jsonString.contains("\"name\":\"My Device\""))
            assert(jsonString.contains("\"services\""))
            assert(jsonString.contains("\"uuid\":\"180D\""))
        }

        @Test
        fun `serialize peripheral with null services`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = null
            )

            val jsonString = json.encodeToString(peripheral)
            assert(jsonString.contains("\"name\":\"My Device\""))
            assert(jsonString.contains("\"services\":null"))
        }

        @Test
        fun `deserialize peripheral with all fields`()
        {
            val jsonString = """
            {
                "name": "My Device",
                "services": [
                    {
                        "uuid": "180D",
                        "name": "Heart Rate",
                        "isPrimary": true,
                        "includedServices": null,
                        "characteristics": null
                    }
                ]
            }
            """.trimIndent()

            val peripheral = json.decodeFromString<UUPeripheralRepresentation>(jsonString)

            assertEquals("My Device", peripheral.name)
            assertEquals(1, peripheral.services?.size)
            assertEquals("180D", peripheral.services?.first()?.uuid)
        }

        @Test
        fun `deserialize peripheral with minimal fields`()
        {
            val jsonString = """{"name":""}"""
            val peripheral = json.decodeFromString<UUPeripheralRepresentation>(jsonString)

            assertEquals("", peripheral.name)
            assertNull(peripheral.services)
        }

        @Test
        fun `deserialize peripheral without services field`()
        {
            val jsonString = """{"name":"My Device"}"""
            val peripheral = json.decodeFromString<UUPeripheralRepresentation>(jsonString)

            assertEquals("My Device", peripheral.name)
            assertNull(peripheral.services)
        }

        @Test
        fun `round trip serialization`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true
            )
            val original = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUPeripheralRepresentation>(jsonString)

            assertEquals(original.name, decoded.name)
            assertEquals(original.services?.size, decoded.services?.size)
            assertEquals(original.services?.first()?.uuid, decoded.services?.first()?.uuid)
        }

        @Test
        fun `round trip with multiple services`()
        {
            val service1 = UUServiceRepresentation("180D", "Heart Rate", true)
            val service2 = UUServiceRepresentation("180F", "Battery", true)
            val original = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service1, service2)
            )

            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<UUPeripheralRepresentation>(jsonString)

            assertEquals(2, decoded.services?.size)
            assertEquals("180D", decoded.services?.first()?.uuid)
            assertEquals("180F", decoded.services?.last()?.uuid)
        }
    }

    @Nested
    inner class RegisterCommonNamesTests
    {
        @Test
        fun `registerCommonNames with services`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate Service",
                isPrimary = true
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            peripheral.registerCommonNames()

            verify(exactly = 1) { UUBluetooth.registerSpecName("180D", "Heart Rate Service") }
        }

        @Test
        fun `registerCommonNames with null services does nothing`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = null
            )

            peripheral.registerCommonNames()

            verify(exactly = 0) { UUBluetooth.registerSpecName(any(), any()) }
        }

        @Test
        fun `registerCommonNames with empty services does nothing`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = emptyList()
            )

            peripheral.registerCommonNames()

            verify(exactly = 0) { UUBluetooth.registerSpecName(any(), any()) }
        }

        @Test
        fun `registerCommonNames with service without name does not register`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = null,
                isPrimary = true
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            peripheral.registerCommonNames()

            verify(exactly = 0) { UUBluetooth.registerSpecName(any(), any()) }
        }

        @Test
        fun `registerCommonNames with characteristics`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = null,
                descriptors = null
            )
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate Service",
                isPrimary = true,
                characteristics = listOf(characteristic)
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            peripheral.registerCommonNames()

            verify(exactly = 1) { UUBluetooth.registerSpecName("180D", "Heart Rate Service") }
            verify(exactly = 1) { UUBluetooth.registerSpecName("2A37", "Heart Rate Measurement") }
        }

        @Test
        fun `registerCommonNames with descriptors`()
        {
            val descriptor = UUDescriptorRepresentation(
                uuid = "2902",
                name = "Client Characteristic Configuration"
            )
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = null,
                descriptors = listOf(descriptor)
            )
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate Service",
                isPrimary = true,
                characteristics = listOf(characteristic)
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            peripheral.registerCommonNames()

            verify(exactly = 1) { UUBluetooth.registerSpecName("180D", "Heart Rate Service") }
            verify(exactly = 1) { UUBluetooth.registerSpecName("2A37", "Heart Rate Measurement") }
            verify(exactly = 1) { UUBluetooth.registerSpecName("2902", "Client Characteristic Configuration") }
        }

        @Test
        fun `registerCommonNames with multiple services`()
        {
            val service1 = UUServiceRepresentation("180D", "Heart Rate", true)
            val service2 = UUServiceRepresentation("180F", "Battery", true)
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service1, service2)
            )

            peripheral.registerCommonNames()

            verify(exactly = 1) { UUBluetooth.registerSpecName("180D", "Heart Rate") }
            verify(exactly = 1) { UUBluetooth.registerSpecName("180F", "Battery") }
        }

        @Test
        fun `registerCommonNames skips null names`()
        {
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = listOf(
                    UUCharacteristicRepresentation("2A37", null, null, null)
                )
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            peripheral.registerCommonNames()

            verify(exactly = 1) { UUBluetooth.registerSpecName("180D", "Heart Rate") }
            verify(exactly = 0) { UUBluetooth.registerSpecName("2A37", any()) }
        }
    }

    @Nested
    inner class DataClassEqualityTests
    {
        @Test
        fun `equals with same values`()
        {
            val service = UUServiceRepresentation("180D", "Heart Rate", true)
            val peripheral1 = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )
            val peripheral2 = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            assertEquals(peripheral1, peripheral2)
            assertEquals(peripheral1.hashCode(), peripheral2.hashCode())
        }

        @Test
        fun `equals with different names`()
        {
            val peripheral1 = UUPeripheralRepresentation(
                name = "Device 1",
                services = null
            )
            val peripheral2 = UUPeripheralRepresentation(
                name = "Device 2",
                services = null
            )

            assert(peripheral1 != peripheral2)
        }

        @Test
        fun `equals with different services`()
        {
            val service1 = UUServiceRepresentation("180D", "Heart Rate", true)
            val service2 = UUServiceRepresentation("180F", "Battery", true)

            val peripheral1 = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service1)
            )
            val peripheral2 = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service2)
            )

            assert(peripheral1 != peripheral2)
        }
    }

    @Nested
    inner class EdgeCaseTests
    {
        @Test
        fun `empty name string`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "",
                services = null
            )

            assertEquals("", peripheral.name)
        }

        @Test
        fun `empty services list`()
        {
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = emptyList()
            )

            assertEquals(emptyList<UUServiceRepresentation>(), peripheral.services)
        }

        @Test
        fun `multiple services`()
        {
            val service1 = UUServiceRepresentation("180D", "Heart Rate", true)
            val service2 = UUServiceRepresentation("180F", "Battery", true)
            val service3 = UUServiceRepresentation("1800", "Generic Access", true)

            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service1, service2, service3)
            )

            assertEquals(3, peripheral.services?.size)
        }

        @Test
        fun `nested service structure`()
        {
            val characteristic = UUCharacteristicRepresentation(
                uuid = "2A37",
                name = "Heart Rate Measurement",
                properties = listOf("Notify"),
                descriptors = listOf(
                    UUDescriptorRepresentation("2902", "Client Characteristic Configuration")
                )
            )
            val service = UUServiceRepresentation(
                uuid = "180D",
                name = "Heart Rate",
                isPrimary = true,
                characteristics = listOf(characteristic)
            )
            val peripheral = UUPeripheralRepresentation(
                name = "My Device",
                services = listOf(service)
            )

            assertEquals(1, peripheral.services?.size)
            assertEquals(1, peripheral.services?.first()?.characteristics?.size)
            assertEquals(1, peripheral.services?.first()?.characteristics?.first()?.descriptors?.size)
        }
    }
}

