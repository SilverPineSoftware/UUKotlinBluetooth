package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RawRes
import androidx.annotation.RequiresApi
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUL2CapChannel
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.UUResources
import com.silverpine.uu.core.uuReadInt32
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuWriteInt32
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.operations.ReadL2CapSettingsOperation
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

    private var readL2CapSettingsOperation: ReadL2CapSettingsOperation? = null

    private fun readL2CapSettings(completion: (Int, Boolean, UUError?)->Unit)
    {
        val op = ReadL2CapSettingsOperation(peripheral)
        readL2CapSettingsOperation = op
        op.start()
        { err ->
            completion(op.psm, op.channelEncrypted, err)
            readL2CapSettingsOperation = null
        }
    }

    private fun onConnect()
    {
        appendOutput("Reading L2Cap Settings from GATT")
        readL2CapSettings()
        { psm, secure, error ->

            error?.let()
            { err ->
                appendOutput("Failed to read L2Cap settings, error: $err")
                return@let
            }

            val timeout = 10000L

            appendOutput("Connecting to L2CapChannel with PSM $psm, secure: $secure, timeout: $timeout")
            channel.connect(psm, secure, timeout)
            { err ->
                appendOutput("Connection Complete, Error: $err")
                updateMenu()
            }
        }
    }

    private fun onDisconnect()
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
        val buffer = ByteArray(Int.SIZE_BYTES)
        buffer.uuWriteInt32(ByteOrder.BIG_ENDIAN, 0, pingCount)

        val cmd = L2CapCommand(L2CapCommand.Id.Echo, buffer)
        val tx = cmd.toByteArray()

        appendOutput("Sending ping, ${tx.size} bytes")
        appendOutput("TX: ${tx.uuToHex()}")
        val start = System.currentTimeMillis()
        channel.sendCommand(tx,10000L, 10000L, tx.size)
        { rx, rxErr ->

            val duration = System.currentTimeMillis() - start
            appendOutput("Ping Sent, took $duration millis, err: $rxErr")

            appendOutput("RX, ${rx?.uuToHex()}")
            ++pingCount
            updateMenu()
        }
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

    private fun onSendImage(@RawRes resourceId: Int)
    {
        val data = UUResources.getRawBytes(resourceId)
        if (data == null)
        {
            appendOutput("Failed to read image resource")
            return
        }

        val cmd = L2CapCommand(L2CapCommand.Id.SendImage, data)
        val tx = cmd.toByteArray()

        appendOutput("Writing Image, ${tx.size} bytes")
        val start = System.currentTimeMillis()
        channel.sendCommand(tx,UUDate.MILLIS_IN_ONE_MINUTE * 5, UUDate.MILLIS_IN_ONE_MINUTE * 5, L2CapCommand.HEADER_SIZE)
        { _, rxErr ->

            val duration = System.currentTimeMillis() - start
            appendOutput("Image Sent and ack received, took $duration millis, err: $rxErr")
            updateMenu()
        }
    }

    override fun buildMenu(): ArrayList<UUMenuItem>
    {
        val list = ArrayList<UUMenuItem>()

        if (channel.isConnected)
        {
            list.add(UUMenuItem(R.string.disconnect_l2cap, this::onDisconnect))
            list.add(UUMenuItem(R.string.ping, this::onPing))
            list.add(UUMenuItem(R.string.read, this::onRead))
            list.add(UUMenuItem(R.string.write, this::onWrite))
            list.add(UUMenuItem("Send Image 1", { onSendImage(R.raw.image_1) }))
            list.add(UUMenuItem("Send Image 2", { onSendImage(R.raw.image_2) }))
        }
        else
        {
            list.add(UUMenuItem(R.string.connect_l2cap, this::onConnect))
        }

        if (peripheral.connectionState != UUPeripheral.ConnectionState.Connected)
        {
            list.add(UUMenuItem(R.string.connect_gatt, this::onConnectGatt))
        }
        else
        {
            list.add(UUMenuItem(R.string.disconnect_gatt, this::onDisconnectGatt))
            list.add(UUMenuItem(R.string.discover_services, this::onDiscoverGattServices))
            list.add(UUMenuItem("Read PSM", this::onReadPsmChannel))
            list.add(UUMenuItem("Read Channel Encrypted", this::onReadChannelEncryptedFlag))
        }

        return list
    }

    private fun onConnectGatt()
    {
        appendOutput("Connecting to GATT Layer...")
        peripheral.connect(10000L, 10000L,
        {
            appendOutput("GATT Layer was connected")
            updateMenu()
        },
        { err ->
            appendOutput("GATT disconnection, error: $err")
            updateMenu()
        })
    }

    private fun onDisconnectGatt()
    {
        appendOutput("Disconnecting GATT Layer")
        peripheral.disconnect(null)
        updateMenu()
    }

    private fun onDiscoverGattServices()
    {
        appendOutput("Discovering GATT Services")
        peripheral.discoverServices(10000L)
        { services, error ->
            appendOutput("Discover Services completed, found ${services.size} services, error: $error")

            services.forEach()
            {
                appendOutput("Service: ${it.uuid}, ${UUBluetooth.bluetoothSpecName(it.uuid)}")
            }
        }

        updateMenu()
    }

    private fun onReadPsmChannel()
    {
        val service = peripheral.discoveredServices().firstOrNull { it.uuid == L2CapConstants.UU_L2CAP_SERVICE_UUID }
        val psmCharacteristic = service?.characteristics?.firstOrNull { it.uuid == L2CapConstants.UU_L2CAP_PSM_CHARACTERISTIC_UUID }
        psmCharacteristic?.let()
        {
            peripheral.readCharacteristic(it, 10000L)
            { peripheral, characteristic, error ->

                error?.let()
                { err ->
                    appendOutput("Failed to read PSM: $err")
                    return@let
                }

                val psm = characteristic.value.uuReadInt32(ByteOrder.LITTLE_ENDIAN, 0)
                appendOutput("PSM Value: $psm")
            }
        }
    }

    private fun onReadChannelEncryptedFlag()
    {
        val service = peripheral.discoveredServices().firstOrNull { it.uuid == L2CapConstants.UU_L2CAP_SERVICE_UUID }
        val psmCharacteristic = service?.characteristics?.firstOrNull { it.uuid == L2CapConstants.UU_L2CAP_CHANNEL_ENCRYPTED_CHARACTERISTIC_UUID }
        psmCharacteristic?.let()
        {
            peripheral.readCharacteristic(it, 10000L)
            { peripheral, characteristic, error ->

                error?.let()
                { err ->
                    appendOutput("Failed to read channel encrypted: $err")
                    return@let
                }

                val secure = characteristic.value.uuReadUInt8(0)
                appendOutput("Channel Secure: $secure")
            }
        }
    }
}