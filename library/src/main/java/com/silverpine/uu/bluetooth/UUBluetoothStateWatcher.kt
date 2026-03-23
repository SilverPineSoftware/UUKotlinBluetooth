package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.silverpine.uu.bluetooth.extensions.uuPowerStateString
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LOG_TAG = "UUBluetoothStateWatcher"

class UUBluetoothStateWatcher(val appContext: Context) : BroadcastReceiver()
{
    private val _state = MutableStateFlow(UUBluetoothState.UNKNOWN)

    val currentState: UUBluetoothState
        get() = _state.value

    val stateFlow: StateFlow<UUBluetoothState> = _state.asStateFlow()

    fun start() = runCatching()
    {
        _state.value = UUBluetooth.currentState
        UULog.debug(LOG_TAG, "Starting bluetooth state watcher, current state: $currentState")
        appContext.registerReceiver(this, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun stop() = runCatching()
    {
        UULog.debug(LOG_TAG, "Stopping bluetooth state watcher, current state: $currentState")
        appContext.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        runCatching()
        {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.action, ignoreCase = true))
            {
                var oldState = -1
                var newState = -1

                if (intent.hasExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE))
                {
                    oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
                }

                if (intent.hasExtra(BluetoothAdapter.EXTRA_STATE))
                {
                    newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                }

                UULog.debug(
                    LOG_TAG,
                    "Bluetooth state changed from ${oldState.uuPowerStateString} ($oldState) to ${newState.uuPowerStateString} ($newState)",
                )

                if (newState != -1)
                {
                    //val next =
                    _state.value = UUBluetoothState.fromBluetoothState(newState)
                    //listener?.invoke(next)
                }
            }
        }
    }
}

/*
class UUBluetoothStateWatcher(val appContext: Context, val listener: UUObjectBlock<UUBluetoothState>) : BroadcastReceiver()
{
    var currentState: UUBluetoothState = UUBluetoothState.UNKNOWN
        private set

    fun start() = runCatching()
    {
        currentState = UUBluetooth.currentState
        UULog.debug(LOG_TAG, "Starting bluetooth state watcher, current state: $currentState")
        appContext.registerReceiver(this, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun stop() = runCatching()
    {
        UULog.debug(LOG_TAG, "Stopping bluetooth state watcher, current state: $currentState")
        appContext.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent)
    {
        runCatching()
        {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.action, ignoreCase = true))
            {
                var oldState = -1
                var newState = -1

                if (intent.hasExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE))
                {
                    oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1)
                }

                if (intent.hasExtra(BluetoothAdapter.EXTRA_STATE))
                {
                    newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                }

                UULog.debug(LOG_TAG, "Bluetooth state changed from ${oldState.uuPowerStateString} ($oldState) to ${newState.uuPowerStateString} ($newState)")

                // Bail if the new state is unknown
                if (newState != -1)
                {
                    currentState = UUBluetoothState.fromBluetoothState(newState)
                    listener(currentState)
                }
            }
        }
    }
}*/