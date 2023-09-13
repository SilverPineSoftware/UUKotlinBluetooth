package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.databinding.ActivityL2CapServerBinding
import com.silverpine.uu.ux.UUPermissions

class L2CapServerActivity : AppCompatActivity()
{
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[L2CapServerViewModel::class.java]
        val binding = ActivityL2CapServerBinding.inflate(layoutInflater)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        viewModel.checkPermissions = this::checkPermissions
        viewModel.reset()
        title = "L2Cap Server"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        UUPermissions.handleRequestPermissionsResult(this, requestCode,
            permissions.asList().toTypedArray(), grantResults)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermissions(completion: ()->Unit)
    {
        UUPermissions.requestPermissions(this, Manifest.permission.BLUETOOTH_ADVERTISE, 9509)
        { permission, granted ->

            UULog.d(javaClass, "checkPermissions", "$permission: $granted")
            completion()
        }
    }
}