package com.silverpine.uu.sample.bluetooth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.ux.viewmodel.UUAdapterItemViewModel

open class RecyclerViewModel: BaseViewModel()
{
    private var _data: MutableLiveData<ArrayList<UUAdapterItemViewModel>> = MutableLiveData()
    val data: LiveData<ArrayList<UUAdapterItemViewModel>> = _data

    fun updateData(list: ArrayList<UUAdapterItemViewModel>)
    {
        _data.postValue(list)
    }

    open fun start()
    {

    }
}