package com.silverpine.uu.bluetooth

/*
interface UUBluetoothProvider
{
    val scanner: UUPeripheralScanner

    fun createSession(peripheral: UUPeripheral): UUPeripheralSession
}*/


/// A provider interface for Core Bluetooth functionality.
/// Conforming types supply initialization, state monitoring, and scanning capabilities.
interface UUBluetoothProvider
{
    /// Sets up the Bluetooth stack, including the central manager and related services.
    fun initialize()

    /// The central manager responsible for discovering and connecting to BLE peripherals.
    // var centralManager: UUCentralManager { get }

    /// Observes and reports changes to the Bluetooth manager’s state.
    // var managerStateMonitor: UUManagerStateMonitor { get }

    /// Scans for and manages discovered Bluetooth peripherals.
    val scanner: UUPeripheralScanner

    /// The current authorization status
    //var authorizationStatus: CBManagerAuthorization { get }
}

/*
public extension UUBluetoothProvider
{
    /// Default implementation retrieving the current authorization status from CoreBluetooth.
    var authorizationStatus: CBManagerAuthorization
    {
        return CBCentralManager.authorization
    }
}*/