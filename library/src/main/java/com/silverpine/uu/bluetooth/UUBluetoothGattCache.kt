package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGatt
import com.silverpine.uu.core.UUInMemoryObjectCache
import com.silverpine.uu.core.UUObjectCache

interface UUBluetoothGattCache: UUObjectCache<BluetoothGatt>

object UUInMemoryBluetoothGattCache: UUBluetoothGattCache, UUInMemoryObjectCache<BluetoothGatt>()