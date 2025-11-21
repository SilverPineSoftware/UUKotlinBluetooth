package com.silverpine.uu.bluetooth.models

import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.UUJson
import kotlinx.serialization.Serializable

/**
 * A representation of a Bluetooth peripheral.
 *
 * `UUPeripheralRepresentation` models a Bluetooth peripheral, providing access to its associated services.
 * This class is designed to work with Android Bluetooth and facilitate encoding and decoding as JSON
 * for storage or communication.
 *
 * ## Features
 * - Conforms to [Serializable] for seamless JSON serialization and deserialization.
 * - Supports initialization from Android's [UUPeripheral].
 * - Provides a method to register common names for services, characteristics, and descriptors.
 *
 * ## Properties
 * - [name]: An optional string providing a human-readable name for the peripheral.
 * - [services]: An optional array of [UUServiceRepresentation] objects representing the services
 *   available on the peripheral.
 *
 * ## Initializers
 * - Primary constructor: Creates a new, empty peripheral representation.
 * - Secondary constructor: Convenience constructor to create a peripheral representation from a
 *   [UUPeripheral] object.
 *
 * ## Methods
 * - [registerCommonNames]: Registers human-readable common names for services, characteristics,
 *   and descriptors in the peripheral.
 *
 * ## Serializable
 * This class is marked with [Serializable], making it easy to serialize into or deserialize from JSON
 * using kotlinx.serialization.
 *
 * ## Example Usage
 * ```kotlin
 * // Create a peripheral representation
 * val peripheral = UUPeripheralRepresentation()
 *
 * // Decode from JSON
 * val jsonString = """
 * {
 *   "services": [
 *     {
 *       "uuid": "180D",
 *       "name": "Heart Rate",
 *       "isPrimary": true,
 *       "includedServices": [],
 *       "characteristics": [
 *         {
 *           "uuid": "2A37",
 *           "name": "Heart Rate Measurement",
 *           "properties": ["Notify"],
 *           "descriptors": []
 *         }
 *       ]
 *     }
 *   ]
 * }
 * """
 * val json = Json { encodeDefaults = true }
 * val decodedPeripheral = json.decodeFromString<UUPeripheralRepresentation>(jsonString)
 * println(decodedPeripheral.services?.firstOrNull()?.name ?: "No services")
 *
 * // Register common names
 * decodedPeripheral.registerCommonNames()
 * ```
 *
 * ## See Also
 * - [UUPeripheral]
 * - [UUServiceRepresentation]
 *
 * @since 1.0.0
 */
@Serializable
data class UUPeripheralRepresentation(
    /**
     * An optional human-readable name for the peripheral.
     *
     * @since 1.0.0
     */
    val name: String = "",

    /**
     * An optional array of services available on the peripheral.
     *
     * @since 1.0.0
     */
    val services: List<UUServiceRepresentation>? = null
) {
    /**
     * Convenience constructor to create a peripheral representation from a [UUPeripheral].
     *
     * This constructor extracts the name and services from the Android Bluetooth peripheral object.
     *
     * @param peripheral A [UUPeripheral] object from Android Bluetooth.
     *
     * @since 1.0.0
     */
    constructor(peripheral: UUPeripheral) : this(
        name = peripheral.name,
        services = peripheral.services
            ?.takeIf { it.isNotEmpty() }
            ?.map { UUServiceRepresentation(it) }
    )

    /**
     * Registers common names for services, characteristics, and descriptors.
     *
     * This method iterates through all services, characteristics, and descriptors associated with
     * the peripheral and logs their common names. In the Kotlin implementation, the common names
     * are already available through the [UUBluetooth.bluetoothSpecName] function, so this method
     * primarily serves as a logging utility.
     *
     * @since 1.0.0
     */
    fun registerCommonNames()
    {
        val services = this.services ?: return

        //val mappedNames = mutableMapOf<String, String>()

        for (service in services)
        {
            service.name?.let { name ->
                service.uuid.let { uuid ->
                    UUBluetooth.registerSpecName(uuid, name)
                }
            }

            service.characteristics?.forEach { characteristic ->
                characteristic.name?.let { name ->
                    characteristic.uuid.let { uuid ->
                        UUBluetooth.registerSpecName(uuid, name)
                    }
                }

                characteristic.descriptors?.forEach { descriptor ->
                    descriptor.name?.let { name ->
                        descriptor.uuid.let { uuid ->
                            UUBluetooth.registerSpecName(uuid, name)
                        }
                    }
                }
            }
        }

        //UULog.debug("UUPeripheralRepresentation", "Mapped common names: $mappedNames")
    }

    override fun toString(): String
    {
        val peripheralJson = UUJson.toJson(this, UUPeripheralRepresentation::class.java).getOrNull()
        return peripheralJson ?: super.toString()
    }
}