package com.silverpine.uu.bluetooth.extensions

import com.silverpine.uu.bluetooth.uuIsValidShortCode
import com.silverpine.uu.bluetooth.uuShortCodeToUuid
import java.util.UUID

val String.uuToBluetoothShortCodeUuid: UUID?
    get() = runCatching {

        if (uuIsValidShortCode(this))
        {
            uuShortCodeToUuid(this)
        }
        else
        {
            null
        }
    }.getOrNull()

val String.uuToUuid: UUID?
    get() = runCatching()
    {
        val shortCodeUuid = uuToBluetoothShortCodeUuid
        return shortCodeUuid ?: UUID.fromString(this)
    }.getOrNull()