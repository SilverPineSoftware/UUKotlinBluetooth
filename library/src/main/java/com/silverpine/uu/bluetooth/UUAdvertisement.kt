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
    val services: Array<ParcelUuid>?
    val serviceData: Map<ParcelUuid,ByteArray>?

    // var overflowServices: [CBUUID]? { get }

//    val solicitedServices: Array<UUID>?
//        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            advertisementData?.scanRecord?.serviceSolicitationUuids?.map { it.uuid }?.toTypedArray()
//        } else {
//            TODO("VERSION.SDK_INT < Q")
//        }
}
