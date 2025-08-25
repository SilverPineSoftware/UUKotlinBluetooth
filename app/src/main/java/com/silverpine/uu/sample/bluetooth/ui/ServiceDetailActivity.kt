package com.silverpine.uu.sample.bluetooth.ui

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralConnectionState
import com.silverpine.uu.bluetooth.uuCanReadData
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.viewmodel.CharacteristicViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.SectionHeaderViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.ServiceViewModel
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UURecyclerActivity
import com.silverpine.uu.ux.uuRequireParcelable
import com.silverpine.uu.ux.uuRequireString
import com.silverpine.uu.ux.uuShowToast
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModelMapping

class ServiceDetailActivity: UURecyclerActivity(layoutResourceId = R.layout.recycler_activity)
{
    private lateinit var peripheral: UUPeripheral
    private lateinit var service: BluetoothGattService

    private var charViewModels: ArrayList<CharacteristicViewModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        appSetBackgroundColor()

        val peripheralIdentifier = intent.uuRequireString("peripheral.identifier")
        val p = UUBluetooth.scanner.getPeripheral(peripheralIdentifier)
            ?: throw RuntimeException("Expect peripheral $peripheralIdentifier to exist!")

        peripheral = p

        service = intent.uuRequireParcelable("service")
        title = peripheral.name
    }

    override fun setupAdapter(recyclerView: RecyclerView)
    {
        adapter.registerViewModel(UUAdapterItemViewModelMapping(ServiceViewModel::class.java, R.layout.service_row, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(SectionHeaderViewModel::class.java, R.layout.section_header, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(CharacteristicViewModel::class.java, R.layout.characteristic_row, BR.vm))
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
            menuHandler.add(R.string.read_all, this::handleReadAll)
        }
        else
        {
            menuHandler.add(R.string.connect, this::handleConnect)
        }
    }

    override fun handleRowTapped(viewModel: ViewModel)
    {

    }

    private fun handleConnect()
    {
        peripheral.connect(60000, {

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

    private fun handleDisconnect()
    {
        peripheral.disconnect(null) //10000) //null)
    }

    private fun handleReadAll()
    {
        val readableChars = service.characteristics.filter { it.uuCanReadData() }
        readNextChar(ArrayList(readableChars))
    }

    private fun readNextChar(list: ArrayList<BluetoothGattCharacteristic>)
    {
        val char = list.removeFirstOrNull()
        char?.let()
        { chr ->
            peripheral.read(chr, 10000)
            { data, error ->

                val vm = charViewModels.firstOrNull { it.model.uuid == chr.uuid  } ?: return@read

                uuDispatchMain()
                {
                    vm.updateData(data)
                    adapter.notifyItemChanged(vm.index)

                    readNextChar(list)
                }
            }
        }
    }

    private fun refreshUi()
    {
        uuDispatchMain()
        {
            charViewModels.clear()

            val tmp = ArrayList<UUAdapterItemViewModel>()
            tmp.add(SectionHeaderViewModel(R.string.info))
            tmp.add(ServiceViewModel(service))
            tmp.add(SectionHeaderViewModel(R.string.characteristics))
            tmp.addAll(service.characteristics.map()
            {
                val vm = CharacteristicViewModel(peripheral, it)
                vm.index = tmp.size
                charViewModels.add(vm)
                vm
            })

            adapter.update(tmp)
        }
    }
}


