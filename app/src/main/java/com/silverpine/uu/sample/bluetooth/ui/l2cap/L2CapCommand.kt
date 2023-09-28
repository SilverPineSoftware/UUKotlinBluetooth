package com.silverpine.uu.sample.bluetooth.ui.l2cap

import com.silverpine.uu.core.uuReadUInt32
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.core.uuWriteData
import com.silverpine.uu.core.uuWriteUInt32
import com.silverpine.uu.core.uuWriteUInt8
import java.nio.ByteOrder

class L2CapCommand(val id: Id, val data: ByteArray)
{
    enum class Id(val value: Int)
    {
        Echo(0x01),
        SendImage(0x02),
        AckImage(0x03);

        companion object
        {
            fun fromUInt8(data: Int): Id?
            {
                return when (data)
                {
                    Echo.value -> Echo
                    SendImage.value -> SendImage
                    else -> null
                }

            }
        }
    }

    var bytesReceived: Int = 0

    fun receiveBytes(bytes: ByteArray)
    {
        data.uuWriteData(bytesReceived, bytes)
        bytesReceived += bytes.size
    }

    fun hasReceivedAllBytes(): Boolean
    {
        return bytesReceived == data.size
    }

    fun toByteArray(): ByteArray
    {
        var buffer = ByteArray(HEADER_SIZE + data.size)
        var index = 0
        index += buffer.uuWriteUInt8(index, 0x55) // U
        index += buffer.uuWriteUInt8(index, 0x55) // U
        index += buffer.uuWriteUInt8(index, 0x42) // B
        index += buffer.uuWriteUInt8(index, 0x6C) // l
        index += buffer.uuWriteUInt8(index, 0x75) // u
        index += buffer.uuWriteUInt8(index, 0x65) // e
        index += buffer.uuWriteUInt8(index, 0x74) // t
        index += buffer.uuWriteUInt8(index, 0x6F) // o
        index += buffer.uuWriteUInt8(index, 0x6F) // o
        index += buffer.uuWriteUInt8(index, 0x74) // t
        index += buffer.uuWriteUInt8(index, 0x68) // h
        index += buffer.uuWriteUInt8(index, id.value)
        index += buffer.uuWriteUInt32(ByteOrder.LITTLE_ENDIAN, index, data.size.toLong())
        buffer.uuWriteData(index, data)
        return buffer
    }

    companion object
    {
        const val HEADER_SIZE = 16

        fun fromBytes(data: ByteArray): L2CapCommand?
        {
            if (data.size >= HEADER_SIZE)
            {
                var index = 0
                val header = data.uuSubData(0, 11)
                index += (header?.size ?: 0)
                val headerText = header?.uuUtf8()
                if (headerText != "UUBluetooth")
                {
                    return null
                }

                val commandByte = data.uuReadUInt8(index)
                index += Byte.SIZE_BYTES
                val commandId = Id.fromUInt8(commandByte)
                if (commandId == null)
                {
                    return null
                }

                val commandLength = data.uuReadUInt32(ByteOrder.LITTLE_ENDIAN, index).toInt()
                index += UInt.SIZE_BYTES
                val cmd = L2CapCommand(commandId, ByteArray(commandLength))

                val cmdBytesLeft = data.uuSubData(index, data.size - index)
                cmdBytesLeft?.let()
                {
                    cmd.data.uuWriteData(0, it)
                    cmd.bytesReceived = it.size
                }

                return cmd
            }

            return null
        }
    }
}