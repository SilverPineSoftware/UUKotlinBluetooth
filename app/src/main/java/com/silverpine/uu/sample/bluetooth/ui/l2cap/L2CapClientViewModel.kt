package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.silverpine.uu.bluetooth.UUL2CapChannel
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuToHexData
import com.silverpine.uu.logging.UULog

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class L2CapClientViewModel: ViewModel()
{
    private var _output: MutableLiveData<String> = MutableLiveData("")
    val output: LiveData<String> = _output

    private lateinit var peripheral: UUPeripheral
    private lateinit var channel: UUL2CapChannel

    fun update(peripheral: UUPeripheral)
    {
        this.peripheral = peripheral
        this.channel = UUL2CapChannel(peripheral)
        appendOutput("Tap Connect to begin")
    }

    fun onConnect()
    {
        appendOutput("Getting L2Cap Socket")

        val psm = peripheral.name?.replace("L2CapServer-", "")?.toInt() ?: 0
        val secure = false
        val timeout = 10000L
        appendOutput("Starting L2CapChannel to PSM $psm, secure: $secure, timeout: $timeout")
        channel.connect(psm, secure, timeout)
        { err ->
            appendOutput("Connection Complete, Error: $err")
        }
    }

    fun onPing()
    {
        val tx = "57575757".uuToHexData() ?: return

        appendOutput("TX: ${tx.uuToHex()}")

        appendOutput("Writing data...")
        channel.write(tx, 10000L)
        { txErr ->
            appendOutput("TX Complete, err: $txErr")

            appendOutput("Reading data...")
            channel.read(10000L)
            { rx, rxErr ->

                appendOutput("RX Complete, ${rx?.uuToHex()}, err: $rxErr")
            }
        }
    }

    private fun appendOutput(line: String)
    {
        uuDispatchMain()
        {
            _output.value += "\n$line"
            UULog.d(javaClass, "outputLog", line)
        }
    }
}