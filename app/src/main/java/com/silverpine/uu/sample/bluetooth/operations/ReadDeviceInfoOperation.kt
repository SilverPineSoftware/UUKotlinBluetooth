package com.silverpine.uu.sample.bluetooth.operations

import com.silverpine.uu.bluetooth.UUBluetoothConstants
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralOperation
import com.silverpine.uu.bluetooth.readUtf8
import com.silverpine.uu.core.UUError

data class ReadDeviceInfoResult(
    var deviceName: String? = null,
    var mfgName: String? = null
)

class ReadDeviceInfoOperation(peripheral: UUPeripheral): UUPeripheralOperation<ReadDeviceInfoResult>(peripheral)
{
    var deviceName: String? = null
    var mfgName: String? = null

    override fun execute(completion: (ReadDeviceInfoResult?, UUError?) -> Unit)
    {
        session.readUtf8(UUBluetoothConstants.Characteristics.DEVICE_NAME_UUID)
        {  deviceNameResult ->

            this.deviceName = deviceNameResult

            session.readUtf8(UUBluetoothConstants.Characteristics.MANUFACTURER_NAME_STRING_UUID)
            {  mfgNameResult ->

                this.mfgName = mfgNameResult

                completion(ReadDeviceInfoResult(this.deviceName, this.mfgName), null)
            }

        }
    }
}