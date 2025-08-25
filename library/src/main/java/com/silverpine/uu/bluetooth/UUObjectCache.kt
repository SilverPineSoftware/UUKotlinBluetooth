package com.silverpine.uu.bluetooth

import java.util.concurrent.ConcurrentHashMap

interface UUObjectCache<T>
{
    operator fun get(identifier: String): T?
    operator fun set(identifier: String, obj: T?)
}

open class UUInMemoryObjectCache<T>: UUObjectCache<T>
{
    private val cache = ConcurrentHashMap<String, T>()

    override operator fun get(identifier: String): T?
    {
        return cache[identifier]
    }

    override operator fun set(identifier: String, device: T?)
    {
        if (device != null)
        {
            cache[identifier] = device
        }
        else
        {
            cache.remove(identifier)
        }
    }
}