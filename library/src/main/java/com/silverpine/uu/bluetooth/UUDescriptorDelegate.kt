package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.core.UUError

/**
 * Interface for delivering BTLE characteristic descriptor specific async events to callers
 */
interface UUDescriptorDelegate
{
    /**
     * Callback invoked when a BTLE event is completed.
     *
     * @param peripheral the peripheral being interacted with
     * @param descriptor the descriptor being interacted with
     * @param error an error if one occurs
     */
    fun onComplete(peripheral: UUPeripheral, descriptor: BluetoothGattDescriptor, error: UUError?)
}