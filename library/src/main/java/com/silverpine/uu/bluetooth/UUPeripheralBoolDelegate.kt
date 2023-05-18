package com.silverpine.uu.bluetooth

/**
 * Interface for delivering async results from a UUPeripheral action that returns a boolean
 */
interface UUPeripheralBoolDelegate
{
    /**
     * Callback invoked when a peripheral action is completed.
     *
     * @param peripheral the peripheral being interacted with
     * @param result result of the operation
     */
    fun onComplete(peripheral: UUPeripheral, result: Boolean)
}