package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.sample.bluetooth.databinding.ActivityL2CapClientBinding
import com.silverpine.uu.ux.uuRequireParcelable

class L2CapClientActivity : AppCompatActivity()
{
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[L2CapClientViewModel::class.java]
        val binding = ActivityL2CapClientBinding.inflate(layoutInflater)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        val peripheral: UUPeripheral = intent.uuRequireParcelable("peripheral")
        viewModel.update(peripheral)
        title = "L2Cap Client"
    }

//    override fun onCreate(savedInstanceState: Bundle?)
//    {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_l2_cap_client)
//
//        peripheral = intent.uuRequireParcelable("peripheral")
//
//        output = findViewById(R.id.output)
//        title = peripheral.name
//    }

//    @SuppressLint("MissingPermission")
//    @RequiresApi(Build.VERSION_CODES.Q)
//    fun onStart(view: View)
//    {
//        appendOutput("Getting L2Cap Socket")
//
//        val psm = peripheral.name?.replace("L2CapServer-", "")?.toInt() ?: 0
//        appendOutput("Connecting to PSM $psm")
//        bluetoothSocket = peripheral.connectL2Cap(psm, false)
//
//        appendOutput("Starting L2Cap Connection")
//
//        try
//        {
//            bluetoothSocket?.connect()
//        }
//        catch (ex: Exception)
//        {
//            appendOutput("Connection Failed: $ex")
//            bluetoothSocket = null
//        }
//    }
//
//    fun onPing(view: View)
//    {
//
//        val tx = "57575757"
//        appendOutput("TX: $tx")
//        bluetoothSocket?.outputStream?.write(tx.uuToHexData())
//
//        val available = bluetoothSocket?.inputStream?.available()
//        appendOutput("There are $available bytes to read.")
//
//        val rxBuffer = ByteArray(1024)
//        val bytesRead = bluetoothSocket?.inputStream?.read(rxBuffer, 0, rxBuffer.size) ?: 0
//
//        appendOutput("Read $bytesRead bytes")
//
//        val rx = rxBuffer.uuSubData(0, bytesRead)
//        appendOutput("RX: ${rx?.uuToHex()}")
//    }

//    private fun appendOutput(line: String)
//    {
//        UULog.d(javaClass, "appendOutput", line)
//
//        output.text = "${output.text}\n$line"
//    }
}