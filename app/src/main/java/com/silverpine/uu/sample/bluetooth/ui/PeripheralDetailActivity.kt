package com.silverpine.uu.sample.bluetooth.ui

import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
import com.silverpine.uu.bluetooth.defaultScanner
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.viewmodel.LabelValueViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.SectionHeaderViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.ServiceViewModel
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UURecyclerActivity
import com.silverpine.uu.ux.uuRequireString
import com.silverpine.uu.ux.uuShowToast
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModelMapping

class PeripheralDetailActivity : UURecyclerActivity()
{
    private lateinit var peripheral: UUPeripheral

    private var discoveredServices: List<BluetoothGattService> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val peripheralIdentifier = intent.uuRequireString("peripheral.identifier")
        val p = UUBluetooth.defaultScanner.getPeripheral(peripheralIdentifier)
            ?: throw RuntimeException("Expect peripheral $peripheralIdentifier to exist!")

        peripheral = p

        title = peripheral.name
    }

    override fun setupAdapter(recyclerView: RecyclerView)
    {
        adapter.registerViewModel(UUAdapterItemViewModelMapping(LabelValueViewModel::class.java, R.layout.label_value_row, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(ServiceViewModel::class.java, R.layout.service_row, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(SectionHeaderViewModel::class.java, R.layout.section_header, BR.vm))
    }

    override fun handleRowTapped(viewModel: ViewModel)
    {
        if (viewModel is ServiceViewModel)
        {
            val intent = Intent(applicationContext, ServiceDetailActivity::class.java)
            intent.putExtra("peripheral.identifier", peripheral.identifier)
            intent.putExtra("service", viewModel.model)
            // intent.putExtra("serviceUuid", viewModel.model.uuid.toString())
            startActivity(intent)
        }
    }

    override fun onResume()
    {
        super.onResume()
        refreshUi()
    }

    override fun populateMenu(menuHandler: UUMenuHandler)
    {
        if (peripheral.peripheralState == UUPeripheralConnectionState.Connected)
        {
            menuHandler.add(R.string.disconnect, this::handleDisconnect)
            menuHandler.add(R.string.discover_services, this::handleDiscoverServices)
        }
        else
        {
            menuHandler.add(R.string.connect, this::handleConnect)
        }
    }

    private fun handleConnect()
    {
        peripheral.connect(60000,
            {

            Log.d("LOG", "Peripheral connected")
            uuShowToast("Connected")
            refreshUi()

        },
            { disconnectError ->

                Log.d("LOG", "Peripheral disconnected")
                uuShowToast("Disconnected")
                refreshUi()
            })
    }

    private fun handleDiscoverServices()
    {
        peripheral.discoverServices( 60000)
        { services, error ->
            uuShowToast("Found ${services?.size ?: 0} services")

            this.discoveredServices = services ?: listOf()
            refreshUi()
        }
    }

    private fun handleDisconnect()
    {
        peripheral.disconnect(10000) //null)
    }

    private fun refreshUi()
    {
        uuDispatchMain()
        {
            val tmp = ArrayList<UUAdapterItemViewModel>()
            tmp.add(SectionHeaderViewModel(R.string.info))
            tmp.add(LabelValueViewModel(R.string.address_label.load(), peripheral.identifier))
            tmp.add(LabelValueViewModel(R.string.name_label.load(), peripheral.name))
            tmp.add(LabelValueViewModel(R.string.state_label.load(), peripheral.peripheralState.name))
            tmp.add(LabelValueViewModel(R.string.rssi_label.load(), "${peripheral.rssi}"))
            tmp.add(LabelValueViewModel(R.string.mtu_size_label.load(), "${peripheral.negotiatedMtuSize}"))

            tmp.add(SectionHeaderViewModel(R.string.services))
            tmp.addAll(discoveredServices.map { ServiceViewModel(it) })
            adapter.update(tmp)
        }
    }

}


