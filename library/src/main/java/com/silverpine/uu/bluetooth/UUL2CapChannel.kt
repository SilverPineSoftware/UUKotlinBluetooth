package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.UUWorkerThread
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
class UUL2CapChannel(private val peripheral: UUPeripheral)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG
    }

    private enum class TimerBuckets
    {
        ConnectWatchdog,
        WriteWatchdog,
        ReadWatchdog
    }

    private val workerThread = UUWorkerThread("UUL2CapChannel_${peripheral.address}")
    private var socket: BluetoothSocket? = null

    val isConnected: Boolean
        get()
        {
            return (socket?.isConnected == true)
        }

    fun connect(
        psm: Int,
        secure: Boolean,
        timeout: Long,
        completion: (UUError?) -> Unit)
    {
        workerThread.post()
        {
            val timerId = timerId(TimerBuckets.ConnectWatchdog)

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                if (LOGGING_ENABLED)
                {
                    debugLog("connect", "L2Cap Connect Timed Out: $peripheral, psm: $psm, secure: $secure")
                }
                notifyConnectComplete(UUBluetoothError.timeoutError(), completion)
            }

            var err: UUError? = null

            try
            {
                val sock = if (secure)
                {
                    peripheral.bluetoothDevice.createL2capChannel(psm)
                }
                else
                {
                    peripheral.bluetoothDevice.createInsecureL2capChannel(psm)
                }

                sock.connect()
                socket = sock
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)

                if (LOGGING_ENABLED)
                {
                    logException("connect", ex)
                }
            }

            UUTimer.cancelActiveTimer(timerId)
            notifyConnectComplete(err, completion)
        }
    }

    fun disconnect(completion: (UUError?)->Unit)
    {
        workerThread.post()
        {
            var err: UUError? = null

            try
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("disconnect", "Disconnecting L2CapChannel")
                }

                socket?.inputStream?.uuSafeClose()
                socket?.outputStream?.uuSafeClose()
                socket?.uuSafeClose()
                socket = null
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)

                if (LOGGING_ENABLED)
                {
                    logException("disconnect", ex)
                }
            }

            notifyDisconnectComplete(err, completion)
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
                notifyWriteComplete(err, completion)
                return@post
            }

            if (!sock.isConnected)
            {
                val err = UUBluetoothError.preconditionFailedError("socket.isConnected is false")
                notifyWriteComplete(err, completion)
                return@post
            }

            val outputStream = sock.outputStream ?: run()
            {
                val err = UUBluetoothError.preconditionFailedError("socket.outputStream is null")
                notifyWriteComplete(err, completion)
                return@post
            }

            val timerId = timerId(TimerBuckets.WriteWatchdog)

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                if (LOGGING_ENABLED)
                {
                    debugLog("write", "L2Cap Write Data Timed Out: $peripheral")
                }

                notifyWriteComplete(UUBluetoothError.timeoutError(), completion)
            }

            var err: UUError? = null

            try
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("write", "TX: ${data.uuToHex()}")
                }

                outputStream.write(data)
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)

                if (LOGGING_ENABLED)
                {
                    logException("write", ex)
                }
            }

            UUTimer.cancelActiveTimer(timerId)
            notifyWriteComplete(err, completion)
        }
    }

    fun read(
        timeout: Long,
        expectedBytes: Int? = null,
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

            val inputStream = sock.inputStream ?: run()
            {
                val err = UUBluetoothError.preconditionFailedError("socket.inputStream is null")
                notifyReadComplete(null, err, completion)
                return@post
            }

            val timerId = timerId(TimerBuckets.ReadWatchdog)

            val readThread = UUStreamReadThread("UUL2CapChannelReadThread", 1024, inputStream, expectedBytes)
            { rx, err ->
                UUTimer.cancelActiveTimer(timerId)
                notifyReadComplete(rx,err, completion)
                false
            }

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                if (LOGGING_ENABLED)
                {
                    debugLog("read", "L2Cap Read Data Timed Out: $peripheral")
                }

                readThread.interrupt()
                notifyReadComplete(null, UUBluetoothError.timeoutError(), completion)
            }

            readThread.start()
        }
    }

    fun sendCommand(
        command: ByteArray,
        sendTimeout: Long,
        receiveTimeout: Long,
        expectedBytes: Int,
        completion: (ByteArray?, UUError?)->Unit)
    {
        write(command, sendTimeout)
        { txErr ->

            if (txErr != null)
            {
                completion(null, txErr)
                return@write
            }

            read(receiveTimeout, expectedBytes, completion)
        }
    }

    private fun notifyConnectComplete(error: UUError?, completion: (UUError?)->Unit)
    {
        workerThread.post()
        {
            completion(error)
        }
    }

    private fun notifyDisconnectComplete(error: UUError?, completion: (UUError?) -> Unit)
    {
        workerThread.post()
        {
            completion(error)
        }
    }

    private fun notifyWriteComplete(error: UUError?, completion: (UUError?) -> Unit)
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
            completion(data, error)
        }
    }

    private fun timerId(bucket: TimerBuckets): String
    {
        return String.format(
            Locale.US,
            "%s__l2cap_%s",
            peripheral.address,
            bucket
        )
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