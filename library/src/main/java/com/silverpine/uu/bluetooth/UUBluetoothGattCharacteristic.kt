package com.silverpine.uu.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.silverpine.uu.core.uuIsBitSet
import java.util.Arrays

/**
 * Checks a characteristic property to see if the specified bit is set
 *
 * @param property the property to check
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuIsCharacteristicPropertySet(property: Int): Boolean
{
    return properties.uuIsBitSet(property)
}

/**
 * Checks a characteristic to see if it supports the notify property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanToggleNotify(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
}

/**
 * Checks a characteristic to see if it supports the indicate property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanToggleIndicate(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_INDICATE)
}

/**
 * Checks a characteristic to see if it supports the read data property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanReadData(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_READ)
}

/**
 * Checks a characteristic to see if it supports the write data property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanWriteData(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_WRITE)
}

/**
 * Checks a characteristic to see if it supports the write data without response property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanWriteWithoutResponse(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
}

/**
 * Checks a characteristic to see if it supports the broadcast property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanBroadcast(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_BROADCAST)
}

/**
 * Checks a characteristic to see if it supports the signed write  property
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuCanWriteSigned(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)
}

/**
 * Checks a characteristic to see if it supports extended properties
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuHasExtendedProperties(): Boolean
{
    return uuIsCharacteristicPropertySet(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS)
}


/**
 * Returns a flag indicating if a characteristic is configured for notifications
 *
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuIsNotifying(): Boolean
{
    val descriptor = getDescriptor(UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
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
 * @return true or false
 */
fun BluetoothGattCharacteristic.uuIsIndicating(): Boolean
{
    val descriptor = getDescriptor(UUBluetoothConstants.Descriptors.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)

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