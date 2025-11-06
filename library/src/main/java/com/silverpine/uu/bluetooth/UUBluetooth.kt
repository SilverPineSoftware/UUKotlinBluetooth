package com.silverpine.uu.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuToHexData
import java.util.Locale
import java.util.UUID

/*
public class UUBluetooth
{
    private init() {}

    private static var provider: UUBluetoothProvider = UUDefaultProvider()

    public static func setProvider(_ provider: UUBluetoothProvider)
    {
        UUBluetooth.provider = provider
    }

    public static func initializeCoreBluetooth()
    {
        // Simply access singleton will create a CBCentralManager and prompt users for permissions.
        _ = UUCentralManager.shared
    }

    public static var scanner: UUPeripheralScanner
    {
        return provider.scanner
    }

    public static func createSession(peripheral: UUPeripheral) -> UUPeripheralSession
    {
        return provider.createSession(peripheral: peripheral)
    }
}
*/

/**
 * Helpful Bluetooth methods, constants, interfaces
 */
@SuppressWarnings("unused")
object UUBluetooth
{
    //private var provider: UUBluetoothProvider lazy by { UUDefaultProvider() }

    //private lateinit var provider: UUBluetoothProvider// by lazy { UUDefaultProvider() }

    private var provider: UUBluetoothProvider? = null

    /*
    private var provider: UUBluetoothProvider
        get()
        {
            var p = _provider
            if (p == null)
            {
                p = UUDefaultProvider()
                _provider = p
            }

            return p
        }

        set(value)
        {
            _provider = value
        }*/


    fun setProvider(provider: UUBluetoothProvider)
    {
        this.provider = provider
    }

    private fun getProvider(): UUBluetoothProvider
    {
        var p = provider
        if (p == null)
        {
            p = UUDefaultProvider()
            provider = p
        }

        return p
    }

    val scanner: UUPeripheralScanner
        get()
        {
            return getProvider().scanner
        }

    /*fun createSession(peripheral: UUPeripheral): UUPeripheralSession
    {
        return provider.createSession(peripheral)
    }*/












    object Constants
    {
        /**
         * Special constant used to indicate that an RSSI reading is not available.
         */
        val noRssi: Int = 127
    }

    /**
     * Framework defaults.  Calling applications can change these once and they are used for
     * all sessions during the life of the current app.
     */
    object Defaults
    {
        var connectTimeout: Long = 10 * UUDate.Constants.MILLIS_IN_ONE_SECOND
        var disconnectTimeout: Long = 10 * UUDate.Constants.MILLIS_IN_ONE_SECOND
        var operationTimeout: Long = 10 * UUDate.Constants.MILLIS_IN_ONE_SECOND
    }

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
    fun init(applicationContext: Context) //, provider: UUBluetoothProvider = UUDefaultProvider())
    {
        UUBluetooth.applicationContext = applicationContext

        //setProvider(provider)
    }

    fun requireApplicationContext(): Context
    {
        val ctx = applicationContext
        if (ctx == null)
        {
            throw RuntimeException("applicationContext is null. Must call UUBluetooth.init(Context) on app startup.")
        }

        return ctx
    }

    val isBluetoothLeSupported: Result<Boolean>
        get() = runCatching()
        {
            return Result.success(requireApplicationContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        }

    val requiredPermissions: Array<String>
        get()
        {
            return buildList()
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                else
                {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }.toTypedArray()
        }

    /**
     * Checks both manifest and runtime permissions for Bluetooth operations.
     * This is a convenience method that first verifies permissions are declared in the manifest,
     * then checks if they are granted at runtime.
     *
     * @return null if all required permissions are declared in the manifest and granted at runtime,
     * or a UUError with INSUFFICIENT_PERMISSIONS error code if any required permissions are missing
     * from the manifest or not granted at runtime. Returns null if unable to check permissions.
     */
    fun checkPermissions(): UUError?
    {
        return checkManifestPermissions() ?: checkRuntimePermissions()
    }

    /**
     * Checks if the required Bluetooth permissions are granted at runtime by the user.
     * This validates that permissions declared in the manifest have been granted by the user.
     * For Android 6.0+ (API 23+), dangerous permissions require explicit user approval.
     *
     * @return null if all required permissions are granted at runtime, or a UUError with
     * INSUFFICIENT_PERMISSIONS error code if any required permissions are not granted.
     * Returns null if unable to check runtime permissions.
     */
    fun checkRuntimePermissions(): UUError?
    {
        val ctx = requireApplicationContext()

        return runCatching()
        {
            val anyNeeded = requiredPermissions.any()
            { permission ->
                ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED
            }

            return if (anyNeeded)
            {
                UUBluetoothError.makeError(UUBluetoothErrorCode.INSUFFICIENT_PERMISSIONS)
            }
            else
            {
                null
            }
        }.getOrNull()
    }

    /**
     * Checks if the required Bluetooth permissions are declared in the calling application's AndroidManifest.xml.
     * This validates that the app has properly declared the necessary permissions in its manifest,
     * which is required for Bluetooth operations to work correctly.
     *
     * @return null if all required permissions are declared in the manifest, or a UUError with
     * INSUFFICIENT_PERMISSIONS error code if any required permissions are missing from the manifest.
     * Returns null if unable to check the manifest (e.g., package info unavailable).
     */
    fun checkManifestPermissions(): UUError?
    {
        return runCatching()
        {
            val context = requireApplicationContext()
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

            val declaredPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
            val missingPermissions = requiredPermissions.filter { permission ->
                !declaredPermissions.contains(permission)
            }

            if (missingPermissions.isNotEmpty())
            {
                val err = UUBluetoothError.makeError(UUBluetoothErrorCode.INSUFFICIENT_PERMISSIONS)
                err.errorDescription = "Required Bluetooth permissions are missing from AndroidManifest.xml: ${missingPermissions.joinToString(", ")}"
                err
            }
            else
            {
                null
            }
        }.getOrNull()
    }

    val currentState: Result<UUBluetoothState>
        get() = runCatching()
        {
            val bluetoothManager = requireApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            return if (adapter != null)
            {
                Result.success(UUBluetoothState.fromBluetoothState(adapter.state))
            }
            else
            {
                Result.success(UUBluetoothState.UNKNOWN)
            }
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









/**
 * Formats a full 128-bit UUID string from a 16-bit short code string
 *
 * @param shortCode the BTLE short code.  Must be exactly 4 chars long
 * @return a valid UUID string, or null if the short code is not valid.
 */
fun uuShortCodeToFullUuidString(shortCode: String): String
{
    return if (!uuIsValidShortCode(shortCode))
    {
        ""
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
 * @return a UUID, or throws an illegal argument exception if invalid.
 */
fun uuShortCodeToUuid(shortCode: String): UUID
{
    return uuUuidFromString(uuShortCodeToFullUuidString(shortCode))
}

fun UUID.uuToParcelUuid(): ParcelUuid
{
    return ParcelUuid(this)
}

/**
 * Checks a string to see if it is a valid BTLE shortcode
 *
 * @param shortCode the string to check
 * @return true if the string is a valid 2 byte hex value
 */
fun uuIsValidShortCode(shortCode: String?): Boolean
{
    val hex = shortCode?.uuToHexData() ?: return false
    return (hex.size == 2)
}

/**
 * Creates a non null UUID from a string.
 *
 * NOTE! this method just wraps UUID.fromString(string) with non null conversion.  It will throw if
 * passed an invalid string.
 */
fun uuUuidFromString(string: String): UUID
{
    return UUID.fromString(string)
}