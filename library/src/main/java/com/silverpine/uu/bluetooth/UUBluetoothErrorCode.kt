package com.silverpine.uu.bluetooth

import com.silverpine.uu.bluetooth.UUBluetooth.requireApplicationContext

/**
 * UUBluetooth error codes.
 */
enum class UUBluetoothErrorCode(val rawValue: Int)
{
    /**
     * An operation was successful.
     */
    Success(0),

    /**
     * An operation attempt was manually timed out by UUCoreBluetooth
     */
    Timeout(1),

    /**
     * A method call was not attempted because the BluetoothDevice was not connected.
     */
    NotConnected(2),

    /**
     * A Bluetooth operation failed for some reason. Check caught exception and user info for
     * more information.  This can be returned from any Bluetooth method that throws exceptions
     */
    OperationFailed(3),

    /**
     * A connection attempt failed.
     */
    ConnectionFailed(4),

    /**
     * A peripheral device disconnected
     */
    Disconnected(5),

    /**
     * An operation could not be attempted because one or more preconditions failed.
     */
    PreconditionFailed(6);

    /**
     *
     * @return a developer friendly error description
     */
    val errorDescription: String?
        get()
        {
            val ctx = requireApplicationContext()
            val rez = ctx.resources

            return when (this)
            {
                Timeout -> rez.getString(R.string.error_description_timeout)
                NotConnected -> rez.getString(R.string.error_description_not_connected)
                OperationFailed -> rez.getString(R.string.error_description_operation_failed)
                ConnectionFailed -> rez.getString(R.string.error_description_connection_failed)
                Disconnected -> rez.getString(R.string.error_description_disconnected)
                PreconditionFailed -> rez.getString(R.string.error_description_precondition_failed)
                else -> null
            }
        }
}