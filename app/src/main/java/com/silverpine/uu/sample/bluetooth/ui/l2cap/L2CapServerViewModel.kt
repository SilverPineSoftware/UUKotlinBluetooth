package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothAdvertiser
import com.silverpine.uu.core.UUResources
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import java.util.UUID

class L2CapServerViewModel: ViewModel()
{
    private var _output: MutableLiveData<String> = MutableLiveData("")
    val output: LiveData<String> = _output

    private val echoServer: BleEchoServer? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            val server = BleEchoServer()

            server.onPsmChanged =
            { psm ->
                advertise(psm)
            }

            server.onLog = this::appendOutput

            server
        }
        else
        {
            null
        }
    }

    private var advertiser = UUBluetoothAdvertiser(UUBluetooth.requireApplicationContext())

    var checkPermissions: ((()->Unit)->Unit) = { }

    fun reset()
    {
        if (echoServer == null)
        {
            appendOutput("This device does not support L2Cap")
        }
        else
        {
            appendOutput("Tap Listen to begin")
        }
    }

    private fun advertise(psm: Int)
    {
        appendOutput("Acquiring BLE Advertise permissions")

        uuDispatchMain()
        {
            checkPermissions()
            {
                try
                {
                    uuDispatchMain()
                    {
                        appendOutput("Starting BLE advertising")

                        advertiser.stop()
                        advertiser.start(UUID.randomUUID(), "L2CapServer-$psm", 160)
                    }
                }
                catch (ex: Exception)
                {
                    appendOutput("Caught an Exception: $ex")
                }
            }
        }
    }

    fun onStartListening()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            echoServer?.start(false)
        }
    }

    private fun appendOutput(@StringRes resourceId: Int)
    {
        appendOutput(UUResources.getString(resourceId))
    }

    private fun appendOutput(line: String)
    {
        uuDispatchMain()
        {
            _output.value += "\n$line"
            UULog.d(javaClass, "output", line)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
class BleEchoServer
{
    private lateinit var bluetoothServerSocket: BluetoothServerSocket
    private lateinit var thread: BleThread
    private val connectionThreads: ArrayList<BleConnectionThread> = arrayListOf()

    var onPsmChanged: (Int)->Unit = { }
    var onLog: (String)->Unit = { }

    fun start(secure: Boolean)
    {
        thread = BleThread(secure)
        thread.start()
    }

    private fun log(method: String, message: String)
    {
        UULog.d(javaClass, method, message)
        onLog(message)
    }

    private fun log(method: String, exception: Exception)
    {
        UULog.d(javaClass, method, "", exception)
        onLog("Caught Exception in $method: $exception")
    }

    inner class BleThread(private val secure: Boolean): Thread("BleEchoServer")
    {
        override fun run()
        {
            try
            {
                log("run", "Getting L2Cap Server socket")

                bluetoothServerSocket = UUBluetooth.listenForL2CapConnection(secure)
                onPsmChanged(bluetoothServerSocket.psm)

                while (true)
                {
                    log("run", "Listening for incoming L2Cap connections, PSM: ${bluetoothServerSocket.psm}")
                    val socket = bluetoothServerSocket.accept()

                    val thread = BleConnectionThread(socket)
                    thread.start()
                }
            }
            catch (ex: Exception)
            {
                log( "run", ex)
            }
        }
    }

    inner class BleConnectionThread(private val socket: BluetoothSocket): Thread("BleConnectionThread")
    {
        override fun run()
        {
            try
            {
                synchronized(connectionThreads)
                {
                    connectionThreads.add(this)
                }

                while (true)
                {
                    log( "handleConnection", "Waiting for incoming data")
                    val rxBuffer = ByteArray(1024)
                    val bytesRead = socket.inputStream?.read(rxBuffer, 0, rxBuffer.size) ?: 0

                    log( "handleConnection", "Got $bytesRead bytes.")
                    val rx = rxBuffer.uuSubData(0, bytesRead)
                    log( "handleConnection", "RX: ${rx?.uuToHex()}")

                    log( "handleConnection", "Echoing bytes back to sender")
                    socket.outputStream?.write(rx)
                }
            }
            catch (ex: Exception)
            {
                log( "run", ex)
            }
            finally
            {
                synchronized(connectionThreads)
                {
                    connectionThreads.remove(this)
                }
            }
        }
    }
}