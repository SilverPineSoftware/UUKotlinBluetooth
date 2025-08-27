package com.silverpine.uu.sample.bluetooth.viewmodel

import android.os.Bundle
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothSniffer
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralScanner
import com.silverpine.uu.bluetooth.UUPeripheralScannerConfig
import com.silverpine.uu.core.UUTimer
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.operations.ReadDeviceInfoOperation
import com.silverpine.uu.sample.bluetooth.tisensortag.TiSensorTagSession
import com.silverpine.uu.sample.bluetooth.ui.PeripheralDetailActivity
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapClientActivity
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapServerActivity
import com.silverpine.uu.ux.UUAlertDialog
import com.silverpine.uu.ux.UUButton
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HomeViewModel: RecyclerViewModel()
{
    // private val scanner: UUBluetoothScanner<UUPeripheral> = UUBluetoothScanner(UUBluetooth.requireApplicationContext(), UUDefaultPeripheralFactory())

    private val scanner: UUPeripheralScanner = UUBluetooth.scanner
    private val sniffer: UUBluetoothSniffer = UUBluetoothSniffer(UUBluetooth.requireApplicationContext())

    private var lastUpdate: Long = 0



    fun start()
    {
        /*scanner.scanDelayedCallback =
        { delayMillis ->
            onToast(UUToast("Scanning too frequently. Scan will resume in $delayMillis milliseconds", Toast.LENGTH_SHORT))
        }*/

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
        list.add(UUMenuItem("Start Sniffer", this::startSniffer))
        list.add(UUMenuItem("Stop Sniffer", this::stopSniffer))

        return list
    }

    private fun startSniffer()
    {
        sniffer.start()
    }

    private fun stopSniffer()
    {
        sniffer.stop()
    }

    private fun startScanning()
    {
        //Log.d(TAG, "startScanning")

        //adapter.update(listOf())

        val config = UUPeripheralScannerConfig()

//        val filters: ArrayList<UUPeripheralFilter<UUPeripheral>> = arrayListOf()
//        //filters.add(RequireMinimumRssiPeripheralFilter(-70))
//        //filters.add(RequireManufacturingDataPeripheralFilter())
//        //filters.add(IgnoreAppleBeaconsPeripheralFilter())
//        //filters.add(RequireNoNamePeripheralFilter())
//        //filters.add(RequireNamePeripheralFilter())
//        //filters.add(MacAddressFilter(listOf("00:11:22:33:44:55")))
//
//        val outOfRangeFilters: ArrayList<UUOutOfRangePeripheralFilter<UUPeripheral>> = arrayListOf()
        //outOfRangeFilters.add(OutOfRangeFilter(30000))

        scanner.config = config
        scanner.started =
        { scanner ->
            UULog.d(javaClass, "startScanning", "Scan was started")
        }

        scanner.ended =
        { scanner, error ->
            UULog.d(javaClass, "startScanning", "Scan ended")
        }

        scanner.listChanged =
        { scanner, list ->

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

        scanner.start()

        updateMenu()
    }

    private fun onPeripheralTapped(peripheral: UUPeripheral)
    {
        val items: ArrayList<UUButton> = ArrayList()
        items.add(UUButton("View Services") { gotoPeripheralServices(peripheral) })
        items.add(UUButton("Read Info") { readDeviceInfo(peripheral) })
        items.add(UUButton("Start L2Cap Client") { gotoL2CapClient(peripheral) })
        items.add(UUButton("Open Sensor Tag Session") { openSensorTagSession(peripheral) })

        val dlg = UUAlertDialog()
        dlg.title = "Choose an action for ${peripheral.name} - ${peripheral.identifier}"
        dlg.items = items

        showAlertDialog(dlg)
    }

    private fun gotoPeripheralServices(peripheral: UUPeripheral)
    {
        val args = Bundle()
        args.putString("peripheral.identifier", peripheral.identifier)

        gotoActivity(PeripheralDetailActivity::class.java, args)
    }

    private fun gotoL2CapClient(peripheral: UUPeripheral)
    {
        val args = Bundle()
        args.putString("peripheral.identifier", peripheral.identifier)

        gotoActivity(L2CapClientActivity::class.java, args)
    }

    private var readDeviceInfoOperation: ReadDeviceInfoOperation? = null
    private fun readDeviceInfo(peripheral: UUPeripheral)
    {
        val op = ReadDeviceInfoOperation(peripheral)
        readDeviceInfoOperation = op
        op.start()
        { _, err ->

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
        }
    }

    private var sensorTagSession: TiSensorTagSession? = null
    private fun openSensorTagSession(peripheral: UUPeripheral)
    {
        uuDispatch()
        {
            val latch = CountDownLatch(1)
            val endLatch = CountDownLatch(1)

            sensorTagSession = TiSensorTagSession(peripheral)
            sensorTagSession?.started =
            { s ->
                UULog.d(javaClass, "openSensorTagSession", "Session started")

                latch.countDown()
            }

            sensorTagSession?.ended =
            { s, e ->
                UULog.d(javaClass, "openSensorTagSession", "Session ended, error: $e")
                endLatch.countDown()
            }

            sensorTagSession?.start()

            latch.await(30, TimeUnit.SECONDS)

            UUTimer.startTimer("end session", 10000, null)
            { _, _ ->
                sensorTagSession?.end(null)
            }

            endLatch.await(30, TimeUnit.SECONDS)
        }
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

            val lhsValue = lhs.model.timeSinceLastUpdate
            val rhsValue = rhs.model.timeSinceLastUpdate

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

            val lhsValue = lhs.model.identifier
            val rhsValue = rhs.model.identifier

            lhsValue.compareTo(rhsValue)
        }
    }

    private fun stopScanning()
    {
        //Log.d(TAG, "stopScanning")

        scanner.stop()
        updateMenu()
    }

    private fun openL2CapServer()
    {
        gotoActivity(L2CapServerActivity::class.java, null)
    }

    /*
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
    }*/
}