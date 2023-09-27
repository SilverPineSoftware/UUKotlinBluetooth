package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.bluetooth.UUL2CapChannel
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuWriteInt32
import com.silverpine.uu.ux.UUMenuItem
import java.nio.ByteOrder

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class L2CapClientViewModel: L2CapBaseViewModel()
{
    private lateinit var peripheral: UUPeripheral
    private lateinit var channel: UUL2CapChannel
    private var pingCount: Int = 0

    fun update(peripheral: UUPeripheral)
    {
        this.peripheral = peripheral
        this.channel = UUL2CapChannel(peripheral)
        updateMenu()
        appendOutput("Tap Connect to begin")
    }

    fun onConnect()
    {
        val psm = peripheral.name?.replace("L2CapServer-", "")?.toInt() ?: 0
        val secure = false
        val timeout = 10000L

        appendOutput("Connecting to L2CapChannel with PSM $psm, secure: $secure, timeout: $timeout")
        channel.connect(psm, secure, timeout)
        { err ->
            appendOutput("Connection Complete, Error: $err")
            updateMenu()
        }
    }

    fun onDisconnect()
    {
        appendOutput("Disconnecting from L2Cap")
        channel.disconnect()
        {err ->
            appendOutput("Disconnect Complete, Error: $err")
            updateMenu()
        }
    }

    private fun onPing()
    {
        val tx = ByteArray(Int.SIZE_BYTES)
        tx.uuWriteInt32(ByteOrder.BIG_ENDIAN, 0, pingCount)

        appendOutput("TX: ${tx.uuToHex()}")

        channel.sendCommand(tx, 10000L, 10000L, 4)
        { rx, rxErr ->

            appendOutput("RX, ${rx?.uuToHex()}, err: $rxErr")
            ++pingCount
            updateMenu()
        }
        /*
        appendOutput("Writing data...")
        channel.write(tx, 10000L)
        { txErr ->
            appendOutput("TX Complete, err: $txErr")
            updateMenu()

            appendOutput("Reading data...")
            channel.read(10000L)
            { rx, rxErr ->

                appendOutput("RX Complete, ${rx?.uuToHex()}, err: $rxErr")
                ++pingCount
                updateMenu()
            }
        }*/
    }

    private fun onWrite()
    {
        val tx = UURandom.bytes(10)
        appendOutput("TX: ${tx.uuToHex()}")

        appendOutput("Writing data...")
        channel.write(tx, 10000L)
        { txErr ->
            appendOutput("TX Complete, err: $txErr")
            updateMenu()
        }
    }

    private fun onRead()
    {
        appendOutput("Reading data...")
        channel.read(10000L)
        { rx, rxErr ->

            appendOutput("RX Complete, ${rx?.uuToHex()}, err: $rxErr")
            updateMenu()
        }
    }

    override fun buildMenu(): ArrayList<UUMenuItem>
    {
        val list = ArrayList<UUMenuItem>()

        if (channel.isConnected)
        {
            list.add(UUMenuItem("Disconnect", this::onDisconnect))
            list.add(UUMenuItem("Ping", this::onPing))
            list.add(UUMenuItem("Write", this::onWrite))
            list.add(UUMenuItem("Read", this::onRead))
        }
        else
        {
            list.add(UUMenuItem("Connect", this::onConnect))
        }

        return list
    }
}