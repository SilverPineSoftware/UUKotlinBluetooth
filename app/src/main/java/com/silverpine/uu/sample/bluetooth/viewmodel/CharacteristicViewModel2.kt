package com.silverpine.uu.sample.bluetooth.viewmodel

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.bluetooth.UUBluetoothGattSession
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.core.uuToHexData
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.ui.Strings
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CharacteristicViewModel2(private val session: UUBluetoothGattSession, private val model: BluetoothGattCharacteristic): UUAdapterItemViewModel()
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

    private var characteristicData: ByteArray? = null
    var data = MutableLiveData<String?>(null)

    init
    {
        _uuid.postValue("${model.uuid}")
        _name.postValue(UUBluetooth.bluetoothSpecName(model.uuid))
        _properties.postValue(UUBluetooth.characteristicPropertiesToString(model.properties))
        _dataEditable.postValue((UUBluetooth.canWriteData(model) || UUBluetooth.canWriteWithoutResponse(model)))
        _canToggleNotify.postValue(UUBluetooth.canToggleNotify(model))
        _canReadData.postValue(UUBluetooth.canReadData(model))
        _canWriteData.postValue(UUBluetooth.canWriteData(model))
        _canWWORWriteData.postValue(UUBluetooth.canWriteWithoutResponse(model))

        refreshNotifyLabel()
        refreshData()
    }

    private fun formatData(): String?
    {
        val data = characteristicData ?: return null

        if (hexSelected.value == true)
        {
            return data.uuToHex()
        }

        return String(data, Charsets.UTF_8)
    }

    fun toggleHex(hex: Boolean)
    {
        Log.d("LOG", "Hex: $hex")
        _hexSelected.value = hex
        refreshData()
    }

    fun readData()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val result = session.readCharacteristic(model)
            characteristicData = result.success

            if (result.error != null)
            {
                // Show error
            }

            refreshData()
        }
    }

    fun toggleNotify()
    {
        val isNotifying = UUBluetooth.isNotifying(model)

        /*
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
        }*/
    }

    fun writeData()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            data.value?.let()
            { hex ->

                val tx = hex.uuToHexData()

                Log.d("DEBUG", "Writing $hex")

                val result = session.writeCharacteristic(model, tx!!, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                refreshData()
            }
        }
    }

    fun wworWriteData()
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            data.value?.let()
            { hex ->

                val tx = hex.uuToHexData()

                Log.d("DEBUG", "Writing $hex without response")

                val result = session.writeCharacteristic(model, tx!!, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                refreshData()
            }
        }
    }

    private fun refreshNotifyLabel()
    {
        if (UUBluetooth.isNotifying(model))
        {
            _isNotifying.postValue(Strings.load(R.string.yes))
        }
        else
        {
            _isNotifying.postValue(Strings.load(R.string.no))
        }
    }

    private fun refreshData()
    {
        data.postValue(formatData())
    }
}