package com.silverpine.uu.bluetooth

import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.core.UUError
import com.silverpine.uu.test.UUAssert
import com.silverpine.uu.test.instrumented.annotations.UUInteractionRequired
import junit.framework.TestCase.fail
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@UUInteractionRequired
class UUPeripheralScannerTests
{
    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun testStartScanWithoutPermissions()
    {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        UUBluetooth.init(context)

        val endedLatch = CountDownLatch(1)
        val errorContainer = AtomicReference<UUError?>(null)

        val scanner = UUBluetooth.scanner
        scanner.started =
        {
            fail("Scanner should not have started")
        }

        scanner.ended =
        { scanner, error ->
            errorContainer.store(error)
            endedLatch.countDown()
        }

        scanner.listChanged =
        { scanner, list ->
            fail("Scanner should not return any devices")
        }

        scanner.start()

        endedLatch.await()

        val error = UUAssert.unwrap(errorContainer.load())
        assertEquals(UUBluetoothErrorCode.INSUFFICIENT_PERMISSIONS.rawValue, error.code)

    }
}