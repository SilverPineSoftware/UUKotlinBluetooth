package com.silverpine.uu.sample.bluetooth.viewmodel

import android.bluetooth.BluetoothGattService
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.sample.bluetooth.ui.uuTypeAsString
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel

class ServiceViewModel(val model: BluetoothGattService): UUAdapterItemViewModel()
{
    private val _uuid = MutableLiveData<String?>(null)
    private val _name = MutableLiveData<String?>(null)
    private val _type = MutableLiveData<String?>(null)

    val uuid: LiveData<String?> = _uuid
    val name: LiveData<String?> = _name
    val type: LiveData<String?> = _type

    var onClick: ((BluetoothGattService)->Unit) = { }

    init
    {
        _uuid.postValue("${model.uuid}")
        _name.postValue(UUBluetooth.bluetoothSpecName(model.uuid))
        _type.postValue(model.uuTypeAsString())
    }

    fun handleClick(view: View)
    {
        onClick.invoke(model)
    }

}