package com.silverpine.uu.sample.bluetooth.viewmodel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.ux.UUAlertDialog
import com.silverpine.uu.ux.UUMenuItem

open class BaseViewModel: ViewModel()
{
    private var _menuItems: MutableLiveData<ArrayList<UUMenuItem>> = MutableLiveData()
    val menuItems: LiveData<ArrayList<UUMenuItem>> = _menuItems

    var gotoActivity: (Class<out AppCompatActivity>, Bundle?)->Unit = { _, _ -> }
    var showAlertDialog: (UUAlertDialog)->Unit = { }

    protected fun updateMenu()
    {
        val list = buildMenu()

        uuDispatchMain()
        {
            _menuItems.value = list
        }
    }

    protected open fun buildMenu(): ArrayList<UUMenuItem>
    {
        return arrayListOf()
    }
}