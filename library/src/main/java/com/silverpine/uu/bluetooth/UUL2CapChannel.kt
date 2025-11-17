package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.UUWorkerThread
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.logException
import java.util.Locale

private const val LOG_TAG = "UUL2CapChannel"

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
open class UUL2CapChannel
{
    companion object
    {
        private val LOGGING_ENABLED = true // BuildConfig.DEBUG
        private val DATA_LOGGING_ENABLED = true //BuildConfig.DEBUG

        const val READ_WATCHDOG_TIMER_ID = "ReadWatchdog"
        const val WRITE_WATCHDOG_TIMER_ID = "WriteWatchdog"
    }

    protected val id = UURandom.uuid()

    protected val workerThread = UUWorkerThread("UUL2CapChannel_$id")
    protected var socket: BluetoothSocket? = null
    //private var readOnceThread: ReadThread? = null
    private var readThread: UUStreamReadThread? = null
    var dataReceived: ((ByteArray)->Unit)? = null
    var readChunkSize: Int = 10240

    val isConnected: Boolean
        get()
        {
            return (socket?.isConnected == true)
        }

    fun disconnect(completion: (UUError?)->Unit)
    {
        stopReading()

        workerThread.post()
        {
            var err: UUError? = null

            try
            {
                debugLog("disconnect", "Disconnecting L2CapChannel")

                socket?.inputStream?.uuSafeClose()
                socket?.outputStream?.uuSafeClose()
                socket?.uuSafeClose()
                socket = null
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)
                logException("disconnect", ex)
            }

            notifyCallback(err, completion)
        }
    }

    fun write(
        data: ByteArray,
        timeout: Long,
        completion: (UUError?)->Unit)
    {
        workerThread.post()
        {
            val sock = socket ?: run()
            {
                val err = UUBluetoothError.preconditionFailedError("socket is null")
                notifyCallback(err, completion)
                return@post
            }

            if (!sock.isConnected)
            {
                val err = UUBluetoothError.preconditionFailedError("socket.isConnected is false")
                notifyCallback(err, completion)
                return@post
            }

            val outputStream = sock.outputStream ?: run()
            {
                val err = UUBluetoothError.preconditionFailedError("socket.outputStream is null")
                notifyCallback(err, completion)
                return@post
            }

            val timerId = timerId(WRITE_WATCHDOG_TIMER_ID)

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog("write", "L2Cap Write timed out.")
                notifyCallback(UUBluetoothError.timeoutError(), completion)
            }

            var err: UUError? = null

            try
            {
                debugLogData("write", "TX", data)
                outputStream.write(data)
                outputStream.flush()
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)
                logException("write", ex)
                reconnect()
            }

            UUTimer.cancelActiveTimer(timerId)
            notifyCallback(err, completion)
        }
    }

    private fun reconnect()
    {
        try
        {
            socket?.uuSafeClose()
            socket?.connect()
        }
        catch (ex: Exception)
        {
            logException("reconnect", ex)
        }
    }

    fun read(
        timeout: Long,
        expectedBytes: Int,
        completion: (ByteArray?, UUError?)->Unit)
    {
        workerThread.post()
        {
            val sock = socket ?: run()
            {
                val err = UUBluetoothError.preconditionFailedError("socket is null")
                notifyReadComplete(null, err, completion)
                return@post
            }

            if (!sock.isConnected)
            {
                val err = UUBluetoothError.preconditionFailedError("socket.isConnected is false")
                notifyReadComplete(null, err, completion)
                return@post
            }

//            val inputStream = sock.inputStream ?: run()
//            {
//                val err = UUBluetoothError.preconditionFailedError("socket.inputStream is null")
//                notifyReadComplete(null, err, completion)
//                return@post
//            }

//            if (readOnceThread != null)
//            {
//                val err = UUBluetoothError.preconditionFailedError("readOnceThread is not null")
//                notifyReadComplete(null, err, completion)
//                return@post
//            }

//            if (readThread != null)
//            {
//                val err = UUBluetoothError.preconditionFailedError("readThread is not null")
//                notifyReadComplete(null, err, completion)
//                return@post
//            }

            val timerId = timerId(READ_WATCHDOG_TIMER_ID)

            val t = UUStreamReadThread("UUL2CapChannel", expectedBytes, sock)

            t.dataReceived =
            { rx ->
                t.interrupt()
                UUTimer.cancelActiveTimer(timerId)
                debugLogData("read", "RX", rx)
                notifyReadComplete(rx, null, completion)
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog("read", "L2Cap Read timed out.")
                t.interrupt()
                notifyReadComplete(null, UUBluetoothError.timeoutError(), completion)
            }

            t.start()
        }
    }

    fun startReading(): UUError?
    {
        val sock = socket ?: run()
        {
            return UUBluetoothError.preconditionFailedError("socket is null")
        }

        val t = UUStreamReadThread("UUL2CapChannel", readChunkSize, sock)
        t.dataReceived = dataReceived
        t.start()
        readThread = t
        return null
    }

    fun stopReading()
    {
        try
        {
            readThread?.interrupt()
        }
        catch (ex: Exception)
        {
            logException("stopReading", ex)
        }
        finally
        {
            readThread = null
        }
    }

    protected fun notifyCallback(error: UUError?, completion: (UUError?) -> Unit)
    {
        workerThread.post()
        {
            completion(error)
        }
    }

    private fun notifyReadComplete(data: ByteArray?, error: UUError?, completion: (ByteArray?, UUError?) -> Unit)
    {
        workerThread.post()
        {
            readThread = null
            completion(data, error)
        }
    }

    protected fun timerId(bucket: String): String
    {
        return String.format(Locale.US, "UUL2CapChannel_%s_%s", id, bucket)
    }

    protected fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.debug(LOG_TAG, "$method: $message")
        }
    }

    protected fun debugLogData(method: String, tag: String, data: ByteArray)
    {
        if (DATA_LOGGING_ENABLED)
        {
            UULog.debug(LOG_TAG, "$method, $tag: ${data.uuToHex()}")
        }
    }

    protected fun logException(method: String, exception: Exception)
    {
        if (LOGGING_ENABLED)
        {
            UULog.logException(LOG_TAG, method, exception)
        }
    }

    /*
    inner class ReadThread(chunkSize: Int, private val inputStream: InputStream): Thread("UUL2CapReadThread_$id")
    {
        private val rxChunk = ByteArray(chunkSize)
        private var totalReceived: Long = 0
        private var readCompletion: (ByteArray)->Unit = { }
        private var countToRead: Int = 0

        fun read(count: Int, completion: (ByteArray)->Unit)
        {
            countToRead = count
            readCompletion = completion
            start()
        }

        override fun run()
        {
            var rx = byteArrayOf()
            val bos = ByteArrayOutputStream()

            try
            {
                while (!isInterrupted && bos.size() < countToRead)
                {
                    val bytesRead = inputStream.read(rxChunk, 0, rxChunk.size)
                    if (bytesRead > 0)
                    {
                        bos.write(rxChunk, 0, bytesRead)

                        totalReceived += bytesRead
                        debugLog("run", "Received $bytesRead, totalReceived: $totalReceived")
                    }
                }

                rx = bos.toByteArray()
            }
            catch (ex: Exception)
            {
                logException("run", ex)
            }
            finally
            {
                bos.uuSafeClose()
            }

            readCompletion(rx)
        }
    }

     */
}