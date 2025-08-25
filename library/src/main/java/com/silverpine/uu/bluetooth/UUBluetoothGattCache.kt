package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGatt

interface UUBluetoothGattCache: UUObjectCache<BluetoothGatt>

object UUInMemoryBluetoothGattCache: UUBluetoothGattCache, UUInMemoryObjectCache<BluetoothGatt>()