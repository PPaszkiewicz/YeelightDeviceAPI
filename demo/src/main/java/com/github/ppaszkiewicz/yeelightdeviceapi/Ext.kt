package com.github.ppaszkiewicz.yeelightdeviceapi

import android.app.Activity
import android.content.Context
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.math.MathUtils


/** Force parse integer value from this EditText returning value within bounds (min 0). */
fun EditText.intValue(max: Int) = intValue(0, max)

/** Force parse integer value from this EditText returning value within bounds. */
fun EditText.intValue(min: Int, max: Int = 1000): Int {
    val text = text.toString()
    return if (text.isEmpty())
        min
    else
        MathUtils.clamp(text.toInt(), min, max)
}

/** */
fun TextView.setText(@StringRes res1: Int, @StringRes res2: Int) {
    text = context.getString(R.string.tips_template, context.getString(res1), context.getString(res2))
}

/** Show toast. Not thread safe.*/
fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

/** Thread safe toast. */
fun Activity.uiToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread { toast(text, duration) }
}

inline fun SeekBar.setOnSeekBarChangeListener(crossinline listener: (Int, Boolean) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            listener(p1, p2)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    })
}