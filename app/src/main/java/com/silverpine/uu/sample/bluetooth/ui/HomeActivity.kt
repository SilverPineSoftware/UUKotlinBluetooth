package com.silverpine.uu.sample.bluetooth.ui

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.Pair
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.silverpine.uu.bluetooth.UUBluetoothScanner
import com.silverpine.uu.bluetooth.UUOutOfRangePeripheralFilter
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralFactory
import com.silverpine.uu.bluetooth.UUPeripheralFilter
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.operations.ReadDeviceInfoOperation
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapClientActivity
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapServerActivity
import com.silverpine.uu.sample.bluetooth.viewmodel.UUPeripheralViewModel
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UUPermissions
import com.silverpine.uu.ux.UURecyclerActivity
import com.silverpine.uu.ux.uuOpenSystemSettings
import com.silverpine.uu.ux.uuPrompt
import java.util.Comparator

class HomeActivity: UURecyclerActivity()
{
    private val TAG = HomeActivity::javaClass.name

    private lateinit var scanner: UUBluetoothScanner<UUPeripheral>

    private var lastUpdate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        scanner = UUBluetoothScanner(applicationContext, object : UUPeripheralFactory<UUPeripheral>
        {
            override fun createPeripheral(
                device: BluetoothDevice,
                rssi: Int,
                scanRecord: ByteArray?
            ): UUPeripheral
            {
                return UUPeripheral(device, rssi, scanRecord)
            }
        })
    }

    override fun setupAdapter(recyclerView: RecyclerView)
    {
        adapter.registerClass(UUPeripheralViewModel::class.java, R.layout.peripheral_row, BR.vm)
    }

    override fun handleRowTapped(viewModel: ViewModel)
    {
        stopScanning()

        if (viewModel is UUPeripheralViewModel)
        {
            val peripheral = viewModel.model
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Choose an action for ${peripheral.name} - ${peripheral.address}")

            val actions = ArrayList<Pair<String, Runnable>>()
            actions.add(Pair("View Services", Runnable { gotoPeripheralServices(peripheral) }))
            actions.add(Pair("Read Info", Runnable { readDeviceInfo(peripheral) }))
            actions.add(Pair("Start L2Cap Client", Runnable { openL2CapClient(peripheral) }))

            val items = arrayOfNulls<String>(actions.size)
            for (i in items.indices)
            {
                items[i] = actions[i].first
            }
            builder.setCancelable(true)

            builder.setItems(items)
            { _, which ->
                if (which >= 0 && which < actions.size)
                {
                    actions[which].second.run()
                }
            }

            builder.create().show()
        }
    }

    private fun gotoPeripheralServices(peripheral: UUPeripheral)
    {
        val intent = Intent(applicationContext, PeripheralDetailActivity::class.java)
        intent.putExtra("peripheral", peripheral)
        startActivity(intent)
    }

    private var readDeviceInfoOperation: ReadDeviceInfoOperation? = null
    private fun readDeviceInfo(peripheral: UUPeripheral)
    {
        val op = ReadDeviceInfoOperation(peripheral)
        readDeviceInfoOperation = op
        op.start()
        { err ->

            uuDispatchMain()
            {
                if (err != null)
                {
                    uuPrompt("Read Device Info",
                        "Error: $err",
                        "OK",
                        null,
                        true,
                        { },
                        {})
                }
                else
                {
                    uuPrompt("Read Device Info",
                        "Name: ${op.deviceName}\nMfg: ${op.mfgName}",
                        "OK",
                        null,
                        true,
                        { },
                        { })
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        refreshPermissions()
    }

    override fun populateMenu(menuHandler: UUMenuHandler)
    {
        UULog.d(javaClass, "populateMenu", "isScanning: ${scanner.isScanning}")

        if (scanner.isScanning)
        {
            menuHandler.addAction(R.string.stop, this::stopScanning)
        }
        else
        {
            menuHandler.addAction(R.string.scan, this::startScanning)
        }

        menuHandler.add(R.string.open_l2cap_server, this::openL2CapServer)
    }

    private fun startScanning()
    {
        Log.d(TAG, "startScanning")

        adapter.update(listOf())

        val filters: ArrayList<UUPeripheralFilter<UUPeripheral>> = arrayListOf()
        filters.add(RequireMinimumRssiPeripheralFilter(-70))
        //filters.add(RequireNoNamePeripheralFilter())
        filters.add(RequireNamePeripheralFilter())

        val outOfRangeFilters: ArrayList<UUOutOfRangePeripheralFilter<UUPeripheral>> = arrayListOf()
        outOfRangeFilters.add(OutOfRangeFilter())

        scanner.startScanning(null, filters, outOfRangeFilters)
        { list ->

            val timeSinceLastUpdate = System.currentTimeMillis() - this.lastUpdate
            if (timeSinceLastUpdate > 300)
            {
                uuDispatchMain()
                {
                    Log.d(TAG, "Updating devices, ${list.size} nearby")
                    val tmp = ArrayList<ViewModel>()
                    val vmList = list.map { UUPeripheralViewModel(it, applicationContext) }
                    sortPeripherals(vmList)
                    tmp.addAll(vmList)
                    adapter.update(tmp)

                    lastUpdate = System.currentTimeMillis()
                }
            }
        }

        invalidateOptionsMenu()
    }

    private fun sortPeripherals(list: List<UUPeripheralViewModel>)
    {
        list.sortedWith(sortByLastRssi(true))
        //list.sortedWith(sortByMacAddress()) // sortByLastRssiUpdateTime(true))

        UULog.d(javaClass, "sortPeripherals", "There are ${list.size} nearby peripherals")
    }

    private fun sortByLastRssi(strongestFirst: Boolean): Comparator<in UUPeripheralViewModel>
    {
        return Comparator()
        { lhs: UUPeripheralViewModel, rhs: UUPeripheralViewModel ->

            val lhsValue = lhs.model.rssi
            val rhsValue = rhs.model.rssi

            if (lhsValue > rhsValue)
            {
                return@Comparator (if (strongestFirst) 1 else -1)
            }
            else if (lhsValue < rhsValue)
            {
                return@Comparator (if (strongestFirst) -1 else 1)
            }
            0
        }
    }

    private fun sortByLastRssiUpdateTime(oldestFirst: Boolean): Comparator<in UUPeripheralViewModel>
    {
        return Comparator()
        { lhs: UUPeripheralViewModel, rhs: UUPeripheralViewModel ->

            val lhsValue = lhs.model.lastRssiUpdateTime
            val rhsValue = rhs.model.lastRssiUpdateTime

            if (lhsValue > rhsValue)
            {
                return@Comparator (if (oldestFirst) 1 else -1)
            }
            else if (lhsValue < rhsValue)
            {
                return@Comparator (if (oldestFirst) -1 else 1)
            }
            0
        }
    }

    private fun sortByMacAddress(): Comparator<in UUPeripheralViewModel>
    {
        return Comparator()
        { lhs: UUPeripheralViewModel, rhs: UUPeripheralViewModel ->

            val lhsValue = lhs.model.address ?: ""
            val rhsValue = rhs.model.address ?: ""

            lhsValue.compareTo(rhsValue)
        }
    }

    private fun stopScanning()
    {
        Log.d(TAG, "stopScanning")

        scanner.stopScanning()
        invalidateOptionsMenu()
    }

    private fun openL2CapServer()
    {
        val intent = Intent(applicationContext, L2CapServerActivity::class.java)
        startActivity(intent)
    }

    private fun openL2CapClient(peripheral: UUPeripheral)
    {
        val intent = Intent(applicationContext, L2CapClientActivity::class.java)
        intent.putExtra("peripheral", peripheral)
        startActivity(intent)
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

            uuPrompt(
                title = R.string.permissions,
                message = msgId,
                positiveButtonTextId = buttonId,
                cancelable = false,
                positiveAction =
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
                })
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

            uuPrompt(
                title = R.string.permissions,
                message = msgId,
                positiveButtonTextId = buttonId,
                cancelable = false,
                positiveAction =
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
                })
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

    inner class RequireNamePeripheralFilter: UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (peripheral.name == null)
            {
                return UUPeripheralFilter.Result.IgnoreForever
            }

            return UUPeripheralFilter.Result.Discover
        }
    }

    inner class RequireNoNamePeripheralFilter: UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (peripheral.name == null)
            {
                return UUPeripheralFilter.Result.Discover
            }

            return UUPeripheralFilter.Result.IgnoreForever
        }
    }

    inner class RequireMinimumRssiPeripheralFilter(private val rssi: Int): UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (peripheral.rssi >= rssi)
            {
                return UUPeripheralFilter.Result.Discover
            }

            return UUPeripheralFilter.Result.IgnoreOnce
        }
    }

    inner class OutOfRangeFilter: UUOutOfRangePeripheralFilter<UUPeripheral>
    {
        override fun checkPeripheralRange(peripheral: UUPeripheral): UUOutOfRangePeripheralFilter.Result
        {
            return if (peripheral.timeSinceLastUpdate > (UUDate.MILLIS_IN_ONE_SECOND * 200))
            {
                UUOutOfRangePeripheralFilter.Result.OutOfRange
            }
            else
            {
                UUOutOfRangePeripheralFilter.Result.InRange
            }
        }
    }
}
