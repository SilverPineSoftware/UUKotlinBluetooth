package com.silverpine.uu.bluetooth

import android.os.ParcelUuid

interface UUAdvertisement
{
    val address: String
    val rssi: Int
    val localName: String?
    val isConnectable: Boolean
    val manufacturingData: ByteArray?
    val transmitPower: Int
    val primaryPhy: Int
    val secondaryPhy: Int
    val timestamp: Long
    val services: List<ParcelUuid>?
    val serviceData: Map<ParcelUuid,ByteArray>?
    val solicitedServices: List<ParcelUuid>?
}
