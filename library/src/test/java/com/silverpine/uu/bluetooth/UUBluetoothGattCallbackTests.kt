package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.internal.uuHashLookup
import com.silverpine.uu.bluetooth.internal.uuToLowercaseString
import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.test.uuRandomBytes
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Suppress("Deprecation")
@OptIn(ExperimentalAtomicApi::class)
class UUBluetoothGattCallbackTests
{
    // clearAll tests

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

        // Assert: all public properties are null
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

    // registerReadCallback

    @Test
    fun register_thenNotify_invokesCallback_andThenPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-1"
        val called = AtomicBoolean(false)
        val got = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id)
        { bytes, error ->
            called.store(true)
            assertEquals(null, error)
            got.store(bytes?.contentEquals(byteArrayOf(1, 2)) == true)
            latch.countDown()
        }

        // This may fan out via uuDispatch(Dispatchers.IO) internally → async
        cb.notifyCharacteristicRead(id, byteArrayOf(1, 2), null)

        // Wait for IO callback to happen
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback did not fire in time")
        assertTrue(called.load())
        assertTrue(got.load())

        // Should be popped after first call
        val again = CountDownLatch(1)
        cb.notifyCharacteristicRead(id, byteArrayOf(1, 2), null)
        // Expect no callback now
        assertTrue(!again.await(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun reRegister_sameId_replacesOldCallback()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-dup"

        val oldCalled = AtomicBoolean(false)
        val newCalled = AtomicBoolean(false)

        val oldLatch = CountDownLatch(1)
        val newLatch = CountDownLatch(1)

        // First registration (should be replaced)
        cb.registerReadCharacteristicCallback(id) { _, _ ->
            oldCalled.store(true)
            oldLatch.countDown()
        }

        // Second registration replaces the first
        cb.registerReadCharacteristicCallback(id)
        { _, _ ->
            newCalled.store(true)
            newLatch.countDown()
        }

        // Trigger
        cb.notifyCharacteristicRead(id, byteArrayOf(0x0A), null)

        // The new callback should fire
        assertTrue(newLatch.await(1, TimeUnit.SECONDS), "New callback did not fire in time")
        assertTrue(newCalled.load(), "New callback should run")

        // The old callback must NOT fire
        assertFalse(oldLatch.await(200, TimeUnit.MILLISECONDS), "Old callback must be replaced")
        assertFalse(oldCalled.load(), "Old callback must not run")
    }

    @Test
    fun clearReadCharacteristicCallback_removesPendingCallback()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-clear"

        var called = false
        cb.registerReadCharacteristicCallback(id) { _, _ -> called = true }

        cb.clearReadCharacteristicCallback(id)

        cb.notifyCharacteristicRead(id, byteArrayOf(0x01), null)
        assertFalse(called, "Cleared callback must not run")
    }

    @Test
    fun multipleIds_areIndependent_andEachPopsSeparately()
    {
        val cb = UUBluetoothGattCallback()
        val idA = "char-A"
        val idB = "char-B"

        val aCount = AtomicInteger(0)
        val bCount = AtomicInteger(0)

        val firstA = CountDownLatch(1)
        val firstB = CountDownLatch(1)
        val secondA = CountDownLatch(1)
        val secondB = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(idA) { bytes, _ ->
            assertArrayEquals(byteArrayOf(0x11), bytes)
            val n = aCount.incrementAndGet()
            if (n == 1) firstA.countDown() else secondA.countDown()
        }

        cb.registerReadCharacteristicCallback(idB) { bytes, _ ->
            assertArrayEquals(byteArrayOf(0x22), bytes)
            val n = bCount.incrementAndGet()
            if (n == 1) firstB.countDown() else secondB.countDown()
        }

        // Notify A and B once — should each fire exactly once
        cb.notifyCharacteristicRead(idA, byteArrayOf(0x11), null)
        cb.notifyCharacteristicRead(idB, byteArrayOf(0x22), null)

        // Await first callbacks
        assertTrue(firstA.await(1, TimeUnit.SECONDS), "A did not fire in time")
        assertTrue(firstB.await(1, TimeUnit.SECONDS), "B did not fire in time")
        assertEquals(1, aCount.get(), "A should fire once")
        assertEquals(1, bCount.get(), "B should fire once")

        // Try to notify again — callbacks are one-shot and should be popped
        cb.notifyCharacteristicRead(idA, byteArrayOf(0x11), null)
        cb.notifyCharacteristicRead(idB, byteArrayOf(0x22), null)

        // Ensure second callbacks NEVER fire
        assertFalse(secondA.await(200, TimeUnit.MILLISECONDS), "A fired more than once")
        assertFalse(secondB.await(200, TimeUnit.MILLISECONDS), "B fired more than once")
        assertEquals(1, aCount.get(), "A should still be exactly once")
        assertEquals(1, bCount.get(), "B should still be exactly once")
    }

    @Test
    fun readDescriptor_register_thenNotify_invokesCallback_andThenPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-1"

        val called = java.util.concurrent.atomic.AtomicBoolean(false)
        val valueOk = java.util.concurrent.atomic.AtomicBoolean(false)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { bytes, error ->
            called.set(true)
            assertNull(error)
            valueOk.set(bytes?.contentEquals(byteArrayOf(0x55, 0x66)) == true)
            latch.countDown()
        }

        cb.notifyDescriptorRead(id, byteArrayOf(0x55, 0x66), null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback did not fire in time")
        assertTrue(called.get())
        assertTrue(valueOk.get())

        // Should be popped after first call
        val again = CountDownLatch(1)
        cb.notifyDescriptorRead(id, byteArrayOf(0x55, 0x66), null)
        assertFalse(again.await(200, TimeUnit.MILLISECONDS), "Callback should be popped")
    }

    @Test
    fun readDescriptor_reRegister_sameId_replacesOldCallback()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-dup"

        val oldCalled = java.util.concurrent.atomic.AtomicBoolean(false)
        val newCalled = java.util.concurrent.atomic.AtomicBoolean(false)

        val oldLatch = CountDownLatch(1)
        val newLatch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { _, _ ->
            oldCalled.set(true)
            oldLatch.countDown()
        }

        // Second registration replaces the first
        cb.registerReadDescriptorCallback(id)
        { _, _ ->
            newCalled.set(true)
            newLatch.countDown()
        }

        cb.notifyDescriptorRead(id, byteArrayOf(0x01), null)

        assertTrue(newLatch.await(1, TimeUnit.SECONDS), "New callback did not fire in time")
        assertTrue(newCalled.get())
        assertFalse(oldLatch.await(200, TimeUnit.MILLISECONDS), "Old callback must be replaced")
        assertFalse(oldCalled.get())
    }

    @Test
    fun readDescriptor_clearReadDescriptorCallback_removesPendingCallback()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-clear"

        val called = java.util.concurrent.atomic.AtomicBoolean(false)
        cb.registerReadDescriptorCallback(id) { _, _ -> called.set(true) }

        cb.clearReadDescriptorCallback(id)

        val latch = CountDownLatch(1)
        cb.notifyDescriptorRead(id, byteArrayOf(0x7F), null)
        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "Cleared callback must not run")
        assertFalse(called.get())
    }

    @Test
    fun readDescriptor_multipleIds_areIndependent_andEachPopsSeparately()
    {
        val cb = UUBluetoothGattCallback()
        val idA = "desc-A"
        val idB = "desc-B"

        val aCount = AtomicInteger(0)
        val bCount = AtomicInteger(0)

        val firstA = CountDownLatch(1)
        val firstB = CountDownLatch(1)
        val secondA = CountDownLatch(1)
        val secondB = CountDownLatch(1)

        cb.registerReadDescriptorCallback(idA)
        { bytes, _ ->
            assertArrayEquals(byteArrayOf(0x11), bytes)
            if (aCount.incrementAndGet() == 1) firstA.countDown() else secondA.countDown()
        }

        cb.registerReadDescriptorCallback(idB)
        { bytes, _ ->
            assertArrayEquals(byteArrayOf(0x22), bytes)
            if (bCount.incrementAndGet() == 1) firstB.countDown() else secondB.countDown()
        }

        cb.notifyDescriptorRead(idA, byteArrayOf(0x11), null)
        cb.notifyDescriptorRead(idB, byteArrayOf(0x22), null)

        assertTrue(firstA.await(1, TimeUnit.SECONDS), "A did not fire in time")
        assertTrue(firstB.await(1, TimeUnit.SECONDS), "B did not fire in time")
        assertEquals(1, aCount.get(), "A should fire once")
        assertEquals(1, bCount.get(), "B should fire once")

        // second notifications should not fire (callbacks are one-shot)
        cb.notifyDescriptorRead(idA, byteArrayOf(0x11), null)
        cb.notifyDescriptorRead(idB, byteArrayOf(0x22), null)

        assertFalse(secondA.await(200, TimeUnit.MILLISECONDS), "A fired more than once")
        assertFalse(secondB.await(200, TimeUnit.MILLISECONDS), "B fired more than once")
        assertEquals(1, aCount.get())
        assertEquals(1, bCount.get())
    }

    @Test
    fun readDescriptor_nullValue_stillInvokesWithNoError()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-null"

        val called = java.util.concurrent.atomic.AtomicBoolean(false)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { bytes, error ->
            called.set(true)
            assertNull(error)
            assertNull(bytes) // exercising nullable value path
            latch.countDown()
        }

        cb.notifyDescriptorRead(id, null, null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback did not fire in time")
        assertTrue(called.get())
    }

    @Test
    fun dataChanged_register_thenOnCharacteristicChanged_invokes_and_isPersistent()
    {
        val cb = UUBluetoothGattCallback()

        // Mock characteristic so we can control UUID and value on the JVM
        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)

        // Two successive payloads → prove persistence (callback fires more than once)
        `when`(ch.value).thenReturn(byteArrayOf(0x01))

        // Use the same ID format your impl uses to map a characteristic to an ID
        val id = uuid.uuToLowercaseString()

        val count = AtomicInteger(0)
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)

        cb.registerCharacteristicDataChangedCallback(id)
        { bytes ->
            when (count.incrementAndGet())
            {
                1 ->
                    {
                    assertArrayEquals(byteArrayOf(0x01), bytes, "first payload mismatch")
                    first.countDown()
                }

                2 ->
                    {
                    assertArrayEquals(byteArrayOf(0x02), bytes, "second payload mismatch")
                    second.countDown()
                }
            }
        }

        // Fire twice via deprecated overload that reads characteristic.value
        cb.onCharacteristicChanged(null, ch)
        assertTrue(first.await(1, TimeUnit.SECONDS), "first callback did not fire")
        assertEquals(1, count.get(), "callback should be persistent and fire once")

        `when`(ch.value).thenReturn(byteArrayOf(0x02))
        cb.onCharacteristicChanged(null, ch)

        assertTrue(second.await(1, TimeUnit.SECONDS), "second callback did not fire")
        assertEquals(2, count.get(), "callback should be persistent and fire twice")
    }

    @Test
    fun dataChanged_register_thenOnCharacteristicChanged_withBytes_invokes_and_isPersistent()
    {
        val cb = UUBluetoothGattCallback()

        // Mock characteristic so we can control UUID and value on the JVM
        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)

        // Two successive payloads → prove persistence (callback fires more than once)
        //`when`(ch.value).thenReturn(byteArrayOf(0x01))

        // Use the same ID format your impl uses to map a characteristic to an ID
        val id = uuid.uuToLowercaseString()

        val count = AtomicInteger(0)
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)

        cb.registerCharacteristicDataChangedCallback(id)
        { bytes ->
            when (count.incrementAndGet())
            {
                1 ->
                {
                    assertArrayEquals(byteArrayOf(0x01), bytes, "first payload mismatch")
                    first.countDown()
                }

                2 ->
                {
                    assertArrayEquals(byteArrayOf(0x02), bytes, "second payload mismatch")
                    second.countDown()
                }
            }
        }

        // Fire twice via deprecated overload that reads characteristic.value
        cb.onCharacteristicChanged(mockGatt(), ch, byteArrayOf(0x01))
        assertTrue(first.await(1, TimeUnit.SECONDS), "first callback did not fire")
        assertEquals(1, count.get(), "callback should be persistent and fire once")

        cb.onCharacteristicChanged(mockGatt(), ch, byteArrayOf(0x02))

        assertTrue(second.await(1, TimeUnit.SECONDS), "second callback did not fire")
        assertEquals(2, count.get(), "callback should be persistent and fire twice")
    }

    @Test
    fun dataChanged_reRegister_sameId_replacesOldCallback()
    {
        val cb = UUBluetoothGattCallback()

        // Mock characteristic so we control UUID and value on the JVM
        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(byteArrayOf(0x0A)) // payload doesn't matter; only who gets called

        // Must match the ID format used internally for data-changed routing
        val id = uuid.uuToLowercaseString()

        val oldCalled = java.util.concurrent.atomic.AtomicBoolean(false)
        val newCalled = java.util.concurrent.atomic.AtomicBoolean(false)
        val oldLatch = CountDownLatch(1)
        val newLatch = CountDownLatch(1)

        // First registration (should be replaced)
        cb.registerCharacteristicDataChangedCallback(id)
        {
            oldCalled.set(true)
            oldLatch.countDown()
        }

        // Second registration replaces the first
        cb.registerCharacteristicDataChangedCallback(id)
        {
            newCalled.set(true)
            newLatch.countDown()
        }

        // Trigger callback path
        cb.onCharacteristicChanged(null, ch)

        // Only the new callback should fire
        assertTrue(newLatch.await(1, TimeUnit.SECONDS), "new callback did not fire")
        assertTrue(newCalled.get(), "new callback flag not set")
        assertFalse(oldLatch.await(200, TimeUnit.MILLISECONDS), "old callback should not fire")
        assertFalse(oldCalled.get(), "old callback should be replaced")
    }

    @Test
    fun dataChanged_clearCharacteristicDataChangedCallback_stopsFurtherInvocations()
    {
        val cb = UUBluetoothGattCallback()

        // Mock the characteristic (it's a final Android class → requires mockito-inline)
        val ch = mock(BluetoothGattCharacteristic::class.java)

        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid) // uuHashLookup() calls characteristic.uuid

        // Return bytes for successive reads of characteristic.value
        // 1st onCharacteristicChanged → 0x55
        // 2nd onCharacteristicChanged → 0x56 (should NOT be observed after we clear)
        `when`(ch.value).thenReturn(byteArrayOf(0x55), byteArrayOf(0x56))

        // Register using the same ID that the implementation will look up internally
        val id = ch.uuid.uuToLowercaseString()

        val called = AtomicBoolean(false)
        val firstLatch = CountDownLatch(1)

        cb.registerCharacteristicDataChangedCallback(id)
        { _ ->
            called.store(true)
            firstLatch.countDown()
        }

        // First change → should invoke the callback
        cb.onCharacteristicChanged(null, ch)
        assertTrue(firstLatch.await(1, TimeUnit.SECONDS), "callback did not fire before clear")
        assertTrue(called.load(), "callback flag not set before clear")

        // Clear and verify no further invocations
        cb.clearCharacteristicDataChangedCallback(id)
        called.store(false)

        // Second change (value now 0x56 per stubbing) → should NOT invoke the callback
        val secondLatch = CountDownLatch(1) // nothing will count this down now
        cb.onCharacteristicChanged(null, ch)

        assertFalse(secondLatch.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clear")
        assertFalse(called.load(), "callback should not be called after clear")
    }

    @Test
    fun dataChanged_multipleIds_areIndependent_and_persistent()
    {
        val cb = UUBluetoothGattCallback()

        // Mock characteristics to avoid Android dependencies and control values
        val chA = mock(BluetoothGattCharacteristic::class.java)
        val chB = mock(BluetoothGattCharacteristic::class.java)

        val uuidA = UUID.randomUUID()
        val uuidB = UUID.randomUUID()
        `when`(chA.uuid).thenReturn(uuidA)
        `when`(chB.uuid).thenReturn(uuidB)

        // First then second payloads to prove persistence
        `when`(chA.value).thenReturn(byteArrayOf(0x11))
        `when`(chB.value).thenReturn(byteArrayOf(0x22))

        // Use the same ID format the impl uses onCharacteristicChanged (UUID lowercased)
        val idA = chA.uuid.uuToLowercaseString()
        val idB = chB.uuid.uuToLowercaseString()

        val aCount = AtomicInteger(0)
        val bCount = AtomicInteger(0)
        val firstA = CountDownLatch(1)
        val firstB = CountDownLatch(1)
        val secondA = CountDownLatch(1)
        val secondB = CountDownLatch(1)

        val firstAResult = AtomicReference<ByteArray?>(null)
        val secondAResult = AtomicReference<ByteArray?>(null)
        val firstBResult = AtomicReference<ByteArray?>(null)
        val secondBResult = AtomicReference<ByteArray?>(null)

        cb.registerCharacteristicDataChangedCallback(idA)
        { bytes ->

            val n = aCount.incrementAndGet()
            if (n == 1)
            {
                firstAResult.store(bytes)
                firstA.countDown()
            }

            if (n == 2)
            {
                secondAResult.store(bytes)
                secondA.countDown()
            }
        }
        cb.registerCharacteristicDataChangedCallback(idB)
        { bytes ->
            val n = bCount.incrementAndGet()
            if (n == 1)
            {
                firstBResult.store(bytes)
                firstB.countDown()
            }

            if (n == 2)
            {
                secondBResult.store(bytes)
                secondB.countDown()
            }
        }

        // First change on each → should invoke once each
        cb.onCharacteristicChanged(null, chA)
        cb.onCharacteristicChanged(null, chB)

        assertTrue(firstA.await(1, TimeUnit.SECONDS), "A did not fire in time")
        assertTrue(firstB.await(1, TimeUnit.SECONDS), "B did not fire in time")
        assertEquals(1, aCount.get(), "A count mismatch after first")
        assertEquals(1, bCount.get(), "B count mismatch after first")
        assertArrayEquals(byteArrayOf(0x11), firstAResult.load())
        assertArrayEquals(byteArrayOf(0x22), firstBResult.load())

        `when`(chA.value).thenReturn(byteArrayOf(0x12))
        `when`(chB.value).thenReturn(byteArrayOf(0x23))

        // Second change on each → prove persistence (callbacks are NOT popped)
        cb.onCharacteristicChanged(null, chA)
        cb.onCharacteristicChanged(null, chB)

        assertTrue(secondA.await(1, TimeUnit.SECONDS), "A did not fire in time")
        assertTrue(secondB.await(1, TimeUnit.SECONDS), "B did not fire in time")
        assertEquals(2, aCount.get(), "A should fire twice total")
        assertEquals(2, bCount.get(), "B should fire twice total")
        assertArrayEquals(byteArrayOf(0x12), secondAResult.load())
        assertArrayEquals(byteArrayOf(0x23), secondBResult.load())
    }

    @Test
    fun dataChanged_nullValue_doesNotInvokeCallback_onDeprecatedOverload()
    {
        val cb = UUBluetoothGattCallback()

        // Mock the characteristic so we can force a null value on JVM
        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(null) // simulate null payload

        // Use the same ID format as the impl when mapping a characteristic to an ID
        val id = uuid.uuToLowercaseString()

        val called = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        cb.registerCharacteristicDataChangedCallback(id)
        {
            called.store(true)
            latch.countDown()
        }

        // Deprecated overload that reads characteristic.value internally
        cb.onCharacteristicChanged(null, ch)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "callback should not fire when value is null")
        assertFalse(called.load(), "callback should not be called when value is null")
    }

    @Test
    fun notifyCharacteristicRead_clearedCallback_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-read"
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id) { _, _ ->
            called.store(true); latch.countDown()
        }
        cb.clearReadCharacteristicCallback(id)

        cb.notifyCharacteristicRead(id, byteArrayOf(0x01), null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clear")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyCharacteristicWrite_clearedCallback_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-write"
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerWriteCharacteristicCallback(id) {
            called.store(true); latch.countDown()
        }
        cb.clearWriteCharacteristicCallback(id)

        cb.notifyCharacteristicWrite(id, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clear")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyDescriptorRead_clearedCallback_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-read"
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id) { _, _ ->
            called.store(true); latch.countDown()
        }
        cb.clearReadDescriptorCallback(id)

        cb.notifyDescriptorRead(id, byteArrayOf(0x02), null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clear")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyDescriptorWrite_clearedCallback_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-write"
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerWriteDescriptorCallback(id) {
            called.store(true); latch.countDown()
        }
        cb.clearWriteDescriptorCallback(id)

        cb.notifyDescriptorWrite(id, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clear")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifySetCharacteristicNotification_clearedCallback_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val id = "set-notify"
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerSetCharacteristicNotificationCallback(id) {
            called.store(true); latch.countDown()
        }
        cb.clearSetCharacteristicNotificationCallback(id)

        cb.notifyCharacteristicSetNotifyCallback(id, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clear")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyRemoteRssiRead_nullProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.readRssiCallback = { _, _ -> called.store(true); latch.countDown() }
        cb.readRssiCallback = null

        cb.notifyRemoteRssiRead(-42, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after nulling")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyMtuChanged_nullProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.mtuChangedCallback = { _, _ -> called.store(true); latch.countDown() }
        cb.mtuChangedCallback = null

        cb.notifyMtuChanged(247, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after nulling")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyPhyRead_nullProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.phyReadCallback = { _, _ -> called.store(true); latch.countDown() }
        cb.phyReadCallback = null

        cb.notifyPhyRead(2, 2, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after nulling")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyPhyUpdate_nullProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.phyUpdatedCallback = { _, _ -> called.store(true); latch.countDown() }
        cb.phyUpdatedCallback = null

        cb.notifyPhyUpdate(1, 3, null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after nulling")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun notifyServicesDiscovered_nullProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()
        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.servicesDiscoveredCallback = { _, _ -> called.store(true); latch.countDown() }
        cb.servicesDiscoveredCallback = null

        cb.notifyServicesDiscovered(emptyList(), null)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after nulling")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun readCharacteristic_withError_invokesAndPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-read-error"
        val called = AtomicReference(false)
        val sawError = AtomicReference(false)
        val latch = CountDownLatch(1)
        val err = mock(UUError::class.java)

        cb.registerReadCharacteristicCallback(id)
        { _, e ->
            called.store(true)
            sawError.store(e != null)
            latch.countDown()
        }

        cb.notifyCharacteristicRead(id, byteArrayOf(0x01, 0x02), err)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire in time")
        assertTrue(called.load(), "callback flag not set")
        assertTrue(sawError.load(), "error should be non-null")

        // verify one-shot pop
        called.store(false)
        cb.notifyCharacteristicRead(id, byteArrayOf(0x03), err)
        assertFalse(called.load(), "callback should be popped after first use")
    }

    @Test
    fun writeCharacteristic_withError_invokesAndPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "char-write-error"
        val called = AtomicReference(false)
        val sawError = AtomicReference(false)
        val latch = CountDownLatch(1)
        val err = mock(UUError::class.java)

        cb.registerWriteCharacteristicCallback(id)
        { e ->
            called.store(true)
            sawError.store(e != null)
            latch.countDown()
        }

        cb.notifyCharacteristicWrite(id, err)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire in time")
        assertTrue(called.load(), "callback flag not set")
        assertTrue(sawError.load(), "error should be non-null")

        // verify one-shot pop
        called.store(false)
        cb.notifyCharacteristicWrite(id, err)
        assertFalse(called.load(), "callback should be popped after first use")
    }

    @Test
    fun readDescriptor_withError_invokesAndPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-read-error"
        val called = AtomicReference(false)
        val sawError = AtomicReference(false)
        val latch = CountDownLatch(1)
        val err = mock(UUError::class.java)

        cb.registerReadDescriptorCallback(id)
        { _, e ->
            called.store(true)
            sawError.store(e != null)
            latch.countDown()
        }

        cb.notifyDescriptorRead(id, byteArrayOf(0x09), err)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire in time")
        assertTrue(called.load(), "callback flag not set")
        assertTrue(sawError.load(), "error should be non-null")

        // verify one-shot pop
        called.store(false)
        cb.notifyDescriptorRead(id, byteArrayOf(0x09), err)
        assertFalse(called.load(), "callback should be popped after first use")
    }

    @Test
    fun writeDescriptor_withError_invokesAndPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "desc-write-error"
        val called = AtomicReference(false)
        val sawError = AtomicReference(false)
        val latch = CountDownLatch(1)
        val err = mock(UUError::class.java)

        cb.registerWriteDescriptorCallback(id)
        { e ->
            called.store(true)
            sawError.store(e != null)
            latch.countDown()
        }

        cb.notifyDescriptorWrite(id, err)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire in time")
        assertTrue(called.load(), "callback flag not set")
        assertTrue(sawError.load(), "error should be non-null")

        // verify one-shot pop
        called.store(false)
        cb.notifyDescriptorWrite(id, err)
        assertFalse(called.load(), "callback should be popped after first use")
    }

    @Test
    fun setCharacteristicNotification_withError_invokesAndPops()
    {
        val cb = UUBluetoothGattCallback()
        val id = "set-notify-error"
        val called = AtomicReference(false)
        val sawError = AtomicReference(false)
        val latch = CountDownLatch(1)
        val err = mock(UUError::class.java)

        cb.registerSetCharacteristicNotificationCallback(id)
        { e ->
            called.store(true)
            sawError.store(e != null)
            latch.countDown()
        }

        cb.notifyCharacteristicSetNotifyCallback(id, err)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire in time")
        assertTrue(called.load(), "callback flag not set")
        assertTrue(sawError.load(), "error should be non-null")

        // verify one-shot pop
        called.store(false)
        cb.notifyCharacteristicSetNotifyCallback(id, err)
        assertFalse(called.load(), "callback should be popped after first use")
    }

    @Test
    fun connectionStateChanged_invokesCallback_twice_and_doesNotPop()
    {
        val cb = UUBluetoothGattCallback()

        val calls = AtomicInteger(0)
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)
        val lastPair = AtomicReference<Pair<Int, Int>?>(null)

        cb.connectionStateChangedCallback =
        { p ->
            lastPair.store(p)

            when (calls.incrementAndGet())
            {
                1 -> first.countDown()
                2 -> second.countDown()
            }
        }

        val gatt = mockGatt()
        cb.onConnectionStateChange(gatt, 0, android.bluetooth.BluetoothProfile.STATE_CONNECTED)
        assertTrue(first.await(1, TimeUnit.SECONDS), "first change not observed")

        cb.onConnectionStateChange(gatt, 5, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(second.await(1, TimeUnit.SECONDS), "second change not observed")

        assertEquals(2, calls.get(), "callback should persist and be called twice")
        assertEquals(Pair(5, android.bluetooth.BluetoothProfile.STATE_DISCONNECTED), lastPair.load(), "last pair mismatch")
    }

    @Test
    fun connectionStateChanged_clearedProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.connectionStateChangedCallback =
        {
            called.store(true)
            latch.countDown()
        }

        cb.connectionStateChangedCallback = null

        val gatt = mockGatt()
        cb.onConnectionStateChange(gatt, 0, android.bluetooth.BluetoothProfile.STATE_CONNECTED)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire after clearing callback")
        assertFalse(called.load(), "callback should not be invoked after clearing")
    }

    @Test
    fun connectionStateChanged_deliversExactStatusAndState()
    {
        val cb = UUBluetoothGattCallback()

        val got = AtomicReference<Pair<Int, Int>?>(null)
        val latch = CountDownLatch(1)

        cb.connectionStateChangedCallback =
        { p ->
            got.store(p)
            latch.countDown()
        }

        val expected = Pair(42, android.bluetooth.BluetoothProfile.STATE_CONNECTING)

        val gatt = mockGatt()
        cb.onConnectionStateChange(gatt, expected.first, expected.second)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(expected, got.load(), "status/state pair mismatch")
    }

    @Test
    fun onServicesDiscovered_success_invokesCallback_withServices_andClears()
    {
        val cb = UUBluetoothGattCallback()

        // Mock GATT + services
        val gatt = mockGatt()
        val svc = mock(BluetoothGattService::class.java)
        `when`(gatt.services).thenReturn(listOf(svc))

        val gotServices = AtomicReference<List<BluetoothGattService>?>(null)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.servicesDiscoveredCallback =
        { services, error ->
            gotServices.store(services)
            gotError.store(error)
            latch.countDown()
        }

        // status = 0 → success
        cb.onServicesDiscovered(gatt, /*status=*/0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(listOf(svc), gotServices.load(), "services list mismatch")
        assertEquals(null, gotError.load(), "error should be null for success")

        // Ensure single-shot: second call should not invoke (callback is cleared)
        val second = CountDownLatch(1)
        cb.onServicesDiscovered(gatt, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire a second time")
    }

    @Test
    fun onServicesDiscovered_error_withStaticMock_invokesCallback_withNonNullError_andClears() {
        val cb = UUBluetoothGattCallback()

        val gatt = mockGatt()
        val svc = mock(BluetoothGattService::class.java)
        `when`(gatt.services).thenReturn(listOf(svc))

        val gotServices = AtomicReference<List<BluetoothGattService>?>(null)
        val gotError = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.servicesDiscoveredCallback = { services, error ->
            gotServices.store(services)
            gotError.store(error)
            latch.countDown()
        }

        val err = mock(UUError::class.java)

        // Arrange
        mockkObject(UUBluetoothError)

        every {
            UUBluetoothError.makeError(UUBluetoothErrorCode.OperationFailed, any())
        } returns err


        cb.onServicesDiscovered(gatt, /*status=*/129)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(listOf(svc), gotServices.load(), "services list mismatch")
        assertTrue(gotError.load() === err, "should be the mocked UUError")

        // single-shot clear
        val second = CountDownLatch(1)
        cb.onServicesDiscovered(gatt, 129)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onServicesDiscovered_clearedProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()

        val gatt = mockGatt()
        `when`(gatt.services).thenReturn(emptyList())

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.servicesDiscoveredCallback =
        { _, _ ->
            called.store(true)
            latch.countDown()
        }

        // Clear before calling
        cb.servicesDiscoveredCallback = null

        cb.onServicesDiscovered(gatt, /*status=*/0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire when callback cleared")
        assertFalse(called.load(), "callback must not be invoked after clearing")
    }

    @Test
    fun onServicesDiscovered_propagatesExactServicesReference()
    {
        val cb = UUBluetoothGattCallback()

        val gatt = mockGatt()
        val svcA = mock(BluetoothGattService::class.java)
        val svcB = mock(BluetoothGattService::class.java)
        val expected = listOf(svcA, svcB)
        `when`(gatt.services).thenReturn(expected)

        val got = AtomicReference<List<BluetoothGattService>?>(null)
        val latch = CountDownLatch(1)

        cb.servicesDiscoveredCallback =
        { services, _ ->
            got.store(services)
            latch.countDown()
        }

        cb.onServicesDiscovered(gatt, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(got.load() === expected, "should pass through the same list instance")
    }

    // onCharacteristicRead

    @Test
    fun onCharacteristicRead_deprecated_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(byteArrayOf(0x01, 0x02))
        val id = ch.uuid.uuToLowercaseString()

        val got = AtomicReference<ByteArray?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id)
        { bytes, e ->
            got.store(bytes); gotErr.store(e); latch.countDown()
        }

        val err = mock(UUError::class.java)

        mockkObject(UUBluetoothError)

        every {
            UUBluetoothError.makeError(UUBluetoothErrorCode.OperationFailed, any())
        } returns err

        cb.onCharacteristicRead(null, ch, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertArrayEquals(byteArrayOf(0x01, 0x02), got.load(), "payload mismatch", )
        assertEquals(null, gotErr.load(), "error should be null")

        // one-shot pop
        val again = CountDownLatch(1)
        cb.onCharacteristicRead(null, ch, 0)
        assertFalse(again.await(200, TimeUnit.MILLISECONDS), "callback should be popped")
    }

    @Test
    fun onCharacteristicRead_deprecated_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(byteArrayOf(0x09))
        val id = ch.uuid.uuToLowercaseString()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id)
        { _, e ->
            gotErr.store(e); latch.countDown()
        }

        val err = mock(UUError::class.java)

        mockkObject(UUBluetoothError)

        every {
            UUBluetoothError.makeError(UUBluetoothErrorCode.OperationFailed, any())
        } returns err

        cb.onCharacteristicRead(null, ch, 129)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(gotErr.load() === err, "should receive mocked error instance")
    }

    @Test
    fun onCharacteristicRead_deprecated_nullCharacteristic_doesNothing()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)
        cb.registerReadCharacteristicCallback("won’t-be-used")
        { _, _ ->
            called.store(true); latch.countDown()
        }

        // Null characteristic → early return, no dispatch
        cb.onCharacteristicRead(null, null, 0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire for null characteristic")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun onCharacteristicRead_deprecated_nullValue_invokes_with_null_bytes()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(null)
        val id = ch.uuid.uuToLowercaseString()

        val got = AtomicReference<ByteArray?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id) { bytes, _ ->
            got.store(bytes); latch.countDown()
        }

        cb.onCharacteristicRead(null, ch, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "bytes should be null")
    }

    @Test
    fun onCharacteristicRead_new_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        val id = ch.uuid.uuToLowercaseString()
        val value = byteArrayOf(0x0A, 0x0B)

        val got = AtomicReference<ByteArray?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id)
        { bytes, e ->
            got.store(bytes); gotErr.store(e); latch.countDown()
        }

        mockkObject(UUBluetoothError)

        cb.onCharacteristicRead(mockGatt(), ch, value, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertArrayEquals(value, got.load(), "payload mismatch")
        assertEquals(null, gotErr.load(), "error should be null")
    }

    @Test
    fun onCharacteristicRead_new_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        val id = ch.uuid.uuToLowercaseString()
        val value = byteArrayOf(0x7F)

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadCharacteristicCallback(id)
        { _, e ->
            gotErr.store(e); latch.countDown()
        }

        val err = mock(UUError::class.java)

        mockkObject(UUBluetoothError)

        every {
            UUBluetoothError.makeError(UUBluetoothErrorCode.OperationFailed, any())
        } returns err

        cb.onCharacteristicRead(mockGatt(), ch, value, 257)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(gotErr.load() === err, "should receive mocked error instance")
    }

    @Test
    fun onCharacteristicWrite_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(byteArrayOf(0x22))
        val id = ch.uuid.uuToLowercaseString()

        val called = AtomicReference(false)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerWriteCharacteristicCallback(id)
        { e ->
            called.store(true); gotErr.store(e); latch.countDown()
        }

        cb.onCharacteristicWrite(null, ch, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(called.load(), "callback flag not set")
        assertEquals(null, gotErr.load(), "error should be null")

        // pop
        called.store(false)
        cb.onCharacteristicWrite(null, ch, 0)
        assertFalse(called.load(), "callback should be popped")
    }

    @Test
    fun onCharacteristicWrite_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val ch = mock(BluetoothGattCharacteristic::class.java)
        val uuid = UUID.randomUUID()
        `when`(ch.uuid).thenReturn(uuid)
        `when`(ch.value).thenReturn(byteArrayOf(0x33))
        val id = ch.uuid.uuToLowercaseString()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerWriteCharacteristicCallback(id)
        { e ->
            gotErr.store(e); latch.countDown()
        }

        val err = mock(UUError::class.java)
        mockkObject(UUBluetoothError)

        every {
            UUBluetoothError.makeError(UUBluetoothErrorCode.OperationFailed, any())
        } returns err

        cb.onCharacteristicWrite(null, ch, 5)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(gotErr.load() === err, "should receive mocked error instance")
    }

    @Test
    fun onCharacteristicWrite_nullCharacteristic_doesNothing()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)
        cb.registerWriteCharacteristicCallback("unused")
        {
            called.store(true); latch.countDown()
        }

        cb.onCharacteristicWrite(null, null, 0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire for null characteristic")
        assertFalse(called.load(), "callback should not be called")
    }


    @Test
    fun onDescriptorRead_deprecated_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val bytes = uuRandomBytes(20)
        val d = mockDescriptor(data = bytes)
        val id = d.uuHashLookup()

        val got = AtomicReference<ByteArray?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { bytes, e ->
            got.store(bytes)
            gotErr.store(e)
            latch.countDown()
        }

        cb.onDescriptorRead(null, d, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertArrayEquals(bytes, got.load(), "payload mismatch")
        assertEquals(null, gotErr.load(), "error should be null")
    }

    @Test
    fun onDescriptorRead_deprecated_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val bytes = uuRandomBytes(0x55)
        val d = mockDescriptor(data = bytes)
        val id = d.uuHashLookup()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { _, e ->
            gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onDescriptorRead(null, d, 8)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)
    }

    @Test
    fun onDescriptorRead_deprecated_nullDescriptor_doesNothing()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback("unused")
        { _, _ ->
            called.store(true); latch.countDown()
        }

        cb.onDescriptorRead(null, null, 0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire for null descriptor")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun onDescriptorRead_deprecated_nullValue_invokes_with_null_bytes()
    {
        val cb = UUBluetoothGattCallback()

        val d = mockDescriptor(data = null)
        val id = d.uuHashLookup()

        val got = AtomicReference<ByteArray?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id)
        { bytes, _ ->
            got.store(bytes); latch.countDown()
        }

        cb.onDescriptorRead(null, d, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "bytes should be null")
    }

    @Test
    fun onDescriptorRead_new_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val value = byteArrayOf(0x66)
        val d = mockDescriptor(data = value)
        val id = d.uuHashLookup()

        val got = AtomicReference<ByteArray?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id) { bytes, e ->
            got.store(bytes); gotErr.store(e); latch.countDown()
        }

        cb.onDescriptorRead(mockGatt(), d, 0, value)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertArrayEquals(value, got.load(), "payload mismatch")
        assertEquals(null, gotErr.load(), "error should be null")
    }

    @Test
    fun onDescriptorRead_new_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val d = mock(BluetoothGattDescriptor::class.java)
        val uuid = UUID.randomUUID()
        `when`(d.uuid).thenReturn(uuid)
        val id = d.uuHashLookup()
        val value = byteArrayOf(0x77)

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerReadDescriptorCallback(id) { _, e ->
            gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onDescriptorRead(mockGatt(), d, 42, value)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)
    }

    @Test
    fun onDescriptorWrite_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val d = mockDescriptor()
        val id = d.uuHashLookup()

        val called = AtomicReference(false)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerWriteDescriptorCallback(id) { e ->
            called.store(true); gotErr.store(e); latch.countDown()
        }

        cb.onDescriptorWrite(null, d, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertTrue(called.load(), "callback flag not set")
        assertEquals(null, gotErr.load(), "error should be null")

        // popped
        called.store(false)
        cb.onDescriptorWrite(null, d, 0)
        assertFalse(called.load(), "callback should be popped")
    }

    @Test
    fun onDescriptorWrite_error_invokes_with_error_and_clears()
    {
        val cb = UUBluetoothGattCallback()

        val d = mockDescriptor()
        val id = d.uuHashLookup()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.registerWriteDescriptorCallback(id) { e ->
            gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onDescriptorWrite(null, d, 6)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)
    }

    @Test
    fun onDescriptorWrite_nullDescriptor_doesNothing()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)
        cb.registerWriteDescriptorCallback("unused")
        {
            called.store(true); latch.countDown()
        }

        cb.onDescriptorWrite(null, null, 0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire for null descriptor")
        assertFalse(called.load(), "callback should not be called")
    }

    @Test
    fun onReliableWriteCompleted_success_invokesCallback_andClears()
    {
        val cb = UUBluetoothGattCallback()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.executeReliableWriteCallback =
        { e ->
            gotErr.store(e)
            latch.countDown()
        }

        // Success path: status = 0 → UUBluetoothError.gattStatusError(...) should return null.
        // No Mockito needed here.
        cb.onReliableWriteCompleted(null, 0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, gotErr.load(), "error should be null for success")

        // single-shot: a second call should not invoke
        val second = CountDownLatch(1)
        cb.onReliableWriteCompleted(null, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after being cleared")
    }

    @Test
    fun onReliableWriteCompleted_error_invokesCallback_withMockedError_andClears()
    {
        val cb = UUBluetoothGattCallback()

        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.executeReliableWriteCallback =
            { e ->
            gotErr.store(e)
            latch.countDown()
        }

        // Arrange the next Bluetooth error produced by the code path
        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        // Non-zero status → should deliver the mocked error
        cb.onReliableWriteCompleted(null, 133)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)

        // cleared after first invocation
        val second = CountDownLatch(1)
        cb.onReliableWriteCompleted(null, 133)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onReliableWriteCompleted_clearedProperty_doesNotInvoke()
    {
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.executeReliableWriteCallback =
        {
            called.store(true)
            latch.countDown()
        }

        // Clear before notifying
        cb.executeReliableWriteCallback = null

        cb.onReliableWriteCompleted(null, 0)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire when callback is cleared")
        assertFalse(called.load(), "callback should not be invoked after clearing")
    }

    @Test
    fun onReadRemoteRssi_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Int?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.readRssiCallback =
        { v, e ->
            got.store(v); gotErr.store(e); latch.countDown()
        }

        cb.onReadRemoteRssi(null, /*rssi=*/-55, /*status=*/0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(-55, got.load(), "rssi mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        // single-shot cleared
        val second = CountDownLatch(1)
        cb.onReadRemoteRssi(null, -44, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onReadRemoteRssi_error_invokes_with_mockedError_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Int?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.readRssiCallback =
        { v, e ->
            got.store(v); gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onReadRemoteRssi(null, /*rssi=*/-60, /*status=*/133)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(-60, got.load(), "rssi mismatch")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)

        val second = CountDownLatch(1)
        cb.onReadRemoteRssi(null, -59, 133)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onMtuChanged_success_invokes_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Int?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.mtuChangedCallback =
        { v, e ->
            got.store(v); gotErr.store(e); latch.countDown()
        }

        cb.onMtuChanged(null, /*mtu=*/247, /*status=*/0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(247, got.load(), "mtu mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onMtuChanged(null, 200, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onMtuChanged_error_invokes_with_mockedError_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Int?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.mtuChangedCallback =
        { v, e ->
            got.store(v); gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onMtuChanged(null, /*mtu=*/185, /*status=*/22)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(185, got.load(), "mtu mismatch")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)

        val second = CountDownLatch(1)
        cb.onMtuChanged(null, 128, 22)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onPhyRead_success_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyReadCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        cb.onPhyRead(null, /*tx=*/2, /*rx=*/3, /*status=*/0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(Pair(2, 3), got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 1, 1, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun notifyPhyRead_success_null_tx_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyReadCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        cb.notifyPhyRead(/*tx=*/null, /*rx=*/3, /*status=*/null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 1, 1, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun notifyPhyRead_success_null_rx_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyReadCallback =
            { p, e ->
                got.store(p); gotErr.store(e); latch.countDown()
            }

        cb.notifyPhyRead(/*tx=*/2, /*rx=*/null, /*status=*/null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 1, 1, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun notifyPhyUpdate_success_null_tx_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyUpdatedCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        cb.notifyPhyUpdate(/*tx=*/null, /*rx=*/3, /*status=*/null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 1, 1, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun notifyPhyUpdate_success_null_rx_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyUpdatedCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        cb.notifyPhyUpdate(/*tx=*/2, /*rx=*/null, /*status=*/null)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(null, got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 1, 1, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onPhyRead_error_invokes_with_mockedError_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyReadCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onPhyRead(null, /*tx=*/1, /*rx=*/2, /*status=*/7)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(Pair(1, 2), got.load(), "phy pair mismatch")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)

        val second = CountDownLatch(1)
        cb.onPhyRead(null, 3, 3, 7)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onPhyUpdate_success_invokes_pair_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyUpdatedCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        cb.onPhyUpdate(null, /*tx=*/3, /*rx=*/1, /*status=*/0)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(Pair(3, 1), got.load(), "phy pair mismatch")
        assertEquals(null, gotErr.load(), "error should be null")

        val second = CountDownLatch(1)
        cb.onPhyUpdate(null, 2, 2, 0)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun onPhyUpdate_error_invokes_with_mockedError_and_clears()
    {
        val cb = UUBluetoothGattCallback()
        val got = AtomicReference<Pair<Int, Int>?>(null)
        val gotErr = AtomicReference<UUError?>(null)
        val latch = CountDownLatch(1)

        cb.phyUpdatedCallback =
        { p, e ->
            got.store(p); gotErr.store(e); latch.countDown()
        }

        //mockNextBluetoothError(UUBluetoothErrorCode.OperationFailed)
        mockBluetoothError()

        cb.onPhyUpdate(null, /*tx=*/1, /*rx=*/3, /*status=*/9)

        assertTrue(latch.await(1, TimeUnit.SECONDS), "callback did not fire")
        assertEquals(Pair(1, 3), got.load(), "phy pair mismatch")
        assertEquals(UUBluetoothErrorCode.OperationFailed.rawValue, gotErr.load()?.code)

        val second = CountDownLatch(1)
        cb.onPhyUpdate(null, 1, 3, 9)
        assertFalse(second.await(200, TimeUnit.MILLISECONDS), "callback should not fire after clearing")
    }

    @Test
    fun serviceChanged_invokes_and_persists()
    {
        val gatt = mockGatt()
        val cb = UUBluetoothGattCallback()

        val calls = AtomicInteger(0)
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)

        cb.serviceChangedCallback =
        {
            when (calls.incrementAndGet())
            {
                1 -> first.countDown()
                2 -> second.countDown()
            }
        }

        // Use wrapper to avoid Android BluetoothGatt dependency
        cb.onServiceChanged(gatt)
        cb.onServiceChanged(gatt)

        assertTrue(first.await(1, TimeUnit.SECONDS), "first serviceChanged not observed")
        assertTrue(second.await(1, TimeUnit.SECONDS), "second serviceChanged not observed")
        assertEquals(2, calls.get(), "callback should persist and be called twice")
    }

    @Test
    fun serviceChanged_clearedProperty_doesNotInvoke()
    {
        val gatt = mockGatt()
        val cb = UUBluetoothGattCallback()

        val called = AtomicReference(false)
        val latch = CountDownLatch(1)

        cb.serviceChangedCallback =
        {
            called.store(true); latch.countDown()
        }
        // Clear before notifying
        cb.serviceChangedCallback = null

        cb.onServiceChanged(gatt)

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS), "should not fire when callback is cleared")
        assertFalse(called.load(), "callback should not be invoked after clearing")
    }

    @Test
    fun onCharacteristicChanged_nullChar()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        cb.onCharacteristicChanged(mockGatt(), null)
        assertEquals(1, logger.logLines.count())
    }

    @Test
    fun onCharacteristicWriteChanged_nullChar()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        cb.onCharacteristicWrite(mockGatt(), null, 0)
        assertEquals(1, logger.logLines.count())
    }

    @Test
    fun onDescriptorWrite_nullChar()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        cb.onDescriptorWrite(mockGatt(), null, 0)
        assertEquals(1, logger.logLines.count())
    }

    @Test
    fun onCharacteristicWrite_nullData()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        val chr = mockCharacteristic(data = null)

        cb.onCharacteristicWrite(mockGatt(), chr, 0)
        assertEquals(1, logger.logLines.count())
    }

    @Test
    fun onDescriptorWrite_nullGatt_nullChar()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        cb.onDescriptorWrite(null, null, 0)
        assertEquals(1, logger.logLines.count())
    }

    @Test
    fun onDescriptorWrite_nullData()
    {
        val cb = UUBluetoothGattCallback()
        val logger = UnitTestLogger()
        UULog.init(logger)

        val d = mockDescriptor(data = null)
        cb.onDescriptorWrite(null, d, 0)
        assertEquals(1, logger.logLines.count())
    }


}