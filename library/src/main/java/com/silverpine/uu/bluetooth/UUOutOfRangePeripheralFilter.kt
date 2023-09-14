package com.silverpine.uu.bluetooth

/**
 * Interface that callers of UUBluetoothScanner can use declare a device as 'out of range'.  Out of
 * range can be determined a number of different ways, and that is left up to the caller of the
 * framework.  UUBluetoothScanner will process the nearby devices list against the out of range
 * filters, and anything that is marked OutOfRange will be removed from the nearby peripheral list.
 * <br>
 * Typical implementations will use RSSI or a 'time since last beacon' type of logic to determine
 * if the BLE device is within range.
 *
 */
interface UUOutOfRangePeripheralFilter<T : UUPeripheral>
{
    /**
     * Enum describing the possible return values during discovery out of range filtering
     */
    enum class Result
    {
        /**
         * The BLE device is considered in range
         */
        InRange,

        /**
         * The BLE device is considered out of range.
         */
        OutOfRange
    }

    /**
     * Return true if the peripheral should be included in the scan results
     *
     * @param peripheral the peripheral to check
     *
     * @return value indicating whether the peripheral should be ignored for this one advertisement or forever, or discovered
     */
    fun checkPeripheralRange(peripheral: T): Result
}