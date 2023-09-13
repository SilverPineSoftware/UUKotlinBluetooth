package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuToHexData

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class L2CapClientViewModel: ViewModel()
{
    private var _output: MutableLiveData<String> = MutableLiveData("")
    val output: LiveData<String> = _output

    private lateinit var peripheral: UUPeripheral
    private var bluetoothSocket: BluetoothSocket? = null

    fun update(peripheral: UUPeripheral)
    {
        this.peripheral = peripheral
        appendOutput("Tap Connect to begin")
    }

    fun onConnect()
    {
        appendOutput("Getting L2Cap Socket")

        val psm = peripheral.name?.replace("L2CapServer-", "")?.toInt() ?: 0
        appendOutput("Connecting to PSM $psm")
        bluetoothSocket = peripheral.connectL2Cap(psm, false)

        appendOutput("Starting L2Cap Connection")

        try
        {
            bluetoothSocket?.connect()
        }
        catch (ex: Exception)
        {
            appendOutput("Connection Failed: $ex")
            bluetoothSocket = null
        }
    }

    fun onPing()
    {
        val tx = "57575757"
        appendOutput("TX: $tx")
        bluetoothSocket?.outputStream?.write(tx.uuToHexData())

        val available = bluetoothSocket?.inputStream?.available()
        appendOutput("There are $available bytes to read.")

        val rxBuffer = ByteArray(1024)
        val bytesRead = bluetoothSocket?.inputStream?.read(rxBuffer, 0, rxBuffer.size) ?: 0

        appendOutput("Read $bytesRead bytes")

        val rx = rxBuffer.uuSubData(0, bytesRead)
        appendOutput("RX: ${rx?.uuToHex()}")
    }

    private fun appendOutput(line: String)
    {
        uuDispatchMain()
        {
            _output.value += "\n$line"
        }
    }
}