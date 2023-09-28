package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothAdvertiser
import com.silverpine.uu.bluetooth.UUL2CapServer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHexData
import com.silverpine.uu.core.uuWriteInt32
import com.silverpine.uu.core.uuWriteUInt8
import com.silverpine.uu.ux.UUMenuItem
import java.nio.ByteOrder

class L2CapServerViewModel: L2CapBaseViewModel()
{
    private val echoServer: UUL2CapServer? by lazy()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            val server = UUL2CapServer()
            server.dataReceived = this::parseReceivedData
            server
        }
        else
        {
            null
        }
    }

    private var rxCommand: L2CapCommand? = null

    private fun parseReceivedData(data: ByteArray): ByteArray?
    {
        appendOutput("Received ${data.size} bytes")

        if (rxCommand == null)
        {
            appendOutput("Parsing Command Header")
            rxCommand = L2CapCommand.fromBytes(data)
            appendOutput("Command Header parsed, Id: ${rxCommand?.id}, Length: ${rxCommand?.data?.size}, Command Bytes Received: ${rxCommand?.bytesReceived}")
        }
        else
        {
            rxCommand?.receiveBytes(data)
            appendOutput("Total Command Bytes Received: ${rxCommand?.bytesReceived}")
        }

        val cmd = rxCommand ?: return null

        if (cmd.hasReceivedAllBytes())
        {
            appendOutput("Full command has been received, payload size: ${rxCommand?.bytesReceived}")

            val cmdResponse = processCommand(cmd)
            rxCommand = null
            cmdResponse?.let()
            { txCmd ->
                return txCmd.toByteArray()
            }
        }

        return null
    }

    private fun processCommand(command: L2CapCommand): L2CapCommand?
    {
        return when (command.id)
        {
            L2CapCommand.Id.Echo ->
                return L2CapCommand(L2CapCommand.Id.Echo, command.data)

            L2CapCommand.Id.SendImage ->
                L2CapCommand(L2CapCommand.Id.AckImage, "ABCD".uuToHexData() ?: byteArrayOf())

            else ->
                null
        }
    }

    private var advertiser = UUBluetoothAdvertiser(UUBluetooth.requireApplicationContext())
    private val secure: Boolean = false

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
                        advertiser.start(L2CapConstants.UU_L2CAP_SERVICE_UUID, "L2CapServer-$psm", 160)
                        advertiser.clearServices()

                        val service = BluetoothGattService(L2CapConstants.UU_L2CAP_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                        val psmCharacteristic = BluetoothGattCharacteristic(L2CapConstants.UU_L2CAP_PSM_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
                        var result = service.addCharacteristic(psmCharacteristic)

                        if (result)
                        {
                            advertiser.registerCharacteristicReadDelegate(L2CapConstants.UU_L2CAP_PSM_CHARACTERISTIC_UUID)
                            {
                                val buffer = ByteArray(Int.SIZE_BYTES)
                                buffer.uuWriteInt32(ByteOrder.LITTLE_ENDIAN, 0, psm)
                                buffer
                            }

                            val channelEncryptedCharacteristic = BluetoothGattCharacteristic(L2CapConstants.UU_L2CAP_CHANNEL_ENCRYPTED_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)
                            result = service.addCharacteristic(channelEncryptedCharacteristic)
                            if (result)
                            {
                                advertiser.registerCharacteristicReadDelegate(L2CapConstants.UU_L2CAP_CHANNEL_ENCRYPTED_CHARACTERISTIC_UUID)
                                {
                                    val buffer = ByteArray(1)
                                    buffer.uuWriteUInt8(0, if (secure) { 1 } else { 0 })
                                    buffer
                                }

                                appendOutput("GATT server configured")
                                advertiser.addService(service)
                            }
                        }
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
            echoServer?.start(secure)
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
