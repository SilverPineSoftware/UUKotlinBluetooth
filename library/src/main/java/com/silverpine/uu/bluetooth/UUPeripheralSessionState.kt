package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUValueBackedEnum

/** Describes the states of a [UUPeripheralSession]. */
enum class UUPeripheralSessionState(override val value: Int): UUValueBackedEnum<Int>
{
    /** The session has not started. */
    NOT_STARTED(0),

    /** The session is attempting to establish a BLE connection to the peripheral. */
    CONNECTING(1),

    /** The session is discovering services on the peripheral. */
    SERVICE_DISCOVERY(2),

    /** The session is discovering characteristics on the peripheral. */
    CHARACTERISTIC_DISCOVERY(3),

    /** The session is discovering descriptors on the peripheral. */
    DESCRIPTOR_DISCOVERY(4),

    /** The session is waiting for subclass-specific work after connecting. */
    FINISH_SESSION_START(5),

    /** The session is ready to use. */
    STARTED(6),

    /** The session is disconnecting. */
    DISCONNECTING(7),

    /** The session has ended. */
    ENDED(8),
}