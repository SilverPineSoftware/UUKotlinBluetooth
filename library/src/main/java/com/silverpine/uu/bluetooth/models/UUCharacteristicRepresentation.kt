package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattCharacteristic
import com.silverpine.uu.bluetooth.extensions.uuCharacteristicPropertiesString
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import kotlinx.serialization.Serializable

/**
 * A representation of a Bluetooth characteristic.
 *
 * `UUCharacteristicRepresentation` models a Bluetooth characteristic, providing essential details
 * such as its UUID, name, properties, and associated descriptors. This class is designed to work with
 * Android Bluetooth and to facilitate encoding and decoding as JSON for storage or communication.
 *
 * ## Features
 * - Conforms to [Serializable] for seamless JSON serialization and deserialization.
 * - Supports initialization from Android's [BluetoothGattCharacteristic].
 * - Includes an optional list of properties and descriptors associated with the characteristic.
 *
 * ## Properties
 * - [uuid]: A unique identifier for the characteristic, stored as a string.
 * - [name]: An optional human-readable name for the characteristic.
 * - [properties]: An optional array of property strings that describe the characteristic's features.
 * - [descriptors]: An optional array of [UUDescriptorRepresentation] objects representing the characteristic's descriptors.
 *
 * ## Initializers
 * - Primary constructor: Initializes a new characteristic representation with a UUID, name, properties, and descriptors.
 * - Secondary constructor: Convenience constructor to create a characteristic representation from a
 *   [BluetoothGattCharacteristic] object.
 *
 * ## Serializable
 * This class is marked with [Serializable], making it easy to serialize into or deserialize from JSON
 * using kotlinx.serialization.
 *
 * ## Example Usage
 * ```kotlin
 * // Create a characteristic representation
 * val characteristic = UUCharacteristicRepresentation(
 *     uuid = "2A37",
 *     name = "Heart Rate Measurement",
 *     properties = listOf("Notify"),
 *     descriptors = listOf(
 *         UUDescriptorRepresentation(
 *             uuid = "2902",
 *             name = "Client Characteristic Configuration"
 *         )
 *     )
 * )
 *
 * // Encode to JSON
 * val json = Json { encodeDefaults = true }
 * val jsonString = json.encodeToString(characteristic)
 * println(jsonString)
 *
 * // Decode from JSON
 * val jsonString = """
 * {
 *   "uuid": "2A37",
 *   "name": "Heart Rate Measurement",
 *   "properties": ["Notify"],
 *   "descriptors": [
 *     { "uuid": "2902", "name": "Client Characteristic Configuration" }
 *   ]
 * }
 * """
 * val decodedCharacteristic = json.decodeFromString<UUCharacteristicRepresentation>(jsonString)
 * println(decodedCharacteristic.name ?: "No name")
 * ```
 *
 * ## See Also
 * - [BluetoothGattCharacteristic]
 * - [UUDescriptorRepresentation]
 *
 * @since 1.0.0
 */
@Serializable
data class UUCharacteristicRepresentation(
    /**
     * A unique identifier for the characteristic.
     *
     * @since 1.0.0
     */
    val uuid: String = "",

    /**
     * An optional human-readable name for the characteristic.
     *
     * @since 1.0.0
     */
    val name: String? = null,

    /**
     * An optional list of property strings describing the characteristic's features.
     *
     * Common properties include: "Read", "Write", "Notify", "Indicate", etc.
     *
     * @since 1.0.0
     */
    val properties: List<String>? = null,

    /**
     * An optional list of descriptors associated with the characteristic.
     *
     * @since 1.0.0
     */
    val descriptors: List<UUDescriptorRepresentation>? = null
) {
    /**
     * Convenience constructor to create a characteristic representation from a [BluetoothGattCharacteristic].
     *
     * This constructor extracts the UUID, name, properties, and descriptors from the Android
     * Bluetooth characteristic object.
     *
     * @param characteristic A [BluetoothGattCharacteristic] object from Android Bluetooth.
     *
     * @since 1.0.0
     */
    constructor(characteristic: BluetoothGattCharacteristic) : this(
        uuid = characteristic.uuid.toString(),
        name = characteristic.uuid.uuCommonName,
        properties = characteristic.properties.uuCharacteristicPropertiesString
            .takeIf { it.isNotEmpty() }
            ?.split(", ")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() },
        descriptors = characteristic.descriptors
            ?.takeIf { it.isNotEmpty() }
            ?.map { UUDescriptorRepresentation(it) }
    )
}