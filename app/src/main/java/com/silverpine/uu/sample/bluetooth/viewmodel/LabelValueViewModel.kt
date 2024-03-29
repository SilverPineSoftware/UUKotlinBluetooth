package com.silverpine.uu.sample.bluetooth.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.ux.UUAdapterItemViewModel

class LabelValueViewModel(
    labelText: String? = null,
    valueText: String? = null): UUAdapterItemViewModel()
{
    private val _label = MutableLiveData<String?>(null)
    private val _value = MutableLiveData<String?>(null)

    val label: LiveData<String?> = _label
    val value: LiveData<String?> = _value

    var onClick: (()->Unit) = { }

    init
    {
        _label.value = labelText
        _value.value = valueText
    }

    fun handleClick(view: View)
    {
        onClick.invoke()
    }
}