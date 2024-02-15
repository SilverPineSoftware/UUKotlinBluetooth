package com.silverpine.uu.bluetooth

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
    PreconditionFailed(6),

    /**
     * An operation could not be attempted because another connection is already active for the given
     * BluetoothDevice
     */
    AlreadyConnected(7),

    /**
     * An operation was interrupted by the user
     */
    UserInterrupted(8);

    /**
     *
     * @return a developer friendly error description
     */
    val errorDescription: String
        get()
        {
            return when (this)
            {
                Timeout -> "The operation timed out."
                NotConnected -> "The BluetoothDevice is not connected."
                OperationFailed -> "The operation failed."
                ConnectionFailed -> "The connection attempt failed."
                Disconnected -> "The peripheral was disconnected."
                PreconditionFailed -> "Unable to perform the operation."
                else -> this.toString()
            }
        }
}