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
}