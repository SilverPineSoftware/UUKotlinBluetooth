package com.silverpine.uu.sample.bluetooth.viewmodel

import android.os.ParcelUuid
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
        _friendlyName.value = (model.advertisement.localName.takeIf { it.isNotEmpty() } ?: model.name).takeIf { it.isNotEmpty() } ?: "Unknown Device"
        _macAddress.value = "${model.identifier}\n${TextUtils.join("\n", model.advertisement.services?.map { it }?.toList() ?: ArrayList<ParcelUuid>(0))}"

        _connectionState.value = "${model.peripheralState}"
        _rssi.value = "${model.rssi}"
        _timeSinceLastUpdate.value = "${model.timeSinceLastUpdate}"

        model.advertisement.manufacturingData?.let()
        {
            _manufacturingData.value = formatManufacturingData(it)
        } ?: run()
        {
            _manufacturingData.value = ""
        }
    }

    fun formatManufacturingData(mfgData: Map<Int,ByteArray>): String
    {
        val builder = StringBuilder()

        for (entry in mfgData)
        {
            val key = entry.key
            val value = entry.value

            val valueString = value.uuToHex()
            builder.append("$key: $valueString\n")
        }

        return builder.toString()
    }

    fun handleClick(view: View)
    {
        onClick.invoke(model)
    }
}