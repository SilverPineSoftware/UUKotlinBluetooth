package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.logging.UULog
import java.io.InputStream

open class UUStreamReadThread(
    name: String = "UUStreamReadThread",
    private val readChunkSize: Int = 1024,
    private val inputStream: InputStream,
    private val expectedBytes: Int?,
    private val dataReceived: (ByteArray?, UUError?)->Boolean): Thread(name)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG
    }

    private var totalReceived: Long = 0

    override fun run()
    {
        try
        {
            while (!isInterrupted)
            {
                val rx = receiveBytes()
                if ((rx?.size ?: 0) > 0)
                {
                    var shouldInterrupt = !dataReceived(rx, null)

                    if (expectedBytes != null)
                    {
                        if (totalReceived >= expectedBytes)
                        {
                            shouldInterrupt = true
                        }
                    }

                    if (shouldInterrupt)
                    {
                        interrupt()
                    }
                }
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                logException("run", ex)
            }
        }
    }

    private fun receiveBytes(): ByteArray?
    {
        val rxChunk = ByteArray(readChunkSize)
        var rx: ByteArray? = null

        try
        {
            val bytesRead = inputStream.read(rxChunk, 0, rxChunk.size)
            if (bytesRead > 0)
            {
                rx = rxChunk.uuSubData(0, bytesRead)

                totalReceived += bytesRead

                if (LOGGING_ENABLED)
                {
                    debugLog("receiveBytes", "Received $bytesRead, totalReceived: $totalReceived")
                }
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                logException("receiveBytes", ex)
            }
        }

        return rx
    }

    private fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, message)
        }
    }

    private fun logException(method: String, exception: Throwable)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, "", exception)
        }
    }
}