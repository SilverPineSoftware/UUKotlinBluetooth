package com.silverpine.uu.bluetooth

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuFormatAsExtendedFileName
import com.silverpine.uu.logging.UUConsoleLogWriter
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogLevel
import com.silverpine.uu.logging.UULogger
import com.silverpine.uu.test.instrumented.annotations.UUInteractionRequired
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@UUInteractionRequired
class BleSnifferTest: BaseTest()
{
    @Test
    fun runBleSniffer() = runBlocking()
    {
        val logger = UULogger(UUConsoleLogWriter())
        logger.logLevel = UULogLevel.VERBOSE
        UULog.setLogger(logger)

        UUBluetooth.init(InstrumentationRegistry.getInstrumentation().targetContext)
        
        val timeout = 20 * UUDate.Constants.MILLIS_IN_ONE_SECOND

        startTest("BLE Sniffer Test")

        val job = Job()
        val sniffer = UUBluetoothSniffer(context)

        appendOutputLine("Starting sniffer, timeout: ${timeout / 1000.0f}")
        sniffer.start()

        UUTimer.startTimer("testTimer", timeout, null)
        { _, _ ->
            appendOutputLine("Test complete")
            job.complete()
        }

        job.join()

        appendOutputLine("Stopping sniffer")
        val summary = sniffer.stop()
        appendOutputLine("Captured ${summary.results.size} results")

        val csvBytes = summary.toCsvBytes() ?: return@runBlocking


        val filename = "sniff_results_${System.currentTimeMillis().uuFormatAsExtendedFileName()}.csv"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use()
        {
            it.write(csvBytes)
        }
    }
}
