package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.silverpine.uu.bluetooth.UUDiscoverServicesCompletionBlock
import com.silverpine.uu.bluetooth.UUPeripheralConnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralDisconnectedBlock
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSafeToString
import com.silverpine.uu.logging.UULog
import java.util.Locale
import java.util.UUID


internal fun BluetoothGattCharacteristic?.uuSafeUuidString(): String
{
    return this?.uuid.uuToLowercaseString()
}

internal fun BluetoothGattDescriptor?.uuSafeUuidString(): String
{
    return this?.uuid.uuToLowercaseString()
}

internal fun UUID?.uuToLowercaseString(): String
{
    return this?.uuSafeToString()?.lowercase(Locale.getDefault()) ?: ""
}

internal fun BluetoothGattCharacteristic?.uuHashLookup(): String
{
    return uuSafeUuidString()
}

internal fun BluetoothGattDescriptor?.uuHashLookup(): String
{
    return "${this?.characteristic?.uuSafeUuidString()}-${this?.uuSafeUuidString()}"
}

internal fun UUPeripheralConnectedBlock.safeNotify()
{
    try
    {
        this()
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

internal fun UUPeripheralDisconnectedBlock.safeNotify(error: UUError?)
{
    try
    {
        this(error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

internal fun UUDiscoverServicesCompletionBlock.safeNotify(services: List<BluetoothGattService>?, error: UUError?)
{
    try
    {
        this(services, error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}