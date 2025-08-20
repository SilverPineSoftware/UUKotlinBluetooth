package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog

typealias UUVoidBlock = () -> Unit
typealias UUErrorBlock = (UUError?) -> Unit
typealias UUObjectBlock<T> = (T) -> Unit
typealias UUListBlock<T> = (List<T>) -> Unit
typealias UUObjectErrorBlock<T> = (T?, UUError?) -> Unit
typealias UUListErrorBlock<T> = (List<T>?, UUError?) -> Unit


fun UUVoidBlock.safeNotify()
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

fun UUErrorBlock.safeNotify(error: UUError?)
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

fun <T> UUObjectBlock<T>.safeNotify(obj: T)
{
    try
    {
        this(obj)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

fun <T> UUListBlock<T>.safeNotify(obj: List<T>)
{
    try
    {
        this(obj)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

fun <T> UUObjectErrorBlock<T>.safeNotify(obj: T?, error: UUError?)
{
    try
    {
        this(obj, error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

fun <T> UUListErrorBlock<T>.safeNotify(obj: List<T>?, error: UUError?)
{
    try
    {
        this(obj, error)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "safeNotify", "", ex)
    }
}

