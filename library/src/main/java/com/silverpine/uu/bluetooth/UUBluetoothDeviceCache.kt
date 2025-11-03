package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import com.silverpine.uu.core.UUInMemoryObjectCache
import com.silverpine.uu.core.UUObjectCache

interface UUBluetoothDeviceCache: UUObjectCache<BluetoothDevice>

object UUInMemoryBluetoothDeviceCache: UUBluetoothDeviceCache, UUInMemoryObjectCache<BluetoothDevice>()
