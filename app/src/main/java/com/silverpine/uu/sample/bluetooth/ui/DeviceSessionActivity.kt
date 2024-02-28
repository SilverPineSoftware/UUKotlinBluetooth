package com.silverpine.uu.sample.bluetooth.ui

import androidx.lifecycle.ViewModelProvider
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.viewmodel.CharacteristicViewModel2
import com.silverpine.uu.sample.bluetooth.viewmodel.DeviceSessionViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.LabelValueViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.RecyclerViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.SectionHeaderViewModel
import com.silverpine.uu.sample.bluetooth.viewmodel.ServiceViewModel
import com.silverpine.uu.ux.uuRequireParcelable
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModelMapping

class DeviceSessionActivity : RecyclerActivity()
{
    override fun getViewModel(): RecyclerViewModel
    {
        val vm = ViewModelProvider(this)[DeviceSessionViewModel::class.java]

        vm.setup(intent.uuRequireParcelable("bluetoothDevice"))

        return vm
    }

    override fun setupAdapter()
    {
        adapter.registerViewModel(UUAdapterItemViewModelMapping(LabelValueViewModel::class.java, R.layout.label_value_row, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(SectionHeaderViewModel::class.java, R.layout.section_header, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(ServiceViewModel::class.java, R.layout.service_row, BR.vm))
        adapter.registerViewModel(UUAdapterItemViewModelMapping(CharacteristicViewModel2::class.java, R.layout.characteristic_row2, BR.vm))
    }
}


