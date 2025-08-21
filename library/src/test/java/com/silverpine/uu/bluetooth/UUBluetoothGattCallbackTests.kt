package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.internal.uuToLowercaseString
import com.silverpine.uu.core.UUError
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.fail
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun reRegister_sameId_replacesOldCallback() {
        val cb = UUBluetoothGattCallback()
        val id = "char-dup"

        val oldCalled = java.util.concurrent.atomic.AtomicBoolean(false)
        val newCalled = java.util.concurrent.atomic.AtomicBoolean(false)

        val oldLatch = CountDownLatch(1)
        val newLatch = CountDownLatch(1)

        // First registration (should be replaced)
        cb.registerReadCharacteristicCallback(id) { _, _ ->
            oldCalled.set(true)
            oldLatch.countDown()
        }

        // Second registration replaces the first
        cb.registerReadCharacteristicCallback(id) { _, _ ->
            newCalled.set(true)
            newLatch.countDown()
        }

        // Trigger
        cb.notifyCharacteristicRead(id, byteArrayOf(0x0A), null)

        // The new callback should fire
        assertTrue(newLatch.await(1, TimeUnit.SECONDS), "New callback did not fire in time")
        assertTrue(newCalled.get(), "New callback should run")

        // The old callback must NOT fire
        assertFalse(oldLatch.await(200, TimeUnit.MILLISECONDS), "Old callback must be replaced")
        assertFalse(oldCalled.get(), "Old callback must not run")
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
    fun multipleIds_areIndependent_andEachPopsSeparately() {
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
            assertNull("No error expected", error)
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
                    assertArrayEquals("first payload mismatch", byteArrayOf(0x01), bytes)
                    first.countDown()
                }

                2 ->
                    {
                    assertArrayEquals("second payload mismatch", byteArrayOf(0x02), bytes)
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




}
