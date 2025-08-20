package com.silverpine.uu.sample.bluetooth.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
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
        val p = UUBluetooth.scanner.getPeripheral(peripheralIdentifier)
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
            intent.putExtra("serviceUuid", viewModel.model.uuid.toString())
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
            menuHandler.add(R.string.read_rssi, this::handleReadRssi)
            menuHandler.add(R.string.request_mtu, this::handleRequestMtu)
            menuHandler.add(R.string.read_phy, this::handleReadPhy)
            menuHandler.add(R.string.set_preferred_phy, this::handleSetPreferredPhy)
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

    private fun handleReadRssi()
    {
        peripheral.readRSSI(60000)
        { rssi, error ->
            uuShowToast("Peripheral RSSI is $rssi")

            refreshUi()
        }
    }

    private fun handleRequestMtu()
    {
        peripheral.requestMtu(100, 10000)
        { mtu, error ->
            uuShowToast("MTU Size is $mtu")

            refreshUi()
        }
    }

    private fun handleReadPhy()
    {
        peripheral.readPhy( 10000)
        { result, error ->
            val txPhy = result?.first
            val rxPhy = result?.second
            uuShowToast("TxPhy: $txPhy, RxPhy: $rxPhy")

            refreshUi()
        }
    }

    private fun handleSetPreferredPhy()
    {
        val txPhy = BluetoothDevice.PHY_LE_2M_MASK
        val rxPhy = BluetoothDevice.PHY_LE_2M_MASK
        val options = BluetoothDevice.PHY_OPTION_NO_PREFERRED
        peripheral.updatePhy(txPhy, rxPhy, options, 10000)
        { result, error ->
            val txPhyUpdated = result?.first
            val rxPhyUpdated = result?.second
            uuShowToast("TxPhy: $txPhyUpdated, RxPhy: $rxPhyUpdated")

            refreshUi()
        }
    }

    private fun handleDisconnect()
    {
        peripheral.disconnect(10000)
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
            tmp.add(LabelValueViewModel(R.string.mtu_size_label.load(), "${peripheral.mtuSize}"))
            tmp.add(LabelValueViewModel(R.string.tx_phy_label.load(), "${peripheral.txPhy}"))
            tmp.add(LabelValueViewModel(R.string.rx_phy_label.load(), "${peripheral.rxPhy}"))

            tmp.add(SectionHeaderViewModel(R.string.services))
            tmp.addAll(discoveredServices.map { ServiceViewModel(it) })
            adapter.update(tmp)
        }
    }

}


