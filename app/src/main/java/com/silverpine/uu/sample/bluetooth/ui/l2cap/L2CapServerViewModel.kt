package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothAdvertiser
import com.silverpine.uu.bluetooth.UUL2CapServer
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuWriteInt32
import com.silverpine.uu.core.uuWriteUInt8
import com.silverpine.uu.ux.UUAlpha
import com.silverpine.uu.ux.UUMenuItem
import java.nio.ByteOrder

class L2CapServerViewModel: L2CapBaseViewModel()
{
    private var _imageAlpha: MutableLiveData<UUAlpha> = MutableLiveData(UUAlpha(0.0f))
    val imageAlpha: LiveData<UUAlpha> = _imageAlpha

    private var _imageBitmap: MutableLiveData<Bitmap?> = MutableLiveData(null)
    val imageBitmap: LiveData<Bitmap?> = _imageBitmap

    private val echoServer: UUL2CapServer? by lazy()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            val server = UUL2CapServer()
            server.dataReceived = this::parseReceivedData
            server.clientConnected = this::clientConnected
            server
        }
        else
        {
            null
        }
    }

    private var rxCommand: L2CapCommand? = null
    private var rxCommandStart: Long = 0

    private fun clientConnected()
    {
        appendOutput("Client was connected, waiting for data...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            echoServer?.startReading()
        }
    }

    private fun parseReceivedData(data: ByteArray)
    {
        appendOutput("Received ${data.size} bytes")
        appendOutput("RX: ${data.uuToHex()}")

        if (rxCommand == null)
        {
            appendOutput("Parsing Command Header")
            rxCommandStart = System.currentTimeMillis()
            rxCommand = L2CapCommand.fromBytes(data)
            appendOutput("Command Header parsed, Id: ${rxCommand?.id}, Length: ${rxCommand?.data?.size}, Command Bytes Received: ${rxCommand?.bytesReceived}")
        }
        else
        {
            rxCommand?.receiveBytes(data)
            appendOutput("Total Command Bytes Received: ${rxCommand?.bytesReceived}")
        }

        val cmd = rxCommand ?: return

        if (cmd.hasReceivedAllBytes())
        {
            val duration = System.currentTimeMillis() - rxCommandStart
            appendOutput("Full command has been received, payload size: ${rxCommand?.bytesReceived}, took: $duration millis")

            val cmdResponse = processCommand(cmd)
            rxCommand = null
            cmdResponse?.let()
            { txCmd ->

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    val tx = txCmd.toByteArray()
                    appendOutput("Writing ${tx.size} bytes")
                    appendOutput("TX: ${tx.uuToHex()}")
                    echoServer?.write(txCmd.toByteArray(), 10000L)
                    { txErr ->
                        appendOutput("Response sent, err: $txErr")

                    }
                }
            }
        }
    }

    private fun processCommand(command: L2CapCommand): L2CapCommand?
    {
        return when (command.id)
        {
            L2CapCommand.Id.Echo ->
                return L2CapCommand(L2CapCommand.Id.Echo, command.data)

            L2CapCommand.Id.SendImage ->
            {
                try
                {
                    val bmp = BitmapFactory.decodeByteArray(command.data, 0, command.data.size)
                    if (bmp != null)
                    {
                        appendOutput("Successfully decoded image")
                        showImage(bmp)
                    }
                }
                catch (ex: Exception)
                {
                    appendOutput("Failed to decode image: $ex")
                }

                L2CapCommand(L2CapCommand.Id.AckImage, byteArrayOf())
            }

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

    private fun showImage(bmp: Bitmap)
    {
        uuDispatchMain()
        {
            _imageBitmap.value = bmp
            _imageAlpha.value = UUAlpha.FADE_IN
        }
    }

    fun onImageTap()
    {
        _imageAlpha.value = UUAlpha.FADE_OUT
    }
}
