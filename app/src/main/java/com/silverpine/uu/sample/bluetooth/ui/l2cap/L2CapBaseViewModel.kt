package com.silverpine.uu.sample.bluetooth.ui.l2cap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.viewmodel.BaseViewModel

abstract class L2CapBaseViewModel: BaseViewModel()
{
    private var _output: MutableLiveData<String> = MutableLiveData("")
    val output: LiveData<String> = _output

    protected fun appendOutput(line: String)
    {
        uuDispatchMain()
        {
            _output.value += "\n$line"
            UULog.d(javaClass, "outputLog", line)
        }
    }
}