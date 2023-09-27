package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.os.Build
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothAdvertiser
import com.silverpine.uu.bluetooth.UUL2CapServer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.ux.UUMenuItem
import java.util.UUID

class L2CapServerViewModel: L2CapBaseViewModel()
{
    private val echoServer: UUL2CapServer? by lazy()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            val server = UUL2CapServer()
            server.dataReceived =
            { rx ->

                appendOutput("Received ${rx.size} bytes")
                appendOutput("RX: ${rx.uuToHex()}")

                appendOutput("TX: ${rx.uuToHex()}")
                rx
            }

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
            appendOutput("Tap Start to begin")
        }

        updateMenu()
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

    private fun onStart()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            echoServer?.start(false)
            { psm, error ->

                if (error != null)
                {
                    appendOutput("start failed with an error: $error")
                    return@start
                }

                appendOutput("Server started, PSM: $psm")
                psm?.let()
                { actualPsm ->
                    advertise(actualPsm)
                }

                updateMenu()
            }
        }
    }

    private fun onStop()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            appendOutput("Stopping server")
            echoServer?.stop()
            updateMenu()
        }
    }

    override fun buildMenu(): ArrayList<UUMenuItem>
    {
        val list = ArrayList<UUMenuItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            echoServer?.let()
            { server ->
                if (server.isRunning)
                {
                    list.add(UUMenuItem("Stop", this::onStop))
                }
                else
                {
                    list.add(UUMenuItem("Start", this::onStart))
                }
            }
        }

        return list
    }
}


/*

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
}*/