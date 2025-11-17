package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.logException

private const val LOG_TAG = "UUStreamReadThread"

open class UUStreamReadThread(
    name: String = "UUStreamReadThread",
    private val readChunkSize: Int = 1024,
    private val socket: BluetoothSocket): Thread(name)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG
    }

    //private var inputStream: InputStream? = null
    private val rxChunk = ByteArray(readChunkSize)
    private var totalReceived: Long = 0

    var dataReceived: ((ByteArray)->Unit)? = null

    override fun run()
    {
        try
        {
            while (!isInterrupted)
            {
                val rx = receiveBytes()
                notifyDataReceived(rx)
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                logException("run", ex)
            }
        }
        finally
        {
            dataReceived = null
        }
    }

    private fun notifyDataReceived(data: ByteArray?)
    {
        try
        {
            if (isInterrupted)
            {
                debugLog("notifyDataReceived", "Thread has been interrupted, do not notify data received")
                return;
            }

            val block = dataReceived
            if (block == null)
            {
                debugLog("notifyDataReceived", "DataReceived callback is null, nothing to do!")
                return;
            }

            if (data == null)
            {
                debugLog("notifyDataReceived", "data is null, nothing to do!")
                return;
            }

            block(data)
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
            UULog.debug(LOG_TAG, "$method, $message")
        }
    }

    private fun logException(method: String, exception: Exception)
    {
        if (LOGGING_ENABLED)
        {
            UULog.logException(LOG_TAG, method, exception)
        }
    }
}