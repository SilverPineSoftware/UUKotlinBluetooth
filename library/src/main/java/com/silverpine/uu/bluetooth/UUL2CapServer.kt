package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothServerSocket
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.logging.UULog

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
class UUL2CapServer(val readChunkSize: Int = 10240)
{
    companion object
    {
        private val LOGGING_ENABLED = BuildConfig.DEBUG
    }

    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var connectionListenerThread: UUBluetoothSocketAcceptThread? = null
    private var readThread: UUStreamReadThread? = null

    var dataReceived: (ByteArray)->ByteArray? = { null }

    val isRunning: Boolean
        get()
        {
            return (bluetoothServerSocket != null)
        }

    fun start(secure: Boolean, completion: (Int?, UUError?)->Unit)
    {
        var psm: Int? = null
        var error: UUError? = null

        try
        {
            bluetoothServerSocket = UUBluetooth.listenForL2CapConnection(secure)
            psm = bluetoothServerSocket?.psm
            startAcceptingSocketConnections()
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("start", ex)
            }

            error = UUBluetoothError.operationFailedError(ex)
        }

        completion(psm, error)
    }

    fun stop()
    {
        stopReading()
        stopAcceptingSocketConnections()

        bluetoothServerSocket?.uuSafeClose()
        bluetoothServerSocket = null
    }

    private fun debugLog(method: String, message: String)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, message)
        }
    }

    private fun debugLog(method: String, exception: Exception)
    {
        if (LOGGING_ENABLED)
        {
            UULog.d(javaClass, method, "", exception)
        }
    }

    private fun startAcceptingSocketConnections()
    {
        stopAcceptingSocketConnections()
        stopReading()

        try
        {
            bluetoothServerSocket?.let()
            { sock ->
                val thread = UUBluetoothSocketAcceptThread(sock)
                thread.start()
                connectionListenerThread = thread
            } ?: run()
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("startAcceptingSocketConnections", "bluetoothServerSocket is null!")
                }
            }
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("startListeningForSocketConnections", ex)
            }
        }
        finally
        {
            connectionListenerThread = null
        }
    }

    private fun stopAcceptingSocketConnections()
    {
        try
        {
            connectionListenerThread?.interrupt()
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("stopListeningForSocketConnections", ex)
            }
        }
        finally
        {
            connectionListenerThread = null
        }
    }

    private fun stopReading()
    {
        try
        {
            readThread?.interrupt()
        }
        catch (ex: Exception)
        {
            if (LOGGING_ENABLED)
            {
                debugLog("stopReading", ex)
            }
        }
        finally
        {
            readThread = null
        }
    }

    inner class UUBluetoothSocketAcceptThread(private val serverSocket: BluetoothServerSocket): Thread("UUBluetoothSocketAcceptThread")
    {
        override fun run()
        {
            try
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("UUBluetoothSocketAcceptThread.run", "Accepting socket connections...")
                }

                val socket = serverSocket.accept()

                if (LOGGING_ENABLED)
                {
                    debugLog("UUBluetoothSocketAcceptThread.run", "Server socket connected")
                }

                val t = UUStreamReadThread("UUL2CapServer", readChunkSize, socket.inputStream, null)
                { rx, err ->

                    if (err != null)
                    {
                        socket?.uuSafeClose()
                        startAcceptingSocketConnections()
                        return@UUStreamReadThread false
                    }

                    rx?.let()
                    { rxBytes ->
                        val txBytes = dataReceived(rxBytes)
                        txBytes?.let()
                        { tx ->
                            socket.outputStream.write(tx, 0, tx.size)
                        }
                    }
                    true
                }

                t.start()
                readThread = t
            }
            catch (ex: Exception)
            {
                if (LOGGING_ENABLED)
                {
                    debugLog("UUBluetoothSocketAcceptThread.run", ex)
                }
            }
        }
    }
}