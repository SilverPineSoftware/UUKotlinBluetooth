package com.silverpine.uu.bluetooth.models

import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.extensions.uuCommonName
import kotlinx.serialization.Serializable

/**
 * A representation of a Bluetooth service.
 *
 * `UUServiceRepresentation` models a Bluetooth service, providing essential details
 * such as its UUID, name, primary status, included services, and associated characteristics.
 * This class is designed to work with Android Bluetooth and facilitate encoding and decoding
 * as JSON for storage or communication.
 *
 * ## Features
 * - Conforms to [Serializable] for seamless JSON serialization and deserialization.
 * - Supports initialization from Android's [BluetoothGattService].
 * - Includes an optional list of included services and characteristics associated with the service.
 *
 * ## Properties
 * - [uuid]: A unique identifier for the service, stored as a string.
 * - [name]: An optional human-readable name for the service.
 * - [isPrimary]: An optional boolean indicating whether the service is a primary service.
 * - [includedServices]: An optional array of [UUServiceRepresentation] objects representing the included services.
 * - [characteristics]: An optional array of [UUCharacteristicRepresentation] objects representing the service's characteristics.
 *
 * ## Initializers
 * - Primary constructor: Initializes a new service representation with a UUID, name, and primary status.
 * - Secondary constructor: Convenience constructor to create a service representation from a
 *   [BluetoothGattService] object.
 *
 * ## Serializable
 * This class is marked with [Serializable], making it easy to serialize into or deserialize from JSON
 * using kotlinx.serialization.
 *
 * ## Example Usage
 * ```kotlin
 * // Create a service representation
 * val service = UUServiceRepresentation(
 *     uuid = "180D",
 *     name = "Heart Rate",
 *     isPrimary = true
 * )
 *
 * // Encode to JSON
 * val json = Json { encodeDefaults = true }
 * val jsonString = json.encodeToString(service)
 * println(jsonString)
 *
 * // Decode from JSON
 * val jsonString = """
 * {
 *   "uuid": "180D",
 *   "name": "Heart Rate",
 *   "isPrimary": true,
 *   "includedServices": [],
 *   "characteristics": []
 * }
 * """
 * val decodedService = json.decodeFromString<UUServiceRepresentation>(jsonString)
 * println(decodedService.name ?: "No name")
 * ```
 *
 * ## See Also
 * - [BluetoothGattService]
 * - [UUCharacteristicRepresentation]
 *
 * @since 1.0.0
 */
@Serializable
data class UUServiceRepresentation(
    /**
     * A unique identifier for the service.
     *
     * @since 1.0.0
     */
    val uuid: String = "",

    /**
     * An optional human-readable name for the service.
     *
     * @since 1.0.0
     */
    val name: String? = null,

    /**
     * An optional boolean indicating whether the service is a primary service.
     *
     * Primary services are the main services that a device provides, while secondary services
     * are included within primary services.
     *
     * @since 1.0.0
     */
    val isPrimary: Boolean? = null,

    /**
     * An optional list of included services for this service.
     *
     * These are secondary services that are included within this service.
     *
     * @since 1.0.0
     */
    val includedServices: List<UUServiceRepresentation>? = null,

    /**
     * An optional list of characteristics associated with this service.
     *
     * @since 1.0.0
     */
    val characteristics: List<UUCharacteristicRepresentation>? = null
) {
    /**
     * Convenience constructor to create a service representation from a [BluetoothGattService].
     *
     * This constructor extracts the UUID, name, primary status, included services, and characteristics
     * from the Android Bluetooth service object.
     *
     * @param service A [BluetoothGattService] object from Android Bluetooth.
     *
     * @since 1.0.0
     */
    constructor(service: BluetoothGattService) : this(
        uuid = service.uuid.toString(),
        name = service.uuid.uuCommonName,
        isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
        includedServices = service.includedServices
            ?.takeIf { it.isNotEmpty() }
            ?.map { UUServiceRepresentation(it) },
        characteristics = service.characteristics
            ?.takeIf { it.isNotEmpty() }
            ?.map { UUCharacteristicRepresentation(it) }
    )
}