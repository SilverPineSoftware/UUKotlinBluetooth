package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.logging.UULog
import java.io.InputStream

open class UUStreamReadThread(
    name: String = "UUStreamReadThread",
    private val readChunkSize: Int = 1024,
    private val socket: BluetoothSocket,
    private val dataReceived: (ByteArray)->Unit): Thread(name)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG
    }

    private var inputStream: InputStream? = null
    private val rxChunk = ByteArray(readChunkSize)
    private var totalReceived: Long = 0

    override fun run()
    {
        try
        {
            while (!isInterrupted)
            {
                val rx = receiveBytes()
                rx?.let()
                {
                    dataReceived(it)
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

    @SuppressLint("MissingPermission")
    private fun receiveBytes(): ByteArray?
    {
        var rx: ByteArray? = null

        try
        {
            if (!socket.isConnected)
            {
                debugLog("receiveBytes", "Socket is NOT Connected! Attempting to reconnect")
                socket.connect()

                val sleepTime = 1000L
                debugLog("receiveBytes", "Sleeping $sleepTime after socket connect")
                sleep(sleepTime)
                return null
            }

            val inputStream = socket.inputStream ?: run()
            {
                debugLog("receiveBytes", "Socket input stream is null! Disconnecting and reconnecting!")
                socket.uuSafeClose()


                val sleepTime = 1000L
                debugLog("receiveBytes", "Sleeping $sleepTime after socket close")
                sleep(sleepTime)
                return null
            }

            val bytesRead = inputStream.read(rxChunk, 0, readChunkSize)
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
            logException("receiveBytes", ex)

            val sleepTime = 1000L
            debugLog("receiveBytes", "Sleeping $sleepTime after catching exception")
            sleep(sleepTime)

            socket.uuSafeClose()
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