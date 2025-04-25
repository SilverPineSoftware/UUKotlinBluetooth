package com.silverpine.uu.sample.bluetooth.operations

import com.silverpine.uu.bluetooth.UUPeripheral
import com.silverpine.uu.bluetooth.UUPeripheralOperation
import com.silverpine.uu.bluetooth.readInt
import com.silverpine.uu.bluetooth.readUByte
import com.silverpine.uu.core.UUError
import com.silverpine.uu.sample.bluetooth.ui.l2cap.L2CapConstants
import java.nio.ByteOrder

class ReadL2CapSettingsOperation(peripheral: UUPeripheral): UUPeripheralOperation<Void>(peripheral)
{
    var psm: Int = 0
    var channelEncrypted: Boolean = false

    override fun execute(completion: (Void?, UUError?) -> Unit)
    {
        session.readInt(L2CapConstants.UU_L2CAP_PSM_CHARACTERISTIC_UUID, ByteOrder.LITTLE_ENDIAN)
        { psmResult ->
            psm = psmResult ?: 0

            session.readUByte(L2CapConstants.UU_L2CAP_CHANNEL_ENCRYPTED_CHARACTERISTIC_UUID)
            { channelEncryptedResult ->

                channelEncrypted = (channelEncryptedResult?.toInt() == 1)
                completion(null, null)
            }
        }
    }
}