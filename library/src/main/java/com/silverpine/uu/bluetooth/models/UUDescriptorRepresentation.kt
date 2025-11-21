package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import kotlinx.serialization.Serializable

/**
 * A representation of a Bluetooth descriptor.
 *
 * `UUDescriptorRepresentation` models a Bluetooth descriptor, which provides additional metadata or
 * configuration for a Bluetooth characteristic. This class is designed to simplify working with
 * descriptors and their representation in JSON or other data formats.
 *
 * ## Features
 * - Conforms to [Serializable] for easy serialization and deserialization.
 * - Uses a unique `uuid` attribute to uniquely identify the descriptor.
 * - Includes an optional `name` property for additional context.
 * - Provides convenience initialization from a [BluetoothGattDescriptor] object.
 *
 * ## Properties
 * - [uuid]: A unique identifier for the descriptor, stored as a string.
 * - [name]: An optional string providing a human-readable name for the descriptor.
 *
 * ## Initializers
 * - Primary constructor: Initializes a new descriptor representation with a UUID and an optional name.
 * - Secondary constructor: Convenience constructor to create a descriptor representation from a
 *   [BluetoothGattDescriptor] object.
 *
 * ## Serializable
 * This class is marked with [Serializable], allowing instances to be serialized into or
 * deserialized from JSON using kotlinx.serialization.
 *
 * ## Example Usage
 * ```kotlin
 * // Creating a descriptor representation
 * val descriptor = UUDescriptorRepresentation(
 *     uuid = "2902",
 *     name = "Client Characteristic Configuration"
 * )
 *
 * // Encoding to JSON
 * val json = Json { encodeDefaults = true }
 * val jsonString = json.encodeToString(descriptor)
 * println(jsonString)
 *
 * // Decoding from JSON
 * val jsonString = """
 * {
 *   "uuid": "2902",
 *   "name": "Client Characteristic Configuration"
 * }
 * """
 * val decodedDescriptor = json.decodeFromString<UUDescriptorRepresentation>(jsonString)
 * println(decodedDescriptor.name ?: "No name") // Output: Client Characteristic Configuration
 * ```
 *
 * ## See Also
 * - [BluetoothGattDescriptor]
 * - [UUCharacteristicRepresentation]
 *
 * @since 1.0.0
 */
@Serializable
data class UUDescriptorRepresentation(
    /**
     * A unique identifier for the descriptor.
     *
     * @since 1.0.0
     */
    val uuid: String = "",

    /**
     * An optional human-readable name for the descriptor.
     *
     * @since 1.0.0
     */
    val name: String? = null
) {
    /**
     * Convenience constructor to create a descriptor representation from a [BluetoothGattDescriptor].
     *
     * @param descriptor A [BluetoothGattDescriptor] object from Android Bluetooth.
     *
     * @since 1.0.0
     */
    constructor(descriptor: BluetoothGattDescriptor) : this(
        uuid = descriptor.uuid.toString(),
        name = descriptor.uuid.uuCommonName
    )
}