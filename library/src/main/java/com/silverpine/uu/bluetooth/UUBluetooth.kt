package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.silverpine.uu.core.uuIsBitSet
import com.silverpine.uu.core.uuToHexData
import com.silverpine.uu.logging.UULog
import java.util.Arrays
import java.util.Locale
import java.util.UUID

/**
 * Helpful Bluetooth methods, constants, interfaces
 */
@SuppressWarnings("unused")
object UUBluetooth
{
    /**
     * Gets the current framework version
     *
     * @since 1.0.0
     */
    val BUILD_VERSION: String = BuildConfig.BUILD_VERSION

    /**
     * Returns the build branch
     *
     * @since 1.0.0
     */
    val BUILD_BRANCH: String = BuildConfig.BUILD_BRANCH

    /**
     * Returns the full hash of the Git latest git commit
     *
     * @since 1.0.0
     */
    val BUILD_COMMIT_HASH: String = BuildConfig.BUILD_COMMIT_HASH

    /**
     * Returns the date the framework was built.
     *
     * @since 1.0.0
     */
    val BUILD_DATE: String = BuildConfig.BUILD_DATE

    /**
     * Formats a full 128-bit UUID string from a 16-bit short code string
     *
     * @param shortCode the BTLE short code.  Must be exactly 4 chars long
     * @return a valid UUID string, or null if the short code is not valid.
     */
    fun shortCodeToFullUuidString(shortCode: String): String?
    {
        return if (!isValidShortCode(shortCode))
        {
            null
        }
        else String.format(
            Locale.US,
            UUBluetoothConstants.BLUETOOTH_UUID_SHORTCODE_FORMAT,
            shortCode
        )
    }

    /**
     * Creates a UUID object from a UUID short code string
     *
     * @param shortCode the short code
     * @return a UUID, or null if the short code is not valid.
     */
    fun shortCodeToUuid(shortCode: String): UUID?
    {
        try
        {
            val str = shortCodeToFullUuidString(shortCode) ?: return null
            return UUID.fromString(str)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "shortCodeToUuid", "", ex)
        }

        return null
    }

    /**
     * Checks a string to see if it is a valid BTLE shortcode
     *
     * @param shortCode the string to check
     * @return true if the string is a valid 2 byte hex value
     */
    fun isValidShortCode(shortCode: String?): Boolean
    {
        val hex = shortCode?.uuToHexData() ?: return false
        return (hex.size == 2)
    }

    /**
     * Returns a developer friendly string for a BluetoothGatt.GATT_* response code
     *
     * @param gattStatus any integer, but assumes one of BluetoothGatt.GATT_* codes
     * @return a string
     */
    fun gattStatusToString(gattStatus: Int): String
    {
        return when (gattStatus)
        {
            BluetoothGatt.GATT_SUCCESS -> "Success"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "ReadNotPermitted"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "WriteNotPermitted"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "InsufficientAuthentication"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "RequestNotSupported"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "InsufficientEncryption"
            BluetoothGatt.GATT_INVALID_OFFSET -> "InvalidOffset"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "InvalidAttributeLength"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "ConnectionCongested"
            BluetoothGatt.GATT_FAILURE -> "Failure"
            UUBluetoothConstants.GATT_ERROR -> "GattError"
            UUBluetoothConstants.GATT_DISCONNECTED_BY_PERIPHERAL -> "DisconnectedByPeripheral"
            else -> String.format(Locale.US, "Unknown-%d", gattStatus)
        }
    }

    /**
     * Returns a developer friendly string for a BluetoothProfile.STATE_* response code
     *
     * @param connectionState any integer, but assumes one of BluetoothProfile.STATE_* codes
     * @return a string
     */
    fun connectionStateToString(connectionState: Int): String
    {
        return when (connectionState)
        {
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            else -> String.format(Locale.US, "Unknown-%d", connectionState)
        }
    }

    /**
     * Returns a developer friendly string for a BluetoothDevice.DEVICE_TYPE_* value
     *
     * @param deviceType the device type
     *
     * @return a string
     */
    fun deviceTypeToString(deviceType: Int): String
    {
        return when (deviceType)
        {
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "LE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> String.format(Locale.US, "Unknown-%d", deviceType)
        }
    }

    /**
     * Returns a developer friendly string for a BluetoothDevice_BOND_* value
     *
     * @param bondState a bond state value
     *
     * @return a string
     */
    fun bondStateToString(bondState: Int): String
    {
        return when (bondState)
        {
            BluetoothDevice.BOND_NONE -> "None"
            BluetoothDevice.BOND_BONDED -> "Bonded"
            BluetoothDevice.BOND_BONDING -> "Bonding"
            else -> String.format(Locale.US, "Unknown-%d", bondState)
        }
    }

    /**
     * Returns a developer friendly string for a BluetoothAdapter.STATE* power state value
     *
     * @param powerState a bond state value
     *
     * @return a string
     */
    fun powerStateToString(powerState: Int): String
    {
        return when (powerState)
        {
            BluetoothAdapter.STATE_OFF -> "Off"
            BluetoothAdapter.STATE_ON -> "On"
            BluetoothAdapter.STATE_TURNING_ON -> "TurningOn"
            BluetoothAdapter.STATE_TURNING_OFF -> "TurningOff"
            else -> String.format(Locale.US, "Unknown-%d", powerState)
        }
    }

    /**
     * Returns a common name for a Bluetooth UUID.  These strings are directly
     * from the bluetooth.org website
     *
     * @param uuid the UUID to check
     * @return a string
     */
    fun bluetoothSpecName(uuid: UUID?): String
    {
        if (uuid == null)
        {
            return "Unknown"
        }

        return if (UUBluetoothConstants.BLUETOOTH_SPEC_NAMES.containsKey(uuid))
        {
            UUBluetoothConstants.BLUETOOTH_SPEC_NAMES[uuid] ?: ""
        } else "Unknown"
    }

    fun addBluetoothSpecName(uuid: UUID, name: String)
    {
        UUBluetoothConstants.BLUETOOTH_SPEC_NAMES[uuid] = name
    }

    /**
     * Formats a string with human friendly properties of BluetoothGattCharacteristics
     *
     * @param properties properties bitmask
     * @return a string
     */
    fun characteristicPropertiesToString(properties: Int): String
    {
        val parts = ArrayList<Any>()

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_BROADCAST))
        {
            parts.add("Broadcast")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_READ))
        {
            parts.add("Read")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))
        {
            parts.add("WriteWithoutResponse")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_WRITE))
        {
            parts.add("Write")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_NOTIFY))
        {
            parts.add("Notify")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_INDICATE))
        {
            parts.add("Indicate")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE))
        {
            parts.add("SignedWrite")
        }

        if (properties.uuIsBitSet(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS))
        {
            parts.add("ExtendedProperties")
        }

        return parts.joinToString(", ")
    }

    /**
     * Formats a string with human friendly permissions of BluetoothGattCharacteristics
     *
     * @param permissions permissions bitmask
     * @return a string
     */
    fun characteristicPermissionsToString(permissions: Int): String
    {
        val parts = ArrayList<Any>()

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_READ))
        {
            parts.add("Read")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED))
        {
            parts.add("ReadEncrypted")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM))
        {
            parts.add("ReadEncryptedMITM")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_WRITE))
        {
            parts.add("Write")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED))
        {
            parts.add("WriteEncrypted")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM))
        {
            parts.add("WriteEncryptedMITM")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED))
        {
            parts.add("WriteSigned")
        }

        if (permissions.uuIsBitSet(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM))
        {
            parts.add("WriteSignedMITM")
        }

        return parts.joinToString(", ")
    }

    /**
     * Returns a flag indicating if a characteristic is configured for notifications
     *
     * @param characteristic the characteristic to check
     *
     * @return true or false
     */
    fun isNotifying(characteristic: BluetoothGattCharacteristic): Boolean
    {
        val descriptor = characteristic.getDescriptor(UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
        if (descriptor != null)
        {
            val data = descriptor.value
            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, data))
            {
                return true
            }
        }

        return false
    }

    /**
     * Returns a flag indicating if a characteristic is configured for indications
     *
     * @param characteristic the characteristic to check
     *
     * @return true or false
     */
    fun isIndicating(characteristic: BluetoothGattCharacteristic): Boolean
    {
        val descriptor = characteristic.getDescriptor(UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)

        if (descriptor != null)
        {
            val data = descriptor.value
            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, data))
            {
                return true
            }
        }

        return false
    }

    /**
     * Checks a characteristic property to see if the specified bit is set
     *
     * @param characteristic the characteristic to check
     * @param property the property to check
     * @return true or false
     */
    private fun isCharacteristicPropertySet(characteristic: BluetoothGattCharacteristic, property: Int): Boolean
    {
        return characteristic.properties.uuIsBitSet(property)
    }

    /**
     * Checks a characteristic to see if it supports the notify property
     *
     * @param characteristic the characteristic to check
     * @return true or false
     */
    fun canToggleNotify(characteristic: BluetoothGattCharacteristic): Boolean
    {
        return isCharacteristicPropertySet(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)
    }

    /**
     * Checks a characteristic to see if it supports the indicate property
     *
     * @param characteristic the characteristic to check
     * @return true or false
     */
    fun canToggleIndicate(characteristic: BluetoothGattCharacteristic): Boolean
    {
        return isCharacteristicPropertySet(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE)
    }

    /**
     * Checks a characteristic to see if it supports the read data property
     *
     * @param characteristic the characteristic to check
     * @return true or false
     */
    fun canReadData(characteristic: BluetoothGattCharacteristic): Boolean
    {
        return isCharacteristicPropertySet(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)
    }

    /**
     * Checks a characteristic to see if it supports the write data property
     *
     * @param characteristic the characteristic to check
     * @return true or false
     */
    fun canWriteData(characteristic: BluetoothGattCharacteristic): Boolean
    {
        return isCharacteristicPropertySet(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)
    }

    /**
     * Checks a characteristic to see if it supports the write data without response property
     *
     * @param characteristic the characteristic to check
     * @return true or false
     */
    fun canWriteWithoutResponse(characteristic: BluetoothGattCharacteristic): Boolean
    {
        return isCharacteristicPropertySet(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Initialization
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private var applicationContext: Context? = null

    /**
     * One time library initialization.  Must be called prior to using any other UUAndroidBluetooth
     * classes or methods.  Pass an applicationContext only.
     *
     * @param applicationContext application context
     */
    fun init(applicationContext: Context)
    {
        UUBluetooth.applicationContext = applicationContext
    }

    fun requireApplicationContext(): Context
    {
        if (applicationContext == null)
        {
            throw RuntimeException("applicationContext is null. Must call UUBluetooth.init(Context) on app startup.")
        }

        return applicationContext!!
    }

    val isBluetoothLeSupported: Boolean
        get() = try
        {
            requireApplicationContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }
        catch (ex: Exception)
        {
            false
        }

    fun getBluetoothState(context: Context): Int?
    {
        try
        {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null)
            {
                return adapter.state
            }
        }
        catch (ex: Exception)
        {
            // Eat it
        }

        return null
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // L2Cap Support
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun listenForL2CapConnection(secure: Boolean): BluetoothServerSocket
    {
        val context = requireApplicationContext()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        return if (secure)
        {
            bluetoothAdapter.listenUsingL2capChannel()
        }
        else
        {
            bluetoothAdapter.listenUsingInsecureL2capChannel()
        }
    }
}