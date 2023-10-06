package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothServerSocket
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSafeClose

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
class UUL2CapServer: UUL2CapChannel()
{
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var connectionListenerThread: UUBluetoothSocketAcceptThread? = null
    private var readThread: UUStreamReadThread? = null

    var clientConnected: ()->Unit = { }

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
            logException("start", ex)
            error = UUBluetoothError.operationFailedError(ex)
        }

        completion(psm, error)
    }

    fun stop()
    {
        disconnect {  }
        stopReading()
        stopAcceptingSocketConnections()

        bluetoothServerSocket?.uuSafeClose()
        bluetoothServerSocket = null
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
                debugLog("startAcceptingSocketConnections", "bluetoothServerSocket is null!")
            }
        }
        catch (ex: Exception)
        {
            logException("startListeningForSocketConnections", ex)
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
            logException("stopListeningForSocketConnections", ex)
        }
        finally
        {
            connectionListenerThread = null
        }
    }

    inner class UUBluetoothSocketAcceptThread(private val serverSocket: BluetoothServerSocket): Thread("UUBluetoothSocketAcceptThread")
    {
        override fun run()
        {
            try
            {
                debugLog("UUBluetoothSocketAcceptThread.run", "Accepting socket connections...")
                socket = serverSocket.accept()

                debugLog("UUBluetoothSocketAcceptThread.run", "Server socket connected")
                clientConnected()
            }
            catch (ex: Exception)
            {
                logException("UUBluetoothSocketAcceptThread.run", ex)
            }
        }
    }
}