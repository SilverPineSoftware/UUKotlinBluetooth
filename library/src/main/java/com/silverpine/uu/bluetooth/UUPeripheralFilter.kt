package com.silverpine.uu.bluetooth

interface UUPeripheralFilter
{
    fun shouldDiscover(peripheral: UUPeripheral): Boolean
}

///**
// * Interface that callers of UUBluetoothScanner can use to manually filter
// * BTLE advertisements
// */
//interface UUPeripheralFilter<T : UUPeripheral>
//{
//    /**
//     * Enum describing the possible return values during discovery filtering
//     */
//    enum class Result
//    {
//        /**
//         * The advertisement is ignored one time, leaving advertisements from this BLE device to
//         * be processed later.  This result is often used for things like an RSSI filter where
//         * something about the advertisement is known or expected to change over time.
//         */
//        IgnoreOnce,
//
//        /**
//         * The advertisement is ignored for the duration of the scan.  This BLE device will not be
//         * discovered until a stop/start scan has been done.
//         */
//        IgnoreForever,
//
//        /**
//         * The advertisement is good and the BLE device will be added to the list of discovered
//         * BLE devices.
//         */
//        Discover
//    }
//
//    /**
//     * Return true if the peripheral should be included in the scan results
//     *
//     * @param peripheral the peripheral to check
//     *
//     * @return value indicating whether the peripheral should be ignored for this one advertisement or forever, or discovered
//     */
//    fun shouldDiscoverPeripheral(peripheral: T): Result
//}