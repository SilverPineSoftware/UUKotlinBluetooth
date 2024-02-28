package com.silverpine.uu.sample.bluetooth.viewmodel

import android.text.TextUtils
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.core.uuToHex
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel

class UUPeripheralViewModel(val model: UUPeripheral): UUAdapterItemViewModel()
{
    private val _friendlyName = MutableLiveData<String?>(null)
    private val _macAddress = MutableLiveData<String?>(null)
    private val _connectionState = MutableLiveData<String?>(null)
    private val _rssi = MutableLiveData<String?>(null)
    private val _timeSinceLastUpdate = MutableLiveData<String?>(null)
    private val _manufacturingData = MutableLiveData<String?>(null)

    val friendlyName: LiveData<String?> = _friendlyName
    val macAddress: LiveData<String?> = _macAddress
    val connectionState: LiveData<String?> = _connectionState
    val rssi: LiveData<String?> = _rssi
    val timeSinceLastUpdate: LiveData<String?> = _timeSinceLastUpdate
    val manufacturingData: LiveData<String?> = _manufacturingData

    var onClick: ((UUPeripheral)->Unit) = { }

    init
    {
        _friendlyName.postValue("${model.name}")
        _macAddress.postValue("${model.address}\n${TextUtils.join("\n", model.serviceUuids)}")

        _connectionState.postValue("${model.connectionState}")
        _rssi.postValue("${model.rssi}")
        _timeSinceLastUpdate.postValue("${model.timeSinceLastUpdate}")

        model.manufacturingData?.let()
        {
            _manufacturingData.postValue(it.uuToHex())
        } ?: run()
        {
            _manufacturingData.postValue("")
        }
    }

    fun handleClick(view: View)
    {
        onClick.invoke(model)
    }
}