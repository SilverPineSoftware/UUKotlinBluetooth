package com.silverpine.uu.bluetooth

import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogLevel
import com.silverpine.uu.logging.UULogWriter
import com.silverpine.uu.logging.UULogger

class UnitTestLogger: UULogWriter
{
    var logLines: ArrayList<String> = arrayListOf()

    override fun writeToLog(
        level: UULogLevel,
        tag: String,
        message: String
    )
    {
        val line = "level: $level, tag: $tag, message: $message"
        logLines.add(line)
        println(line)
    }

    companion object
    {
        fun init(): UnitTestLogger
        {
            val logWriter = UnitTestLogger()
            val logger = UULogger(logWriter)
            logger.logLevel = UULogLevel.VERBOSE
            UULog.setLogger(logger)
            return logWriter
        }
    }
}