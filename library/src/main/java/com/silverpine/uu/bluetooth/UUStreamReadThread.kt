package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.io.ByteArrayOutputStream
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

    private var interrupted: Boolean = false

    override fun run()
    {
        try
        {
            var keepLooping = true

            while (!interrupted && keepLooping)
            {
                keepLooping = receiveBytes()
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

    private fun receiveBytes(): Boolean
    {
        val rxChunk = ByteArray(readChunkSize)
        val bos = ByteArrayOutputStream()
        var rx: ByteArray? = null
        var err: UUError? = null

        try
        {
            var bytesRead: Int
            var totalBytesRead = 0

            sleepUntilDataAvailable()

            while (!interrupted && inputStream.available() > 0)
            {
                bytesRead = inputStream.read(rxChunk, 0, rxChunk.size)

                if (LOGGING_ENABLED)
                {
                    debugLog("receiveBytes", "Read Chunk: $bytesRead")
                }

                if (bytesRead > 0)
                {
                    bos.write(rxChunk, 0, bytesRead)
                }

                totalBytesRead += bytesRead
                if (expectedBytes != null)
                {
                    if (totalBytesRead >= expectedBytes)
                    {
                        break
                    }
                }
            }

            rx = bos.toByteArray()

            if (LOGGING_ENABLED)
            {
                debugLog("receiveBytes", "RX: ${rx.uuToHex()}")
            }
        }
        catch (ex: Exception)
        {
            err = UUBluetoothError.operationFailedError(ex)

            if (LOGGING_ENABLED)
            {
                logException("read", ex)
            }
        }

        if (interrupted)
        {
            return false
        }

        return dataReceived(rx, err)
    }

    private fun sleepUntilDataAvailable()
    {
        do
        {
            uuSleep("sleepUntilDataAvailable", 10L)
        }
        while (!interrupted && inputStream.available() == 0)
    }

    fun uuInterrupt()
    {
        try
        {
            interrupted = true
            interrupt()
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                logException("uuInterrupt", ex)
            }
        }
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