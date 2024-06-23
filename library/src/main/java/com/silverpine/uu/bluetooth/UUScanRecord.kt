package com.silverpine.uu.bluetooth

import android.bluetooth.le.ScanResult
import com.silverpine.uu.core.uuReadUInt8
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.logging.UULog


/*
when (dataType.toByte())
{
    UUPeripheral.DATA_TYPE_FLAGS ->
    {
        parseFlags(data)
    }

    UUPeripheral.DATA_TYPE_MANUFACTURING_DATA ->
    {
        manufacturingData = data
    }

    UUPeripheral.DATA_TYPE_COMPLETE_LOCAL_NAME ->
    {
        localName = data.uuUtf8()
    }

    UUPeripheral.DATA_TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,
    UUPeripheral.DATA_TYPE_COMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS ->
    {
        parseServiceUuid(data, 2)
    }

    UUPeripheral.DATA_TYPE_COMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
    UUPeripheral.DATA_TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS ->
    {
        parseServiceUuid(data, 16)
    }
}
*/

class UUScanRecordPart(val dataType: Byte, val data: ByteArray)
{

}

class UUScanRecord(private val scanResult: ScanResult)
{
    val records: ArrayList<UUScanRecordPart> = arrayListOf()

    init
    {
        parseScanRecord()
    }

    private fun parseScanRecord()
    {
        val bytes = scanResult.scanRecord?.bytes ?: return

        var index = 0
        while (index < bytes.size)
        {
            val length = bytes.uuReadUInt8(index)
            index += Byte.SIZE_BYTES
            if (length == 0)
            {
                break
            }

            val dataType = bytes.uuReadUInt8(index)
            index += Byte.SIZE_BYTES

            val dataLength = length - 1
            val data = bytes.uuSubData(index, dataLength)
            if (data == null)
            {
                UULog.d(javaClass, "parseScanRecord", "Unable to get data chunk at index $index with count $dataLength")
                break
            }

            if (data.size != dataLength)
            {
                UULog.d(javaClass, "parseScanRecord", "Data length is wrong. Expected $dataLength but got ${data.size}")
            }

            index += data.size
            records.add(UUScanRecordPart(dataType.toByte(), data))
        }
    }
}