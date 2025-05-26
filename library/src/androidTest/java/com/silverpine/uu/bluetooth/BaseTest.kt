package com.silverpine.uu.bluetooth

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.test.ui.uuAppendOutputLine
import com.silverpine.uu.test.ui.uuRequestPermissions
import com.silverpine.uu.test.ui.uuSetTestName
import org.junit.Rule

open class BaseTest
{
    @Rule @JvmField
    val activityScenarioRule = ActivityScenarioRule(UUBleTestActivity::class.java)

    protected val context = InstrumentationRegistry.getInstrumentation().targetContext

    protected suspend fun startTest(testName: String)
    {
        appendOutputLine("Starting test")

        activityScenarioRule.uuSetTestName(testName)

        appendOutputLine("Acquiring BLE permissions")
        requestBluetoothPermissions() //activityScenarioRule)
    }

    protected fun appendOutputLine(line: String)
    {
        log("OUTPUT: $line")

        activityScenarioRule.uuAppendOutputLine(line)
    }

    protected fun log(text: String)
    {
        Log.d(javaClass.name, text)
    }

    private fun requestBluetoothPermissions() //(scenarioRule: ActivityScenarioRule<*>)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            //requestBluetoothPermissionsPost31(scenarioRule)

            val result = activityScenarioRule.uuRequestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
        else
        {
            //requestBluetoothPermissionsPre31(scenarioRule)

            val result = activityScenarioRule.uuRequestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    /*
    companion object
    {
        suspend fun requestBluetoothPermissions(scenarioRule: ActivityScenarioRule<*>)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                requestBluetoothPermissionsPost31(scenarioRule)
            }
            else
            {
                requestBluetoothPermissionsPre31(scenarioRule)
            }
        }

        private suspend fun requestBluetoothPermissionsPre31(scenarioRule: ActivityScenarioRule<*>)
        {
            val job = Job()

            scenarioRule.scenario.onActivity()
            { activity ->
                UUPermissions.requestPermissions(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    1234)
                { _: String?, _: Boolean -> job.complete() }
            }

            job.join()
        }

        private suspend fun requestBluetoothPermissionsPost31(scenarioRule: ActivityScenarioRule<*>)
        {
            val job = Job()

            scenarioRule.scenario.onActivity { activity ->
                UUPermissions.requestMultiplePermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    1234
                ) { job.complete() }
            }

            job.join()
        }
    }*/
}
