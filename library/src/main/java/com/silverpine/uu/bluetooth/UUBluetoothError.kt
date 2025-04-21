package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGatt
import com.silverpine.uu.core.UUError
import java.util.UUID

/**
 * Container class for UUBluetooth errors
 */
internal object UUBluetoothError
{
    /**
     * Lookup key for errorDetails for the failing underlying bluetooth method name.
     */
    private const val USER_INFO_KEY_METHOD_NAME = "methodName"
    private const val USER_INFO_KEY_MESSAGE = "message"
    private const val USER_INFO_KEY_GATT_STATUS = "gattStatus"
    private const val USER_INFO_KEY_MISSING_CHARACTERISTIC = "missingCharacteristic"
    private const val USER_INFO_KEY_STATUS_CODE = "statusCode"

    private const val DOMAIN = "UUBluetoothError"

    /**
     * Creates a UUBluetoothError
     *
     * @param errorCode error code
     * @param caughtException caught exception
     */
    private fun makeError(errorCode: UUBluetoothErrorCode, caughtException: Exception? = null): UUError
    {
        val err = UUError(errorCode.rawValue, DOMAIN, caughtException)
        err.errorDescription = errorCode.errorDescription
        return err
    }

    /**
     * Wrapper method to return a success error object
     *
     * @return a UUBluetoothError object
     */
    fun success(): UUError
    {
        return makeError(UUBluetoothErrorCode.Success)
    }

    /**
     * Wrapper method to return a not connected error
     *
     * @return a UUBluetoothError object
     */
    fun notConnectedError(): UUError
    {
        return makeError(UUBluetoothErrorCode.NotConnected)
    }

    /**
     * Wrapper method to return a connection failed error
     *
     * @return a UUBluetoothError object
     */
    fun connectionFailedError(): UUError
    {
        return makeError(UUBluetoothErrorCode.ConnectionFailed)
    }

    /**
     * Wrapper method to return a timeout error
     *
     * @return a UUBluetoothError object
     */
    fun timeoutError(): UUError
    {
        return makeError(UUBluetoothErrorCode.Timeout)
    }

    /**
     * Wrapper method to return an already connected error
     *
     * @return a UUBluetoothError object
     */
    fun alreadyConnectedError(): UUError
    {
        return makeError(UUBluetoothErrorCode.AlreadyConnected)
    }

    /**
     * Wrapper method to return an user interrupted error
     *
     * @return a UUBluetoothError object
     */
    fun userInterruptedError(): UUError
    {
        return makeError(UUBluetoothErrorCode.UserInterrupted)
    }

    /**
     * Wrapper method to return a disconnected error
     *
     * @return a UUBluetoothError object
     */
    fun disconnectedError(): UUError
    {
        return makeError(UUBluetoothErrorCode.Disconnected)
    }

    /**
     * Wrapper method to return an underlying Bluetooth method failure.  This is returned when
     * a method returns false or null or othe error condition.
     *
     * @param method the method name
     * @param statusCode optional statusCode
     *
     * @return a UUBluetoothError object
     */
    fun operationFailedError(method: String, statusCode: Int? = null): UUError
    {
        val err = makeError(UUBluetoothErrorCode.OperationFailed)
        err.addUserInfo(USER_INFO_KEY_METHOD_NAME, method)

        statusCode?.let()
        { code ->
            err.addUserInfo(USER_INFO_KEY_STATUS_CODE, "$code")
        }

        err.errorDescription = "Operation Failed: $method"
        return err
    }

    /**
     * Error returned when a required characteristic is missing.
     */
    fun missingRequiredCharacteristic(characteristic: UUID): UUError
    {
        val err = makeError(UUBluetoothErrorCode.OperationFailed)
        err.addUserInfo(USER_INFO_KEY_METHOD_NAME, "requireDiscoveredCharacteristic")
        err.addUserInfo(USER_INFO_KEY_MISSING_CHARACTERISTIC, characteristic.toString())
        err.errorDescription = "Missing required characteristic $characteristic"
        return err
    }

    /**
     * Error returned when a required descriptor is missing.
     */
    fun missingRequiredDescriptor(descriptor: UUID): UUError
    {
        val err = makeError(UUBluetoothErrorCode.OperationFailed)
        err.addUserInfo(USER_INFO_KEY_METHOD_NAME, "requireDiscoveredDescriptor")
        err.addUserInfo(USER_INFO_KEY_MISSING_CHARACTERISTIC, descriptor.toString())
        err.errorDescription = "Missing required descriptor $descriptor"
        return err
    }

    /**
     * Wrapper method to return an error on a pre-condition check.
     *
     * @param message a developer friendly message about the precondition that failed.
     *
     * @return a UUBluetoothError object
     */
    fun preconditionFailedError(message: String): UUError
    {
        val err = makeError(UUBluetoothErrorCode.PreconditionFailed)
        err.addUserInfo(USER_INFO_KEY_MESSAGE, message)
        err.errorDescription = "Precondition Failed: $message"
        return err
    }

    /**
     * Wrapper method to return an underlying Bluetooth method failure.  This is returned when
     * a method returns false or null or other error condition.
     *
     * @param caughtException the exception that caused this error
     *
     * @return a UUBluetoothError object
     */
    fun operationFailedError(caughtException: Exception): UUError
    {
        return makeError(UUBluetoothErrorCode.OperationFailed, caughtException)
    }

    /**
     * Wrapper method to return an underlying Bluetooth method failure.  This is returned when
     * a method returns false or null or othe error condition.
     *
     * @param method the method name
     * @param gattStatus the gatt status at time of failure
     *
     * @return a UUBluetoothError object
     */
    fun gattStatusError(method: String, gattStatus: Int): UUError?
    {
        return if (gattStatus != BluetoothGatt.GATT_SUCCESS)
        {
            val err = makeError(UUBluetoothErrorCode.OperationFailed)
            err.addUserInfo(USER_INFO_KEY_METHOD_NAME, method)
            err.addUserInfo(USER_INFO_KEY_GATT_STATUS, gattStatus.toString())
            err
        }
        else
        {
            null
        }
    }
}