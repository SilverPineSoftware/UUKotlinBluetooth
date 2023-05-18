package com.silverpine.uu.bluetooth

/**
 * Interface for delivering async results from a UUPeripheral action
 */
interface UUPeripheralDelegate
{
    /**
     * Callback invoked when a peripheral action is completed.
     *
     * @param peripheral the peripheral being interacted with
     */
    fun onComplete(peripheral: UUPeripheral)
}