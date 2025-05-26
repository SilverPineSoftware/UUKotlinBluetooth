package com.silverpine.uu.bluetooth

import android.util.SparseArray
import java.util.UUID

interface UUAdvertisement
{
    val address: String
    val rssi: Int
    val localName: String
    val isConnectable: Boolean
    val manufacturingData: SparseArray<ByteArray>?
    val transmitPower: Int
    val primaryPhy: Int
    val secondaryPhy: Int
    val timestamp: Long
    val services: List<UUID>?
    val serviceData: Map<UUID,ByteArray>?
    val solicitedServices: List<UUID>?
}
