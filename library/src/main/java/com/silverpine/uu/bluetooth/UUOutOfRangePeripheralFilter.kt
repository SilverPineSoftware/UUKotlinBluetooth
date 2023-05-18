package com.silverpine.uu.bluetooth

/**
 * Interface that callers of UUBluetoothScanner can use declare a device as 'out of range'.  Out of
 * range can be determined a number of different ways, and that is left up to the caller of the
 * framework.  UUBluetoothScanner will process the nearby devices list against the out of range
 * filters, and anything that is marked OutOfRange will be removed from the nearby peripheral list.
 */
interface UUOutOfRangePeripheralFilter<T : UUPeripheral?>
{
    enum class Result
    {
        InRange, OutOfRange
    }

    /**
     * Return true if the peripheral should be included in the scan results
     *
     * @param peripheral the peripheral to check
     *
     * @return value indicating whether the peripheral should be ignored for this one advertisement or forever, or discovered
     */
    fun checkPeripheralRange(peripheral: T): Result?
}