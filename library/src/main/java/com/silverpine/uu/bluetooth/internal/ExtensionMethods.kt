package com.silverpine.uu.bluetooth.internal

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.bluetooth.UUPeripheralConnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralDisconnectedBlock
import com.silverpine.uu.bluetooth.UUPeripheralSession
import com.silverpine.uu.bluetooth.UUPeripheralSessionErrorCallback
import com.silverpine.uu.bluetooth.UUPeripheralSessionObjectErrorCallback
import com.silverpine.uu.bluetooth.UUPeripheralSessionStartedCallback
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

internal fun UUPeripheralSessionStartedCallback.safeNotify(session: UUPeripheralSession)
{
    try
    {
        this(session)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

internal fun UUPeripheralSessionErrorCallback.safeNotify(session: UUPeripheralSession, error: UUError?)
{
    try
    {
        this(session, error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

internal fun <T> UUPeripheralSessionObjectErrorCallback<T>.safeNotify(session: UUPeripheralSession, data: T?, error: UUError?)
{
    try
    {
        this(session, data, error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}
