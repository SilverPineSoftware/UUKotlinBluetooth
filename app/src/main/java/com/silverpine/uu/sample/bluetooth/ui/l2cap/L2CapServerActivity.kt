package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.databinding.ActivityL2CapServerBinding
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.UUPermissions

class L2CapServerActivity : AppCompatActivity()
{
    private lateinit var viewModel: L2CapServerViewModel
    private lateinit var menuHandler: UUMenuHandler
    private var menuViewModels: ArrayList<UUMenuItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[L2CapServerViewModel::class.java]
        val binding = ActivityL2CapServerBinding.inflate(layoutInflater)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        viewModel.menuItems.observe(this)
        {
            menuViewModels.clear()
            menuViewModels.addAll(it)
            invalidateMenu()
        }

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

    private fun checkPermissions(completion: ()->Unit)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            UUPermissions.requestPermissions(this, Manifest.permission.BLUETOOTH_ADVERTISE, 9509)
            { permission, granted ->

                UULog.d(javaClass, "checkPermissions", "$permission: $granted")
                completion()
            }
        }
        else
        {
            completion()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // UUMenuHandler
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuHandler = UUMenuHandler(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean
    {
        menu?.clear()

        menuViewModels.forEach()
        { mi ->
            menuHandler.add(mi.title, mi.action)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return menuHandler.handleMenuClick(item)
    }
}