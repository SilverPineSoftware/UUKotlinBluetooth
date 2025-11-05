package com.silverpine.uu.bluetooth.extensions

import android.os.ParcelUuid
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

val String.uuToParcelUuid: ParcelUuid?
    get() = runCatching()
    {
        val shortCodeUuid = uuToBluetoothShortCodeUuid
        return if (shortCodeUuid != null)
        {
            ParcelUuid(shortCodeUuid)
        }
        else
        {
            ParcelUuid.fromString(this)
        }
    }.getOrNull()