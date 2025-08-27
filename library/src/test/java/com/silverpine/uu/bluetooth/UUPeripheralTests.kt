package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.logging.UULog
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalAtomicApi::class)
class UUPeripheralTests
{
    @Before
    fun setupTests()
    {
        UULog.init(UnitTestLogger())

        UUTimer.workerThread = UnitTestTimerThread()
        UUTimer.listActiveTimers().forEach { it.cancel() }

        mockBluetoothError()
        mockUUBluetoothContext()
        mockUUDispatch()
    }

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
        val device = mockDevice(address)

        UUPeripheral.deviceCache[address] = device

        val connectedCalled = AtomicReference(false)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        val timerId = "${address}__Connect"
        UUTimer.startTimer(timerId, 100000, null)
        { _, _ ->

        }

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

    @Test
    fun connect_success_invokesConnected_and_setsGatt_andClearsWatchdog()
    {
        val address = "AA:00:AA:00:AA:00"
        val advertisement = UUAdvertisement(
            address = address,
            rssi = -55,
            timestamp = System.currentTimeMillis()
        )

        val peripheral = UUPeripheral(advertisement)

        // Provide a device via the cache
        val device = mockDevice(address)

        UUPeripheral.deviceCache = object : UUBluetoothDeviceCache {
            override fun get(identifier: String): BluetoothDevice? = if (identifier == address) device else null
            override fun set(identifier: String, device: BluetoothDevice?) { /* not needed */ }
        }

        // Capture the BluetoothGattCallback passed into connectGatt, and return a mock GATT
        val gatt = mockGatt()

        val connectLatch = CountDownLatch(1)

        val capturedCallback = AtomicReference<BluetoothGattCallback?>(null)
        `when`(
            device.connectGatt(
                Mockito.any(Context::class.java),
                Mockito.anyBoolean(),
                Mockito.any(BluetoothGattCallback::class.java),
                Mockito.anyInt()
            )
        ).thenAnswer { invocation ->
            capturedCallback.store(invocation.getArgument(2))
            connectLatch.countDown()
            gatt
        }

        val connectedLatch = CountDownLatch(1)
        val disconnectedCalled = AtomicReference(false)
        val disconnectedError = AtomicReference<UUError?>(null)

        // Act
        peripheral.connect(
            timeout = 5_000,
            connected = {
                connectedLatch.countDown()
                        },
            disconnected = {
                err ->
                disconnectedError.store(err)
                disconnectedCalled.store(true)
            }
        )

        assertTrue(connectLatch.await(1, TimeUnit.SECONDS))

        val cb = capturedCallback.load()
        assertNotNull(cb, "BluetoothGattCallback was not captured")
        cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED)

        // Assert
        assertTrue(connectedLatch.await(1, TimeUnit.SECONDS), "connected callback did not fire")
        assertFalse(disconnectedCalled.load(), "disconnected callback should not be called")

        // gatt should be cached for this peripheral
        val cachedGatt = UUPeripheral.gattCache[address]
        assertTrue(cachedGatt === gatt, "gatt was not cached for the device")

        UUPeripheral.gattCache[address] = null
    }

    @Test
    fun connect_error_invokesConnected_and_setsGatt_andClearsWatchdog()
    {
        UULog.init(UnitTestLogger())

        val address = "AA:00:AA:00:AA:00"
        val advertisement = UUAdvertisement(
            address = address,
            rssi = -55,
            timestamp = System.currentTimeMillis()
        )

        val peripheral = UUPeripheral(advertisement)

        // Provide a device via the cache
        val device = mockDevice(address)

        UUPeripheral.deviceCache = object : UUBluetoothDeviceCache {
            override fun get(identifier: String): BluetoothDevice? = if (identifier == address) device else null
            override fun set(identifier: String, device: BluetoothDevice?) { /* not needed */ }
        }

        // Capture the BluetoothGattCallback passed into connectGatt, and return a mock GATT
        val gatt = mockGatt()

        val connectLatch = CountDownLatch(1)

        val capturedCallback = AtomicReference<BluetoothGattCallback?>(null)
        `when`(
            device.connectGatt(
                Mockito.any(Context::class.java),
                Mockito.anyBoolean(),
                Mockito.any(BluetoothGattCallback::class.java),
                Mockito.anyInt()
            )
        ).thenAnswer { invocation ->
            capturedCallback.store(invocation.getArgument(2))
            connectLatch.countDown()
            gatt
        }

        val disconnectedLatch = CountDownLatch(1)
        val connectedCalled = AtomicReference(false)
        val disconnectedError = AtomicReference<UUError?>(null)

        // Act
        peripheral.connect(
            timeout = 5_000,
            connected = {
                connectedCalled.store(true)
            },
            disconnected = {
                    err ->
                disconnectedError.store(err)
                disconnectedLatch.countDown()
            }
        )

        assertTrue(connectLatch.await(1, TimeUnit.SECONDS))

        val cb = capturedCallback.load()
        assertNotNull(cb, "BluetoothGattCallback was not captured")
        cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_FAILURE, BluetoothGatt.STATE_DISCONNECTED)

        // Assert
        assertTrue(disconnectedLatch.await(1, TimeUnit.SECONDS), "disconnected callback did not fire")
        assertFalse(connectedCalled.load(), "connected callback should not be called")

        // gatt should be cached for this peripheral
        val cachedGatt = UUPeripheral.gattCache[address]
        assertNull(cachedGatt, "gatt should not be cached after connection failure")

        val err = disconnectedError.load()
        assertNotNull(err, "Expected an error")
        assertEquals(err.code, UUBluetoothErrorCode.ConnectionFailed.rawValue)
    }
}

/*
class UnitTestDeviceCache: UUBluetoothDeviceCache
{
    var devices = ConcurrentHashMap<String, BluetoothDevice>()

    override fun get(identifier: String): BluetoothDevice?
    {
        return devices[identifier]
    }

    override fun set(identifier: String, obj: BluetoothDevice?)
    {
        if (obj != null)
        {
            devices[identifier] = obj
        }
        else
        {
            devices.remove(identifier)
        }
    }

    fun clear()
    {
        devices.clear()
    }
}*/