package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUInMemoryObjectCache
import com.silverpine.uu.core.UUObjectCache

interface UUPeripheralConnectionStateCache: UUObjectCache<UUPeripheralConnectionState>

object UUInMemoryPeripheralConnectionStateCache: UUPeripheralConnectionStateCache, UUInMemoryObjectCache<UUPeripheralConnectionState>()
