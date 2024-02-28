package com.silverpine.uu.sample.bluetooth.viewmodel

import android.os.Bundle
import android.widget.Toast
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothGattSession
import com.silverpine.uu.bluetooth.UUBluetoothScanner
import com.silverpine.uu.bluetooth.UUDefaultPeripheralFactory
import com.silverpine.uu.bluetooth.UUOutOfRangePeripheralFilter
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralFilter
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.operations.ReadDeviceInfoOperation
import com.silverpine.uu.sample.bluetooth.ui.DeviceSessionActivity
import com.silverpine.uu.sample.bluetooth.ui.PeripheralDetailActivity
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapClientActivity
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapServerActivity
import com.silverpine.uu.ux.UUAlertDialog
import com.silverpine.uu.ux.UUButton
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.UUToast
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel: RecyclerViewModel()
{
    private val scanner: UUBluetoothScanner<UUPeripheral> = UUBluetoothScanner(UUBluetooth.requireApplicationContext(), UUDefaultPeripheralFactory())

    private var lastUpdate: Long = 0

    override fun start()
    {
        scanner.scanDelayedCallback =
        { delayMillis ->
            uuToast(UUToast("Scanning too frequently. Scan will resume in $delayMillis milliseconds", Toast.LENGTH_SHORT))
        }

        stopScanning()
        updateMenu()
    }

    override fun buildMenu(): ArrayList<UUMenuItem>
    {
        val list = ArrayList<UUMenuItem>()

        if (scanner.isScanning)
        {
            list.add(UUMenuItem(R.string.stop, this::stopScanning, true))
        }
        else
        {
            list.add(UUMenuItem(R.string.scan, this::startScanning, true))
        }

        list.add(UUMenuItem(R.string.open_l2cap_server, this::openL2CapServer))

        return list
    }


    private fun startScanning()
    {
        //Log.d(TAG, "startScanning")

        //adapter.update(listOf())

        val filters: ArrayList<UUPeripheralFilter<UUPeripheral>> = arrayListOf()
        //filters.add(RequireMinimumRssiPeripheralFilter(-70))
        //filters.add(RequireManufacturingDataPeripheralFilter())
        //filters.add(IgnoreAppleBeaconsPeripheralFilter())
        //filters.add(RequireNoNamePeripheralFilter())
        filters.add(RequireNamePeripheralFilter())

        val outOfRangeFilters: ArrayList<UUOutOfRangePeripheralFilter<UUPeripheral>> = arrayListOf()
        outOfRangeFilters.add(OutOfRangeFilter(30000))

        scanner.startScanning(null, filters, outOfRangeFilters)
        { list ->

            val timeSinceLastUpdate = System.currentTimeMillis() - this.lastUpdate
            if (timeSinceLastUpdate > 300)
            {
                uuDispatchMain()
                {
                    val tmp = ArrayList<UUAdapterItemViewModel>()
                    val vmList = list.map()
                    {
                        val vm = UUPeripheralViewModel(it)
                        vm.onClick = this::onPeripheralTapped
                        vm
                    }

                    sortPeripherals(vmList)
                    tmp.addAll(vmList)
                    updateData(tmp)

                    lastUpdate = System.currentTimeMillis()
                }
            }
        }

        updateMenu()
    }

    private fun onPeripheralTapped(peripheral: UUPeripheral)
    {
        val items: ArrayList<UUButton> = ArrayList()
        items.add(UUButton("View Services") { gotoPeripheralServices(peripheral) })
        items.add(UUButton("Read Info") { readDeviceInfo(peripheral) })
        items.add(UUButton("Start L2Cap Client") { gotoL2CapClient(peripheral) })
        items.add(UUButton("Start Session") { gotoDeviceSession(peripheral) })

        val dlg = UUAlertDialog()
        dlg.title = "Choose an action for ${peripheral.name} - ${peripheral.address}"
        dlg.items = items

        uuShowAlertDialog(dlg)
    }

    private fun gotoPeripheralServices(peripheral: UUPeripheral)
    {
        val args = Bundle()
        args.putParcelable("peripheral", peripheral)

        uuStartActivity(PeripheralDetailActivity::class.java, args)
    }

    private fun gotoDeviceSession(peripheral: UUPeripheral)
    {
        val args = Bundle()
        args.putParcelable("bluetoothDevice", peripheral.bluetoothDevice)

        uuStartActivity(DeviceSessionActivity::class.java, args)
    }

    private fun gotoL2CapClient(peripheral: UUPeripheral)
    {
        val args = Bundle()
        args.putParcelable("peripheral", peripheral)

        uuStartActivity(L2CapClientActivity::class.java, args)
    }

    private var readDeviceInfoOperation: ReadDeviceInfoOperation? = null
    private fun readDeviceInfo(peripheral: UUPeripheral)
    {
        val session = UUBluetoothGattSession(UUBluetooth.requireApplicationContext(), peripheral.bluetoothDevice)

        CoroutineScope(Dispatchers.IO).launch()
        {
            val connectError = session.connect()
            UULog.d(javaClass, "readDeviceInfo","Connect returned: $connectError")

            val servicesResult = session.discoverServices()
            UULog.d(javaClass, "readDeviceInfo", "discoverServices, error: ${servicesResult.error}, services: ${servicesResult.success}")
            servicesResult.success?.forEach { UULog.d(javaClass, "readDeviceInfo", "Service: ${it.uuid} - ${UUBluetooth.bluetoothSpecName(it.uuid)}") }

            servicesResult.success?.let()
            { services ->
                for (service in services)
                {
                    service.characteristics.forEach { UULog.d(javaClass, "readDeviceInfo", "Service: ${it.uuid} - ${UUBluetooth.bluetoothSpecName(it.uuid)}") }

                    for (chr in service.characteristics)
                    {
                        UULog.d(javaClass, "readDeviceInfo", "Reading ${chr.uuid} - ${UUBluetooth.bluetoothSpecName(chr.uuid)}, Properties: ${UUBluetooth.characteristicPropertiesToString(chr.properties)}, Permissions: ${UUBluetooth.characteristicPermissionsToString(chr.permissions)}")
                        val readResult = session.readCharacteristic(chr)
                        UULog.d(javaClass, "readDeviceInfo", "Read char ${chr.uuid} returned ${readResult.success?.uuToHex()}, error: ${readResult.error}")
                    }
                }

            }


            session.disconnect()
        }

        /*
        val op = ReadDeviceInfoOperation(peripheral)
        readDeviceInfoOperation = op
        op.start()
        { err ->

            uuDispatchMain()
            {
                if (err != null)
                {
                    val dlg = UUAlertDialog()
                    dlg.title = "Read Device Info"
                    dlg.message = "Error: $err"
                    dlg.positiveButton = UUButton("OK")
                    {

                    }

                    showAlertDialog(dlg)
                }
                else
                {
                    val dlg = UUAlertDialog()
                    dlg.title = "Read Device Info"
                    dlg.message = "Name: ${op.deviceName}\nMfg: ${op.mfgName}"
                    dlg.positiveButton = UUButton("OK")
                    {

                    }

                    showAlertDialog(dlg)
                }
            }
        }*/
    }

    private fun sortPeripherals(list: List<UUPeripheralViewModel>)
    {
        list.sortedWith(sortByLastRssi(true))
        //list.sortedWith(sortByMacAddress()) // sortByLastRssiUpdateTime(true))

        UULog.d(javaClass, "sortPeripherals", "There are ${list.size} nearby peripherals")
    }

    private fun sortByLastRssi(strongestFirst: Boolean): java.util.Comparator<in UUPeripheralViewModel>
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

    private fun sortByLastRssiUpdateTime(oldestFirst: Boolean): java.util.Comparator<in UUPeripheralViewModel>
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

    private fun sortByMacAddress(): java.util.Comparator<in UUPeripheralViewModel>
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
        //Log.d(TAG, "stopScanning")

        scanner.stopScanning()
        updateMenu()
    }

    private fun openL2CapServer()
    {
        uuStartActivity(L2CapServerActivity::class.java, null)
    }

    inner class RequireNamePeripheralFilter: UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (peripheral.name == null)
            {
                return UUPeripheralFilter.Result.IgnoreOnce
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

            return UUPeripheralFilter.Result.IgnoreOnce
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

    inner class RequireManufacturingDataPeripheralFilter(): UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (peripheral.manufacturingData != null)
            {
                return UUPeripheralFilter.Result.Discover
            }

            return UUPeripheralFilter.Result.IgnoreOnce
        }
    }

    inner class IgnoreAppleBeaconsPeripheralFilter(): UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            val check = peripheral.manufacturingData?.uuReadUInt8(0)
            if (check == 0x4C)
            {
                return UUPeripheralFilter.Result.IgnoreOnce
            }

            return UUPeripheralFilter.Result.Discover
        }
    }

    inner class MacAddressFilter(private val list: List<String>): UUPeripheralFilter<UUPeripheral>
    {
        override fun shouldDiscoverPeripheral(peripheral: UUPeripheral): UUPeripheralFilter.Result
        {
            if (list.contains(peripheral.address))
            {
                return UUPeripheralFilter.Result.Discover
            }

            return UUPeripheralFilter.Result.IgnoreOnce
        }
    }

    inner class OutOfRangeFilter(private val outOfRangeTimeout: Long): UUOutOfRangePeripheralFilter<UUPeripheral>
    {
        override fun checkPeripheralRange(peripheral: UUPeripheral): UUOutOfRangePeripheralFilter.Result
        {
            return if (peripheral.timeSinceLastUpdate > outOfRangeTimeout)
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