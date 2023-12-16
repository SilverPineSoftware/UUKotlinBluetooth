package com.silverpine.uu.sample.bluetooth.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.databinding.ActivityHomeBinding
import com.silverpine.uu.sample.bluetooth.viewmodel.HomeViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.UUPeripheralViewModel
import com.silverpine.uu.ux.UUAdapterItemViewModelMapping
import com.silverpine.uu.ux.UUAlertDialog
import com.silverpine.uu.ux.UUButton
import com.silverpine.uu.ux.UUPermissions
import com.silverpine.uu.ux.UUViewModelRecyclerAdapter
import com.silverpine.uu.ux.uuOpenSystemSettings
import com.silverpine.uu.ux.uuShowAlertDialog

class HomeActivity: BaseActivity()
{
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: UUViewModelRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setupViewModel(viewModel, binding)

        viewModel.data.observe(this)
        {
            adapter.update(it)
        }

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UUViewModelRecyclerAdapter(this)
        recyclerView.adapter = adapter
        setupAdapter()

        viewModel.reset()
    }

    private fun setupAdapter()
    {
        adapter.registerViewModel(UUAdapterItemViewModelMapping(UUPeripheralViewModel::class.java, R.layout.peripheral_row, BR.vm))
    }

    override fun onResume()
    {
        super.onResume()

        refreshPermissions()
    }

    companion object
    {
        const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

        @RequiresApi(Build.VERSION_CODES.S)
        const val BLUETOOTH_SCAN_PERMISSION = Manifest.permission.BLUETOOTH_SCAN

        @RequiresApi(Build.VERSION_CODES.S)
        const val BLUETOOTH_CONNECT_PERMISSION = Manifest.permission.BLUETOOTH_CONNECT
    }

    private val hasLocationPermission: Boolean
        get()
        {
            return UUPermissions.hasPermission(applicationContext, LOCATION_PERMISSION)
        }

    private val canRequestLocationPermission: Boolean
        get()
        {
            return UUPermissions.canRequestPermission(this, LOCATION_PERMISSION)
        }

    private val hasScanPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return UUPermissions.hasPermission(applicationContext, BLUETOOTH_SCAN_PERMISSION)
        }

    private val canRequestScanPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return UUPermissions.canRequestPermission(this, BLUETOOTH_SCAN_PERMISSION)
        }

    private val hasConnectPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return UUPermissions.hasPermission(applicationContext, BLUETOOTH_CONNECT_PERMISSION)
        }

    private val canRequestConnectPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return UUPermissions.canRequestPermission(this, BLUETOOTH_CONNECT_PERMISSION)
        }

    private fun refreshPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            refreshPermissionsPostSdk31()
        }
        else
        {
            refreshPermissionsPreSdk31()
        }
    }

    private fun refreshPermissionsPreSdk31()
    {
        refreshPermissionsPreSdk31(hasLocationPermission)
    }

    private fun refreshPermissionsPreSdk31(hasPermissions: Boolean)
    {
        if (!hasPermissions)
        {
            val canRequest = canRequestLocationPermission
            var msgId = R.string.location_permission_denied_message
            var buttonId = R.string.app_settings

            if (canRequest)
            {
                msgId = R.string.location_permission_request_message
                buttonId = R.string.request_permission
            }

            val dlg = UUAlertDialog()
            dlg.setTitleResource(R.string.permissions)
            dlg.setMessageResource(msgId)
            dlg.cancelable = false
            dlg.positiveButton = UUButton(buttonId)
            {
                if (canRequest)
                {
                    UUPermissions.requestPermissions(this, LOCATION_PERMISSION, 12276)
                    { _, granted ->

                        refreshPermissionsPreSdk31(granted)
                    }
                }
                else
                {
                    uuOpenSystemSettings()
                }
            }

            uuShowAlertDialog(dlg)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun refreshPermissionsPostSdk31()
    {
        val hasPermissions = (hasScanPermission and hasConnectPermission)
        if (!hasPermissions)
        {
            val canRequest = (canRequestScanPermission and canRequestConnectPermission)
            var msgId = R.string.bluetooth_permission_denied_message
            var buttonId = R.string.app_settings

            if (canRequest)
            {
                msgId = R.string.bluetooth_permission_request_message
                buttonId = R.string.request_permission
            }

            val dlg = UUAlertDialog()
            dlg.setTitleResource(R.string.permissions)
            dlg.setMessageResource(msgId)
            dlg.cancelable = false
            dlg.positiveButton = UUButton(buttonId)
            {
                if (canRequest)
                {
                    UUPermissions.requestMultiplePermissions(this, arrayOf(BLUETOOTH_SCAN_PERMISSION, BLUETOOTH_CONNECT_PERMISSION), 12276)
                    { _ ->

                        refreshPermissionsPostSdk31()
                    }
                }
                else
                {
                    uuOpenSystemSettings()
                }
            }

            uuShowAlertDialog(dlg)
        }
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
}
