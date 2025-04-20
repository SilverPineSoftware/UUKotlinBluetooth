package com.silverpine.uu.bluetooth.old
//
//import com.silverpine.uu.core.UUError
//
///**
// * Interface for delivering BTLE connection events to callers
// */
//interface UUConnectionDelegate
//{
//    /**
//     * Invoked when a peripheral is successfully connected.
//     *
//     * @param peripheral the peripheral that was connected
//     */
//    fun onConnected(peripheral: UUPeripheral)
//
//    /**
//     * Invoked when a peripheral was disconnected
//     *
//     * @param peripheral the peripheral that was disconnect
//     * @param error the error (if any) that caused the disconnect to occur
//     */
//    fun onDisconnected(peripheral: UUPeripheral, error: UUError?)
//}