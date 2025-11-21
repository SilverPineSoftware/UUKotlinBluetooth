package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuDispatch
import com.silverpine.uu.core.uuDispatchMain
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

fun mockError(domain: String, code: Int): UUError
{
    val err = mock(UUError::class.java)
    `when`(err.domain).thenReturn(domain)
    `when`(err.code).thenReturn(code)
    return err
}

fun mockBluetoothError() //: UUError
{
//    val err = mock(UUError::class.java)
//    `when`(err.code).thenReturn(code.rawValue)
//    `when`(err.domain).thenReturn("UUBluetoothError")

    mockkObject(UUBluetoothError)

    // 1) Cover both makeError forms (with explicit null and with any exception)
    //every { UUBluetoothError.makeError(code, any()) } returns err

    every { UUBluetoothError.makeError(any(), any()) } answers {
        mockError("UUBluetoothError", firstArg<UUBluetoothErrorCode>().rawValue)
    }

    /*
    every { UUBluetoothError.makeError(code, null) } returns err
    // Optional: catch-all if different codes are passed
    every { UUBluetoothError.makeError(any(), any()) } returns err
    */

    // 2) Cover the higher-level factories commonly used in your code
    /*every { UUBluetoothError.connectionFailedError() } returns err
    every { UUBluetoothError.gattStatusError(any(), any()) } returns err
    every { UUBluetoothError.alreadyConnectedError() } returns err
    every { UUBluetoothError.preconditionFailedError(any()) } returns err
    */
    // add more here if your code uses others (e.g., timeoutError, preconditionFailedErrorNoMessage, etc.)


    //return err
}

fun mockUUDispatch()
{
    mockkStatic("com.silverpine.uu.core.UUDispatchKt")

    every { uuDispatchMain(any()) } answers {
        val block = firstArg<() -> Unit>()
        block()  // run inline
    }

    every { uuDispatchMain(any(), any()) } answers {
        val block = firstArg<() -> Unit>()
        block()  // run inline
    }

    every { uuDispatch(any()) } answers {
        val block = firstArg<() -> Unit>()
        block()  // run inline
    }

    every { uuDispatch(any(), any()) } answers {
        val block = firstArg<() -> Unit>()
        block()  // run inline
    }
}

fun mockUUBluetoothContext(): Context
{
    val ctx = mock(Context::class.java)

    mockkObject(UUBluetooth)

    every {
        UUBluetooth.requireApplicationContext()
    } returns ctx

    return ctx
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

@Suppress("Deprecation")
fun mockService(uuid: UUID = UUID.randomUUID(), type: Int = android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY): android.bluetooth.BluetoothGattService
{
    val s = mock(android.bluetooth.BluetoothGattService::class.java)
    `when`(s.uuid).thenReturn(uuid)
    `when`(s.type).thenReturn(type)
    `when`(s.includedServices).thenReturn(null)
    `when`(s.characteristics).thenReturn(null)
    return s
}
