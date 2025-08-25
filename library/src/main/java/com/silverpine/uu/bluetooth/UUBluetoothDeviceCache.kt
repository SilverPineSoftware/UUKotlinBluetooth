package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice

interface UUBluetoothDeviceCache: UUObjectCache<BluetoothDevice>

object UUInMemoryBluetoothDeviceCache: UUBluetoothDeviceCache, UUInMemoryObjectCache<BluetoothDevice>()
