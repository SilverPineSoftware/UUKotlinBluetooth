package com.silverpine.uu.sample.bluetooth.viewmodel

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothConstants
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.uuCanReadData
import com.silverpine.uu.bluetooth.uuCanToggleNotify
import com.silverpine.uu.bluetooth.uuCanWriteData
import com.silverpine.uu.bluetooth.uuCanWriteWithoutResponse
import com.silverpine.uu.bluetooth.uuHasExtendedProperties
import com.silverpine.uu.bluetooth.uuIsNotifying
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.core.uuIsNull
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuToHexData
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.ui.Strings
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel

class CharacteristicViewModel(private val peripheral: UUPeripheral, val model: BluetoothGattCharacteristic): UUAdapterItemViewModel()
{
    private val _uuid = MutableLiveData<String?>(null)
    private val _name = MutableLiveData<String?>(null)
    private val _properties = MutableLiveData<String?>(null)
    private val _isNotifying = MutableLiveData<String?>(null)
    private val _dataEditable = MutableLiveData(false)
    private val _hexSelected = MutableLiveData(true)
    private val _canToggleNotify = MutableLiveData(false)
    private val _canReadData = MutableLiveData(false)
    private val _canWriteData = MutableLiveData(false)
    private val _canWWORWriteData = MutableLiveData(false)

    val uuid: LiveData<String?> = _uuid
    val name: LiveData<String?> = _name
    val properties: LiveData<String?> = _properties
    val isNotifying: LiveData<String?> = _isNotifying
    val dataEditable: LiveData<Boolean> = _dataEditable
    val hexSelected: LiveData<Boolean> = _hexSelected
    val canToggleNotify: LiveData<Boolean> = _canToggleNotify
    val canReadData: LiveData<Boolean> = _canReadData
    val canWriteData: LiveData<Boolean> = _canWriteData
    val canWWORWriteData: LiveData<Boolean> = _canWWORWriteData

    var data = MutableLiveData<String?>(null)

    var index = 0

    init
    {
        _uuid.value = "${model.uuid}"
        _name.value = UUBluetooth.bluetoothSpecName(model.uuid)
        _properties.value = UUBluetooth.characteristicPropertiesToString(model.properties)
        _dataEditable.value = model.uuCanWriteData() or model.uuCanWriteWithoutResponse()
        _canToggleNotify.value = model.uuCanToggleNotify()
        _canReadData.value = model.uuCanReadData()
        _canWriteData.value = model.uuCanWriteData()
        _canWWORWriteData.value = model.uuCanWriteWithoutResponse()

        refreshNotifyLabel()
        refreshData()
    }

    private fun formatData(): String?
    {
        return formatData(model.value)
    }

    private fun formatData(value: ByteArray?): String?
    {
        if (value == null)
        {
            return null
        }

        if (hexSelected.value == true)
        {
            return value.uuToHex()
        }

        return String(value, Charsets.UTF_8)
    }

    fun toggleHex(hex: Boolean)
    {
        Log.d("LOG", "Hex: $hex")
        _hexSelected.value = hex
        refreshData()
    }

    fun readData()
    {
        peripheral.readCharacteristic(model, 60000)
        { p, updatedCharacteristic, error ->

            uuDispatchMain()
            {
                refreshData()
            }
        }
    }

    fun toggleNotify()
    {
        val isNotifying = model.uuIsNotifying()

        peripheral.setNotifyState(model,
            !isNotifying,
            30000,
            { peripheral, characteristic, error -> //UULog.debug(javaClass, "setNotify.characteristicChanged",
                //  "Characteristic changed, characteristic: " + characteristic.uuid +
                //        ", data: " + UUString.byteToHex(characteristic.value) +
                //      ", error: " + error)

                uuDispatchMain()
                {
                    refreshData()
                }
            }
        ) { peripheral, characteristic, error -> //UULog.debug(javaClass, "setNotify.onComplete",
            //  ("Set Notify complete, characteristic: " + characteristic.uuid +
            //        ", error: " + error))
            //UUListView.reloadRow(listView, position)

            uuDispatchMain()
            {
                refreshData()
            }
        }
    }

    fun writeData()
    {
        data.value?.let()
        { hex ->

            val tx = hex.uuToHexData()

            Log.d("DEBUG", "Writing $hex")

            peripheral.writeCharacteristic(model, tx!!, 10000)
            { p, c, e ->

                uuDispatchMain()
                {
                    refreshData()
                }
            }
        }
    }

    fun wworWriteData()
    {
        data.value?.let()
        { hex ->

            val tx = hex.uuToHexData()

            Log.d("DEBUG", "Writing WWOR $hex")

            peripheral.writeCharacteristicWithoutResponse(model, tx!!, 10000)
            { p, c, e ->

                uuDispatchMain()
                {
                    refreshData()
                }
            }
        }
    }

    private fun refreshNotifyLabel()
    {
        if (model.uuIsNotifying())
        {
            _isNotifying.value = Strings.load(R.string.yes)
        }
        else
        {
            _isNotifying.value = Strings.load(R.string.no)
        }
    }

    private fun refreshData()
    {
        data.value = formatData()
    }

    fun updateData(updatedData: ByteArray?)
    {
        data.value = formatData(updatedData)
    }
}

@BindingAdapter("selected")
fun setSelected(view: View, value: Boolean)
{
    view.isSelected = value
    view.invalidate()
}