package com.silverpine.uu.sample.bluetooth.ui.l2cap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.ui.UUMenuItem

abstract class L2CapBaseViewModel: ViewModel()
{
    private var _output: MutableLiveData<String> = MutableLiveData("")
    val output: LiveData<String> = _output

    private var _menuItems: MutableLiveData<ArrayList<UUMenuItem>> = MutableLiveData()
    val menuItems: LiveData<ArrayList<UUMenuItem>> = _menuItems

    protected fun updateMenu()
    {
        val list = buildMenu()

        uuDispatchMain()
        {
            _menuItems.value = list
        }
    }

    abstract fun buildMenu(): ArrayList<UUMenuItem>

    protected fun appendOutput(line: String)
    {
        uuDispatchMain()
        {
            _output.value += "\n$line"
            UULog.d(javaClass, "outputLog", line)
        }
    }
}