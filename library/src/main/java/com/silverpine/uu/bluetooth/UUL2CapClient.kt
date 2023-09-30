package com.silverpine.uu.bluetooth

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUTimer

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("MissingPermission")
class UUL2CapClient(private val peripheral: UUPeripheral): UUL2CapChannel()
{
    companion object
    {
        const val CONNECT_WATCHDOG_TIMER_ID = "ConnectWatchdog"
    }

    fun connect(
        psm: Int,
        secure: Boolean,
        timeout: Long,
        completion: (UUError?) -> Unit)
    {
        workerThread.post()
        {
            val timerId = timerId(CONNECT_WATCHDOG_TIMER_ID)

            UUTimer.startTimer(timerId, timeout, null)
            { _, _ ->
                debugLog("connect", "L2Cap Connect Timed Out: $peripheral, psm: $psm, secure: $secure")
                notifyCallback(UUBluetoothError.timeoutError(), completion)
            }

            var err: UUError? = null

            try
            {
                val sock = if (secure)
                {
                    peripheral.bluetoothDevice.createL2capChannel(psm)
                }
                else
                {
                    peripheral.bluetoothDevice.createInsecureL2capChannel(psm)
                }

                sock.connect()
                socket = sock
            }
            catch (ex: Exception)
            {
                err = UUBluetoothError.operationFailedError(ex)
                logException("connect", ex)
            }

            UUTimer.cancelActiveTimer(timerId)
            notifyCallback(err, completion)
        }
    }

    fun sendCommand(
        command: ByteArray,
        sendTimeout: Long,
        receiveTimeout: Long,
        expectedBytes: Int,
        completion: (ByteArray?, UUError?)->Unit)
    {
        write(command, sendTimeout)
        { txErr ->

            if (txErr != null)
            {
                completion(null, txErr)
                return@write
            }

            read(receiveTimeout, expectedBytes, completion)
        }
    }
}