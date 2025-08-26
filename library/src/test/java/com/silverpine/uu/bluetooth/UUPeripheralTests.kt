package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalAtomicApi::class)
class UUPeripheralTests
{
    @Test
    fun connect_alreadyConnected_returnsAlreadyConnectedError()
    {
        val address = "AA:BB:CC:DD:EE:FF"
        val advertisement = UUAdvertisement(
            address = address,
            rssi = -55,
            timestamp = System.currentTimeMillis()
        )

        val peripheral = UUPeripheral(advertisement)

        // Simulate existing GATT → triggers early "already connected" error
        val gatt = mockGatt()
        UUPeripheral.gattCache[address] = gatt

        val connectedCalled = AtomicReference(false)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        mockNextBluetoothError(UUBluetoothErrorCode.AlreadyConnected)

        peripheral.connect(
            timeout = 5_000,
            connected = { connectedCalled.store(true) },
            disconnected = { e -> gotError.store(e); latch.countDown() }
        )

        assertTrue(latch.await(1, TimeUnit.SECONDS), "disconnected callback did not fire")
        assertFalse(connectedCalled.load(), "connected callback should not be called")
        assertEquals(UUBluetoothErrorCode.AlreadyConnected.rawValue, gotError.load()?.code, "error code mismatch")

        // cleanup
        UUPeripheral.gattCache[address] = null
    }

    @Test
    fun connect_deviceNotFound_returnsPreconditionFailedError()
    {
        val address = "11:22:33:44:55:66"
        val advertisement = UUAdvertisement(
            address = address,
            rssi = -55,
            timestamp = System.currentTimeMillis()
        )
        val peripheral = UUPeripheral(advertisement)

        // Ensure device cache has NO device for this address
        UUPeripheral.deviceCache[address] = null

        val connectedCalled = AtomicReference(false)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        mockNextBluetoothError(UUBluetoothErrorCode.PreconditionFailed)

        peripheral.connect(
            timeout = 5_000,
            connected = { connectedCalled.store(true) },
            disconnected = { e -> gotError.store(e); latch.countDown() }
        )

        assertTrue(latch.await(1, TimeUnit.SECONDS), "disconnected callback did not fire")
        assertFalse(connectedCalled.load(), "connected callback should not be called")
        assertEquals(UUBluetoothErrorCode.PreconditionFailed.rawValue, gotError.load()?.code, "error code mismatch")
    }

    @Test
    fun testMocks()
    {
        val t = mockTimer("foo")
    }

    @Test
    fun connect_watchdogActive_returnsAlreadyConnectedError()
    {
        val address = "AA:00:AA:00:AA:00"
        val advertisement = UUAdvertisement(
            address = address,
            rssi = -55,
            timestamp = System.currentTimeMillis()
        )
        val peripheral = UUPeripheral(advertisement)

        // Must have a device in the cache to bypass the "device not found" branch
        val device = mockDevice(address = address)

        UUPeripheral.deviceCache[address] = device

        val connectedCalled = AtomicReference(false)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        mockNextBluetoothError(UUBluetoothErrorCode.AlreadyConnected)

        // Pretend a connect watchdog timer is already active
        /*Mockito.mockStatic(UUTimer::class.java).use { st ->
            st.`when`<UUTimer?> { UUTimer.findActiveTimer(Mockito.anyString()) }
                .thenReturn(mock(UUTimer::class.java))


        }*/

        val timerId = "${address}__Connect"
        mockTimer(timerId)

        peripheral.connect(
            timeout = 5_000,
            connected = { connectedCalled.store(true) },
            disconnected = { e -> gotError.store(e); latch.countDown() }
        )

        assertTrue(latch.await(1, TimeUnit.SECONDS), "disconnected callback did not fire")
        assertFalse(connectedCalled.load(), "connected callback should not be called")
        assertEquals(UUBluetoothErrorCode.AlreadyConnected.rawValue, gotError.load()?.code, "error code mismatch")

        // cleanup
        UUPeripheral.deviceCache[address] = null
    }
}