package com.silverpine.uu.sample.bluetooth.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.widget.Toast
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothGattSession
import com.silverpine.uu.bluetooth.UUConnectionState
import com.silverpine.uu.core.UUResources
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.ui.load
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.UUToast
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class DeviceSessionViewModel: RecyclerViewModel()
{
    private lateinit var device: BluetoothDevice
    private lateinit var session: UUBluetoothGattSession

    private var connectionState: UUConnectionState = UUConnectionState.Undetermined
    private var discoveredServices: List<BluetoothGattService> = listOf()

    fun setup(bluetoothDevice: BluetoothDevice)
    {
        device = bluetoothDevice
        session = UUBluetoothGattSession(UUBluetooth.requireApplicationContext(), bluetoothDevice)
    }

    override fun start()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            refreshUi()
        }
    }

    override fun buildMenu(): ArrayList<UUMenuItem>
    {
        val list = ArrayList<UUMenuItem>()

        list.add(UUMenuItem(R.string.connect, this::connect))
        list.add(UUMenuItem(R.string.disconnect, this::disconnect))
        list.add(UUMenuItem(R.string.discover_services, this::discoverServices))

        return list
    }

    private fun connect()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val err = session.connect()
            if (err != null)
            {
                uuToast(UUToast(err.toString(), Toast.LENGTH_LONG))
                return@launch
            }

            uuToast(UUToast("Connected to ${device.address}", Toast.LENGTH_LONG))

            refreshUi()
        }
    }

    private fun disconnect()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            session.disconnect()
            uuToast(UUToast("Disconnected from ${device.address}", Toast.LENGTH_LONG))

            refreshUi()
        }
    }

    private fun discoverServices()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val result = session.discoverServices()
            discoveredServices = result.success ?: listOf()

            if (result.error != null)
            {
                uuToast(UUToast(result.error.toString(), Toast.LENGTH_LONG))
                discoveredServices = listOf()
            }
            else
            {
                uuToast(UUToast("Discovered ${discoveredServices.size} services from ${device.address}", Toast.LENGTH_LONG))
            }

            refreshUi()
        }
    }

    private suspend fun refreshUi()
    {
        connectionState = session.connectionState()

        val tmp = ArrayList<UUAdapterItemViewModel>()
        tmp.add(SectionHeaderViewModel(R.string.info))
        tmp.add(LabelValueViewModel(R.string.address_label.load(), device.address))
        tmp.add(LabelValueViewModel(R.string.name_label.load(), device.name))
        tmp.add(LabelValueViewModel(R.string.state_label.load(), connectionState.name))
        //tmp.add(LabelValueViewModel(R.string.rssi_label.load(), "${peripheral.rssi}"))
        //tmp.add(LabelValueViewModel(R.string.mtu_size_label.load(), "${peripheral.negotiatedMtuSize}"))

        tmp.add(SectionHeaderViewModel(R.string.services))

        for (service in discoveredServices)
        {
            tmp.add(ServiceViewModel(service))
            tmp.addAll(service.characteristics.map { CharacteristicViewModel2(session, it) })
        }

        updateData(tmp)
        updateMenu()
    }
}