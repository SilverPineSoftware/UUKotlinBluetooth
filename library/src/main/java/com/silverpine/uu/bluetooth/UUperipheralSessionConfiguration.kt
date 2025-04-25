package com.silverpine.uu.bluetooth

data class UUPeripheralSessionConfiguration(
    var connectTimeout: Long = UUBluetooth.Defaults.connectTimeout,
    var disconnectTimeout: Long = UUBluetooth.Defaults.disconnectTimeout,
    var serviceDiscoveryTimeout: Long = UUBluetooth.Defaults.operationTimeout,
    var readTimeout: Long = UUBluetooth.Defaults.operationTimeout,
    var writeTimeout: Long = UUBluetooth.Defaults.operationTimeout,
)