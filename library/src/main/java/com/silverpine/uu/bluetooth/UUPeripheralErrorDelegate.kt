package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError

///**
// * Interface for delivering async results from a UUPeripheral action
// */
//interface UUPeripheralErrorDelegate
//{
//    /**
//     * Callback invoked when a peripheral action is completed.
//     *
//     * @param peripheral the peripheral being interacted with
//     * @param error an error if one occurs
//     */
//    fun onComplete(peripheral: UUPeripheral, error: UUError?)
//}

typealias UUPeripheralErrorDelegate = (UUPeripheral,UUError?)->Unit