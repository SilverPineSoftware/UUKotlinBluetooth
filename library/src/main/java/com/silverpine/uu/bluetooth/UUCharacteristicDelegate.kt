package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import com.silverpine.uu.core.UUError

/**
 * Interface for delivering BTLE characteristic specific async events to callers
 */
interface UUCharacteristicDelegate
{
    /**
     * Callback invoked when a BTLE event is completed.
     *
     * @param peripheral the peripheral being interacted with
     * @param characteristic the characteristic being interacted with
     * @param error an error if one occurs
     */
    fun onComplete(
        peripheral: UUPeripheral,
        characteristic: BluetoothGattCharacteristic,
        error: UUError?
    )
}