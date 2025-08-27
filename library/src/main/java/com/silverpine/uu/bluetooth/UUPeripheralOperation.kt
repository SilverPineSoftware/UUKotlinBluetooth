package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog

open class UUPeripheralOperation<Result>(
    val peripheral: UUPeripheral,
    configuration: UUPeripheralSessionConfiguration = UUPeripheralSessionConfiguration())
{
    protected var session: UUPeripheralSession
        private set

    private var operationCallback: ((Result?, UUError?)->Unit)? = null

    var operationResult: Result? = null
        private set

    init
    {
        session = UUPeripheralSession(peripheral)
        session.configuration = configuration
        session.started =
        { session ->
            internalExecute()
        }

        session.ended =
        { session, error ->

            try
            {
                operationCallback?.invoke(operationResult, error)
            }
            catch (ex: Exception)
            {
                UULog.d(javaClass, "safeNotify", "", ex)
            }
        }
    }

    fun start(completion: ((Result?, UUError?)->Unit))
    {
        operationCallback = completion
        session.start()
    }

    fun end(result: Result?, error: UUError?)
    {
        //UULog.debug(tag: LOG_TAG, message: "**** Ending Operation with result: \(String(describing: result)),  error: \(error?.localizedDescription ?? "nil")")
        operationResult = result

        session.end(error)
    }

    open fun execute(completion: (Result?, UUError?)->Unit)
    {
        completion(null, null)
    }

    private fun internalExecute()
    {
        execute()
        { result, err ->
            end(result, err)
        }
    }
}


