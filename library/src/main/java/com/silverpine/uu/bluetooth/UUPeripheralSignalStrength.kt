package com.silverpine.uu.bluetooth

import com.silverpine.uu.bluetooth.UUPeripheralSignalStrength.Companion.thresholds


/**
 * Bluetooth peripheral signal strength levels based on RSSI (Received Signal Strength Indicator) values.
 *
 * This enum class provides configurable RSSI thresholds that calling applications can adjust to customize
 * the signal strength categorization based on their specific requirements. The default thresholds follow
 * industry-standard Bluetooth RSSI classifications, but can be modified by accessing the [thresholds]
 * property in the companion object.
 *
 * Example of customizing thresholds:
 * ```
 * // Adjust individual threshold values
 * UUPeripheralSignalStrength.thresholds.veryGood = -35
 * UUPeripheralSignalStrength.thresholds.good = -55
 * UUPeripheralSignalStrength.thresholds.moderate = -75
 * UUPeripheralSignalStrength.thresholds.poor = -90
 *
 * // Or create a new Thresholds instance with custom values
 * UUPeripheralSignalStrength.thresholds = UUPeripheralSignalStrength.Thresholds(
 *     veryGood = -35,
 *     good = -55,
 *     moderate = -75,
 *     poor = -90
 * )
 * ```
 */
enum class UUPeripheralSignalStrength
{
    /**
     * Very poor signal strength (RSSI < -90 dBm).
     */
    VERY_POOR,

    /**
     * Poor signal strength (RSSI >= -90 dBm and < -70 dBm).
     */
    POOR,

    /**
     * Moderate signal strength (RSSI >= -70 dBm and < -50 dBm).
     */
    MODERATE,

    /**
     * Good signal strength (RSSI >= -50 dBm and < -30 dBm).
     */
    GOOD,

    /**
     * Very good signal strength (RSSI >= -30 dBm).
     */
    VERY_GOOD;

    /**
     * Configurable RSSI threshold values (in dBm) for signal strength classification.
     * These values can be modified to customize the signal strength categorization.
     * Thresholds must be in descending order (VERY_GOOD >= GOOD >= MODERATE >= POOR).
     */
    data class Thresholds(
        /**
         * RSSI threshold for very good signal strength (default: -30 dBm).
         */
        var veryGood: Int = -30,

        /**
         * RSSI threshold for good signal strength (default: -50 dBm).
         */
        var good: Int = -50,

        /**
         * RSSI threshold for moderate signal strength (default: -70 dBm).
         */
        var moderate: Int = -70,

        /**
         * RSSI threshold for poor signal strength (default: -90 dBm).
         */
        var poor: Int = -90
    )
    {
        init
        {
            require(veryGood >= good && good >= moderate && moderate >= poor)
            {
                "Thresholds must be in descending order: veryGood >= good >= moderate >= poor"
            }
        }
    }

    companion object
    {   /**
         * Default threshold values. Modify this instance to customize signal strength categorization.
         */
        var thresholds = Thresholds()

        /**
         * Converts an RSSI value to a UUPeripheralSignalStrength enum value.
         *
         * @param signal the RSSI value in dBm (typically negative, where values closer to 0 indicate stronger signals)
         * @return the corresponding UUPeripheralSignalStrength based on the RSSI thresholds
         */
        fun from(signal: Int): UUPeripheralSignalStrength
        {
            val t = thresholds
            return when
            {
                signal >= t.veryGood -> VERY_GOOD
                signal >= t.good -> GOOD
                signal >= t.moderate -> MODERATE
                signal >= t.poor -> POOR
                else -> VERY_POOR
            }
        }
    }
}