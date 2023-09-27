package com.silverpine.uu.sample.bluetooth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class RecyclerViewModel: BaseViewModel()
{
    private var _data: MutableLiveData<ArrayList<ViewModel>> = MutableLiveData()
    val data: LiveData<ArrayList<ViewModel>> = _data

    fun updateData(list: ArrayList<ViewModel>)
    {
        _data.value = list
    }
}