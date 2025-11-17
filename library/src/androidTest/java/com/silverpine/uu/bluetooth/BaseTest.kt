package com.silverpine.uu.bluetooth

import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.test.instrumented.UUTestPermissions
import com.silverpine.uu.test.instrumented.annotations.UUInteractionRequired
import com.silverpine.uu.test.instrumented.uuAppendOutputLine
import com.silverpine.uu.test.instrumented.uuSetTestName
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@UUInteractionRequired
open class BaseTest
{
    @Rule @JvmField
    val activityScenarioRule = ActivityScenarioRule(UUBleTestActivity::class.java)

    protected val context = InstrumentationRegistry.getInstrumentation().targetContext

    protected suspend fun startTest(testName: String)
    {
        appendOutputLine("Starting test")

        activityScenarioRule.uuSetTestName(testName)

        appendOutputLine("Acquiring BLE permissions")
        requestBluetoothPermissions() //activityScenarioRule)
    }

    protected fun appendOutputLine(line: String)
    {
        log("OUTPUT: $line")

        activityScenarioRule.uuAppendOutputLine(line)
    }

    protected fun log(text: String)
    {
        Log.d(javaClass.name, text)
    }

    protected fun testWait(seconds: Long)
    {
        Thread.sleep(seconds * 1000)
    }

    private fun requestBluetoothPermissions()
    {
        UUTestPermissions.grantBlePermissions()
    }

    @OptIn(ExperimentalAtomicApi::class)
    protected fun scanOnceForPeripheral(filter: (UUPeripheral)-> Boolean): UUPeripheral?
    {
        val latch = CountDownLatch(1)
        val result = AtomicReference<UUPeripheral?>(null)

        val scanner = UUBluetooth.scanner

        scanner.listChanged =
        { scanner, peripherals ->

            val found = peripherals.firstOrNull { filter(it) == true }
            if (found != null)
            {
                result.store(found)
                scanner.stop()
            }
        }

        scanner.ended =
        { scanner, error ->
            latch.countDown()
        }

        scanner.start()

        latch.await(30, TimeUnit.SECONDS)

        return result.load()
    }
}
