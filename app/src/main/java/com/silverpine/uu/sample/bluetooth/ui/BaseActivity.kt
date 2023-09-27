package com.silverpine.uu.sample.bluetooth.ui

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.viewmodel.BaseViewModel
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.uuShowAlertDialog
import com.silverpine.uu.ux.uuStartActivity

open class BaseActivity : AppCompatActivity()
{
    private lateinit var menuHandler: UUMenuHandler
    private var menuViewModels: ArrayList<UUMenuItem> = arrayListOf()

    open fun setupViewModel(viewModel: BaseViewModel, binding: ViewDataBinding)
    {
        binding.setVariable(BR.vm, viewModel)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        viewModel.gotoActivity = this::uuStartActivity
        viewModel.showAlertDialog = this::uuShowAlertDialog

        viewModel.menuItems.observe(this)
        {
            menuViewModels.clear()
            menuViewModels.addAll(it)
            invalidateMenu()
        }
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

            if (mi.isAction)
            {
                menuHandler.addAction(mi.title, mi.action)
            }
            else
            {
                menuHandler.add(mi.title, mi.action)
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return menuHandler.handleMenuClick(item)
    }
}