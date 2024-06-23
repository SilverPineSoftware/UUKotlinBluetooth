package com.silverpine.uu.bluetooth

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.silverpine.uu.core.uuDispatchMain
import com.silverpine.uu.ux.UUPermissions

class UUBleTestActivity : AppCompatActivity()
{
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var testNameLabel: AppCompatTextView
    private lateinit var label: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val rootLayoutId = View.generateViewId()
        val testNameLabelId = View.generateViewId()
        val labelId = View.generateViewId()

        rootLayout = ConstraintLayout(this)
        rootLayout.id = rootLayoutId
        rootLayout.setBackgroundColor(Color.argb(255, 123, 123, 123))
        rootLayout.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        testNameLabel = AppCompatTextView(this)
        testNameLabel.id = testNameLabelId
        testNameLabel.setTextColor(Color.argb(255, 0, 0, 0))
        //testNameLabel.setBackgroundColor(Color.argb(255, 0, 100, 0))
        testNameLabel.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        testNameLabel.textAlignment = AppCompatTextView.TEXT_ALIGNMENT_CENTER
        testNameLabel.setTypeface(testNameLabel.typeface, Typeface.BOLD)
        testNameLabel.textSize = 24.0f

        label = AppCompatTextView(this)
        label.id = labelId
        label.setTextColor(Color.argb(255, 0, 0, 0))
        //label.setBackgroundColor(Color.argb(255, 100, 0, 0))
        label.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        label.textSize = 18.0f
        label.movementMethod = ScrollingMovementMethod()
        //label.gravity = Gravity.BOTTOM

        rootLayout.addView(testNameLabel)
        rootLayout.addView(label)
        setContentView(rootLayout)

        val parentId = ConstraintSet.PARENT_ID

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(testNameLabelId, ConstraintSet.TOP, parentId, ConstraintSet.TOP)
        constraintSet.connect(testNameLabelId, ConstraintSet.START, parentId, ConstraintSet.START)
        constraintSet.connect(testNameLabelId, ConstraintSet.END, parentId, ConstraintSet.END)

        constraintSet.connect(labelId, ConstraintSet.START, parentId, ConstraintSet.START)
        constraintSet.connect(labelId, ConstraintSet.END, parentId, ConstraintSet.END)
        constraintSet.connect(labelId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM)

        constraintSet.connect(labelId, ConstraintSet.TOP, testNameLabelId, ConstraintSet.BOTTOM)
        constraintSet.connect(testNameLabelId, ConstraintSet.BOTTOM, labelId, ConstraintSet.TOP)

        constraintSet.setMargin(testNameLabelId, ConstraintSet.TOP, 30)
        constraintSet.setMargin(testNameLabelId, ConstraintSet.BOTTOM, 30)
        constraintSet.setMargin(testNameLabelId, ConstraintSet.START, 30)
        constraintSet.setMargin(testNameLabelId, ConstraintSet.END, 30)

        constraintSet.setMargin(labelId, ConstraintSet.BOTTOM, 30)
        constraintSet.setMargin(labelId, ConstraintSet.START, 30)
        constraintSet.setMargin(labelId, ConstraintSet.END, 30)

        constraintSet.applyTo(rootLayout)

        testNameLabel.text = "Waiting for tests..."
        label.text = ""

        title = "UUKotlinBluetooth"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        UUPermissions.handleRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    fun setTestName(testName: String)
    {
        testNameLabel.text = testName
    }

    fun appendLine(line: String)
    {
        val current = label.text.toString()
        label.text = "${current}\n${line}".trimIndent()

        label.dkScrollToBottom()
    }
}

fun TextView.dkScrollToBottom()
{
    uuDispatchMain()
    {
        layout?.let()
        {
            val yScroll = (it.getLineTop(lineCount) - height).coerceAtLeast(0)
            scrollTo(0, yScroll)
        }
    }
}