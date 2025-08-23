package com.silverpine.uu.sample.bluetooth.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.ViewDataBinding
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.sample.bluetooth.BR
import com.silverpine.uu.sample.bluetooth.viewmodel.BaseViewModel
import com.silverpine.uu.ux.UUMenuHandler
import com.silverpine.uu.ux.UUMenuItem
import com.silverpine.uu.ux.uuShowAlertDialog
import com.silverpine.uu.ux.uuShowToast
import com.silverpine.uu.ux.uuStartActivity

open class BaseActivity : AppCompatActivity()
{
    private lateinit var menuHandler: UUMenuHandler
    private var menuViewModels: ArrayList<UUMenuItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val color = Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        { // Android 15+
            window.decorView.setOnApplyWindowInsetsListener()
            { view, insets ->
                view.setBackgroundColor(color)
                insets
            }
        }
        else
        {
            // For Android 14 and below
            window.statusBarColor = color
        }

        WindowCompat.getInsetsController(window, window.decorView).apply()
        {
            isAppearanceLightStatusBars = false
        }
    }


    open fun setupViewModel(viewModel: BaseViewModel, binding: ViewDataBinding)
    {
        binding.setVariable(BR.vm, viewModel)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        viewModel.gotoActivity = this::uuStartActivity
        viewModel.showAlertDialog = this::uuShowAlertDialog
        viewModel.onToast = this::uuShowToast

        viewModel.menuItems.observe(this)
        {
            UULog.d(javaClass, "setupViewModel", "Menu items have changed")
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