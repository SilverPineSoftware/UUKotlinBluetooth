package com.silverpine.uu.bluetooth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.bluetooth.internal.UUBlePeripheralScanner
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.logging.UUConsoleLogger
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class BleScannerTest: BaseTest()
{
    @Test
    fun runScannerTest() = runBlocking()
    {
        UULog.init(UUConsoleLogger())

        val timeout = 20 * UUDate.Constants.millisInOneSecond

        startTest("BLE Scanner Test")

        val job = Job()
        val scanner = UUBlePeripheralScanner(context)

        appendOutputLine("Starting scanner, timeout: ${timeout / 1000.0f}")

        val config = UUPeripheralScannerConfig()
        scanner.config = config
        scanner.started =
        { scanner ->
            appendOutputLine("Scan started")
        }

        scanner.ended =
        { scanner, error ->
            appendOutputLine("Scan ended, error: $error")
        }

        scanner.listChanged =
        { scanner, list ->
            appendOutputLine("${list.size} nearby devices")
        }

        scanner.start()

        UUTimer.startTimer("testTimer", timeout, null)
        { _, _ ->
            appendOutputLine("Test complete")
            job.complete()
        }

        job.join()

        appendOutputLine("Stopping scanner")
        scanner.stop()
    }
}
