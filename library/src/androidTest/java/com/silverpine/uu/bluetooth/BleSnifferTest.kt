package com.silverpine.uu.bluetooth

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuFormatDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.Date

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class BleSnifferTest: BaseTest()
{
    @Test
    fun runBleSniffer() = runBlocking()
    {
        val timeout = 300 * UUDate.MILLIS_IN_ONE_SECOND

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


        val filename = "sniff_results_${Date(System.currentTimeMillis()).uuFormatDate(UUDate.EXTENDED_FILE_NAME_FORMAT)}.csv"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use()
        {
            it.write(csvBytes)
        }
    }
}
