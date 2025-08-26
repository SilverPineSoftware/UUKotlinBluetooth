package com.silverpine.uu.bluetooth

import com.silverpine.uu.core.UUTimerThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class UnitTestTimerThread : UUTimerThread
{
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val tasks = ConcurrentHashMap<Runnable, ScheduledFuture<*>>()

    override fun postDelayed(interval: Long, runnable: Runnable)
    {
        val future = executor.schedule({
            try
            {
                runnable.run()
            }
            finally
            {
                tasks.remove(runnable)
            }
        }, interval, TimeUnit.MILLISECONDS)
        tasks[runnable] = future
    }

    override fun remove(runnable: Runnable)
    {
        tasks.remove(runnable)?.cancel(false)
    }

    /** Call this at the end of a test to stop the executor cleanly. */
    fun shutdown()
    {

        executor.shutdownNow()
        tasks.clear()
    }
}