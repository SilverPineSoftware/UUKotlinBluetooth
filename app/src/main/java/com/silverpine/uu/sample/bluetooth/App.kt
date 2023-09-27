package com.silverpine.uu.sample.bluetooth

import android.app.Application
import com.silverpine.uu.bluetooth.UUBluetooth
import com.silverpine.uu.core.UUResources
import com.silverpine.uu.sample.bluetooth.ui.Strings

class App: Application()
{
    override fun onCreate()
    {
        super.onCreate()

        UUResources.init(applicationContext)
        UUBluetooth.init(applicationContext)
        Strings.init(applicationContext)
    }
}