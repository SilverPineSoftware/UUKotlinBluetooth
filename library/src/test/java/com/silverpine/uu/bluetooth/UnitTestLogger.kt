package com.silverpine.uu.bluetooth

import com.silverpine.uu.logging.UULogger

class UnitTestLogger: UULogger
{
    var logLines: ArrayList<String> = arrayListOf()

    override fun writeToLog(
        level: Int,
        callingClass: Class<*>,
        method: String,
        message: String,
        exception: Throwable?
    )
    {
        val line = "level: $level, callingClass: ${callingClass.javaClass.name}, method: $method, message: $message, exception: $exception"
        logLines.add(line)
        println(line)
    }
}