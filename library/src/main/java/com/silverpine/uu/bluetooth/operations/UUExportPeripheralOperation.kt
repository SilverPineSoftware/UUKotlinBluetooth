package com.silverpine.uu.bluetooth.operations

import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.models.UUPeripheralRepresentation
import com.silverpine.uu.bluetooth.models.UUServiceRepresentation
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.logging.UULog

private const val LOG_TAG = "UUExportPeripheralOperation"

class UUExportPeripheralOperation(peripheral: UUPeripheral)
    : UUPeripheralOperation<UUPeripheralRepresentation>(peripheral)
{
    override fun execute(completion: (UUPeripheralRepresentation?, UUError?) -> Unit)
    {
        val peripheralRepresentation = UUPeripheralRepresentation(
            services = session.discoveredServices.map { service ->
                UUServiceRepresentation(service)
            }
        )

        val peripheralJson = UUJson.toJson(peripheralRepresentation, UUPeripheralRepresentation::class.java).getOrNull()
        UULog.debug(LOG_TAG, "Peripheral JSON: $peripheralJson")

        completion(peripheralRepresentation, null)
    }
}
