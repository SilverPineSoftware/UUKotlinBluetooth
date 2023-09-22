package com.silverpine.uu.sample.bluetooth.ui.l2cap

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.R
import com.silverpine.uu.sample.bluetooth.databinding.ActivityL2CapClientBinding
import com.silverpine.uu.sample.bluetooth.ui.UUMenuItem
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.uuRequireParcelable

class L2CapClientActivity : AppCompatActivity()
{
    private lateinit var viewModel: L2CapClientViewModel
    private lateinit var menuHandler: UUMenuHandler
    private var menuViewModels: ArrayList<UUMenuItem> = arrayListOf()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[L2CapClientViewModel::class.java]
        val binding = ActivityL2CapClientBinding.inflate(layoutInflater)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        viewModel.menuItems.observe(this)
        {
            menuViewModels.clear()
            menuViewModels.addAll(it)
            invalidateMenu()
        }

        val peripheral: UUPeripheral = intent.uuRequireParcelable("peripheral")
        viewModel.update(peripheral)
        title = "L2Cap Client"
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // UUMenuHandler
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuHandler = UUMenuHandler(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean
    {
        menu?.clear()

        menuViewModels.forEach()
        { mi ->
            menuHandler.add(mi.title, mi.action)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return menuHandler.handleMenuClick(item)
    }
}