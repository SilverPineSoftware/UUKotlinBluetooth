package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattService
import com.silverpine.uu.core.UUError
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.fail
import org.junit.Test

class UUBluetoothGattCallbackTests
{
    @Test
    fun clearAll_removesAllCallbacks_andPreventsFurtherInvocations()
    {
        val cb = UUBluetoothGattCallback()

        // Register one of each callback so clearAll has something to clear
        cb.connectionStateChangedCallback = { /* no-op */ }
        cb.servicesDiscoveredCallback = { _: List<BluetoothGattService>?, _: UUError? -> }

        cb.registerReadCharacteristicCallback("char-read") { _, _ -> fail("readCharacteristic should be cleared") }
        cb.registerWriteCharacteristicCallback("char-write") { _ -> fail("writeCharacteristic should be cleared") }
        cb.registerReadDescriptorCallback("desc-read") { _, _ -> fail("readDescriptor should be cleared") }
        cb.registerWriteDescriptorCallback("desc-write") { _ -> fail("writeDescriptor should be cleared") }
        cb.registerSetCharacteristicNotificationCallback("set-notify") { _ -> fail("setNotify should be cleared") }

        cb.readRssiCallback = { _, _ -> fail("readRssi should be cleared") }
        cb.mtuChangedCallback = { _, _ -> fail("mtuChanged should be cleared") }
        cb.phyReadCallback = { _, _ -> fail("phyRead should be cleared") }
        cb.phyUpdatedCallback = { _, _ -> fail("phyUpdate should be cleared") }
        cb.executeReliableWriteCallback = { _ -> fail("reliableWrite should be cleared") }
        cb.serviceChangedCallback = { fail("serviceChanged should be cleared") }

        // Act
        cb.clearAll()

        // Assert: all public properties are nulled
        assertNull(cb.connectionStateChangedCallback)
        assertNull(cb.servicesDiscoveredCallback)
        assertNull(cb.readRssiCallback)
        assertNull(cb.mtuChangedCallback)
        assertNull(cb.phyReadCallback)
        assertNull(cb.phyUpdatedCallback)
        assertNull(cb.executeReliableWriteCallback)
        assertNull(cb.serviceChangedCallback)

        // And: no previously-registered map callbacks fire after clearAll
        // (If any of these cause the earlier fail() lambdas to run, the test fails.)
        cb.notifyCharacteristicRead("char-read", byteArrayOf(1, 2, 3), null)
        cb.notifyCharacteristicWrite("char-write", null)
        cb.notifyDescriptorRead("desc-read", byteArrayOf(9), null)
        cb.notifyDescriptorWrite("desc-write", null)
        cb.notifyCharacteristicSetNotifyCallback("set-notify", null)

        // Single-shot property callbacks should also be inert now
        cb.notifyRemoteRssiRead(-55, null)
        cb.notifyMtuChanged(200, null)
        cb.notifyPhyRead(2, 2, null)
        cb.notifyPhyUpdate(1, 3, null)

        // Services-discovered is also single-shot; property is already null so this is inert
        cb.notifyServicesDiscovered(emptyList(), null)

        // If we reached here with no failures/exceptions, clearAll worked.
        assert(true)
    }
}