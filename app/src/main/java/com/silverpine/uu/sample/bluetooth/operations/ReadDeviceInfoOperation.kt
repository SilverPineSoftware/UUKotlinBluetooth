package com.silverpine.uu.sample.bluetooth.operations

import android.util.Log
import com.silverpine.uu.bluetooth.UUBluetoothConstants
import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralSession
import com.silverpine.uu.bluetooth.operations.UUPeripheralOperation
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

    init
    {
        session.stateChanged = this::handleStateChanged
    }

    private fun handleStateChanged(session: UUPeripheralSession)
    {
        Log.d("ReadDeviceInfoOperation", "State Changed to ${session.state}")
    }

    override fun execute(completion: (ReadDeviceInfoResult?, UUError?) -> Unit)
    {
        session.readUtf8(UUBluetoothConstants.Characteristics.DEVICE_NAME_UUID)
        {  session, deviceNameResult, error ->

            this.deviceName = deviceNameResult

            session.readUtf8(UUBluetoothConstants.Characteristics.MANUFACTURER_NAME_STRING_UUID)
            {  session, mfgNameResult, error ->

                this.mfgName = mfgNameResult

                completion(ReadDeviceInfoResult(this.deviceName, this.mfgName), null)
            }

        }
    }
}