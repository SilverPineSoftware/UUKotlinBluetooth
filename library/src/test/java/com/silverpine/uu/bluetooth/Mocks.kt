package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer
import io.mockk.every
import io.mockk.mockkObject
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

fun mockNextBluetoothError(code: UUBluetoothErrorCode): UUError
{
    val err = mock(UUError::class.java)
    `when`(err.code).thenReturn(code.rawValue)
    `when`(err.domain).thenReturn("UUBluetoothError")

    mockkObject(UUBluetoothError)

    every {
        UUBluetoothError.makeError(code, any())
    } returns err

    return err
}

fun mockTimer(timerId: String): UUTimer
{
    val obj = mock(UUTimer::class.java)
    //`when`(obj.timerId).thenReturn(timerId)

    mockkObject(UUTimer)

    every {
        UUTimer.findActiveTimer(timerId)
    } returns obj

    return obj
}

fun mockGatt(): BluetoothGatt
{
    return mock(BluetoothGatt::class.java)
}

fun mockDevice(address: String): BluetoothDevice
{
    val m: BluetoothDevice = mock(BluetoothDevice::class.java)

    `when`(m.address).thenReturn(address)

    return m
}

@Suppress("Deprecation")
fun mockCharacteristic(uuid: UUID = UUID.randomUUID(), data: ByteArray? = byteArrayOf(0x00)): BluetoothGattCharacteristic
{
    val ch = mock(BluetoothGattCharacteristic::class.java)
    `when`(ch.uuid).thenReturn(uuid)
    `when`(ch.value).thenReturn(data)
    return ch
}

@Suppress("Deprecation")
fun mockDescriptor(uuid: UUID = UUID.randomUUID(), data: ByteArray? = byteArrayOf(0x00), characteristic: BluetoothGattCharacteristic = mockCharacteristic()): BluetoothGattDescriptor
{
    val d = mock(BluetoothGattDescriptor::class.java)
    `when`(d.uuid).thenReturn(uuid)
    `when`(d.value).thenReturn(data)
    `when`(d.characteristic).thenReturn(characteristic)
    return d
}
