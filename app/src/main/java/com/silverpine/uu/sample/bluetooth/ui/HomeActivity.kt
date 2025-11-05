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
import com.silverpine.uu.ux.UUAlertDialog
import com.silverpine.uu.ux.UUButton
import com.silverpine.uu.ux.UUViewModelRecyclerAdapter
import com.silverpine.uu.ux.permissions.UUPermissionProvider
import com.silverpine.uu.ux.permissions.UUPermissions
import com.silverpine.uu.ux.uuOpenSystemSettings
import com.silverpine.uu.ux.uuShowAlertDialog
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModelMapping

class HomeActivity: BaseActivity()
{
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: UUViewModelRecyclerAdapter
    private var permissionsProvider: UUPermissionProvider = UUPermissions

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        UUPermissions.init(this)

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

        viewModel.start()
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
            return permissionsProvider.getPermissionStatus(LOCATION_PERMISSION).isGranted
        }

    private val canRequestLocationPermission: Boolean
        get()
        {
            return permissionsProvider.getPermissionStatus(LOCATION_PERMISSION).canRequest
        }

    private val hasScanPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return permissionsProvider.getPermissionStatus(BLUETOOTH_SCAN_PERMISSION).isGranted
        }

    private val canRequestScanPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return permissionsProvider.getPermissionStatus(BLUETOOTH_SCAN_PERMISSION).canRequest
        }

    private val hasConnectPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return permissionsProvider.getPermissionStatus(BLUETOOTH_CONNECT_PERMISSION).isGranted
        }

    private val canRequestConnectPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.S)
        get()
        {
            return permissionsProvider.getPermissionStatus(BLUETOOTH_CONNECT_PERMISSION).canRequest
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
                    permissionsProvider.requestPermissions(arrayOf(LOCATION_PERMISSION))
                    { result ->
                        val granted =
                            result.getOrDefault(LOCATION_PERMISSION, null)?.isGranted ?: false
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
                    permissionsProvider.requestPermissions(arrayOf(BLUETOOTH_SCAN_PERMISSION, BLUETOOTH_CONNECT_PERMISSION))
                    { result ->
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
}
