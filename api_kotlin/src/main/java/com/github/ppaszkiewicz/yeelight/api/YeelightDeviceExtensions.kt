package com.github.ppaszkiewicz.yeelight.api

import android.animation.ArgbEvaluator
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import com.github.ppaszkiewicz.yeelight.api.connection.YCommandLifecycleListener
import com.github.ppaszkiewicz.yeelight.api.connection.YLifecycleListener
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp

/** Color used to display minimum possible temperature. */
private const val orangeColor: Int = 0x00FF6400 or (0xFF shl 24)

/** Current color resolved by checking mode. Might return black on errors. */
fun YeelightDevice.currentColor(): Int {
    return when (colorMode) {
        YeelightProp.ColorMode.mode_rgb -> {
            if (color == YeelightDevice.UNDEFINED_VALUE)
                Color.BLACK
            else
                color
        }
        YeelightProp.ColorMode.mode_unknown -> Color.BLACK
        YeelightProp.ColorMode.mode_color_temp -> colorFromTemp(colorTemp)
        YeelightProp.ColorMode.mode_hsv -> Color.HSVToColor(hsvColor)
        null -> Color.BLACK // should never be returned (mode_unknown expected)
    }
}

/** Estimation of color for given [temp]. By default 1700K is [orangeColor] and 6500K is [Color.WHITE]. */
fun colorFromTemp(temp: Int, color1700 : Int = orangeColor, color6500 : Int = Color.WHITE): Int {
    val t = temp.coerceIn(YeelightDevice.TEMP_MIN, YeelightDevice.TEMP_MAX) - YeelightDevice.TEMP_MIN
    val prog = t / (YeelightDevice.TEMP_MAX - YeelightDevice.TEMP_MIN).toFloat()
    return ArgbEvaluator().evaluate(prog, color1700, color6500) as Int
}

/** Convert this boolean value to "ON" or "OFF". */
fun Boolean.onOff() = if (this) "ON" else "OFF"

/* ------------------------ Lifecycle listener creators below. --------------------------------- */

/**
 * Set listener that will be automatically destroyed once lifecycle is destroyed as well.
 *
 * This is recommended when setting up listeners to devices kept in a ViewModel.
 * */
fun YeelightDevice.setLifecycleAwareListener(lifecycleOwner: LifecycleOwner, listener: YeelightConnection.Listener) {
    setConnectionListener(listener.wrapWithLifecycle(lifecycleOwner))
}

/** Wrap this listener with lifecycle aware object that will discard this listener when lifecycle gets destroyed. */
fun YeelightConnection.Listener.wrapWithLifecycle(lifecycleOwner: LifecycleOwner) =
        YLifecycleListener(this).apply { lifecycleOwner.lifecycle.addObserver(this) }

/** Setup listener for this command that will be automatically discarded based on provided lifecycle. */
fun YeelightCommand.onReply(lifecycleOwner: LifecycleOwner, onReply: YeelightCommand.Listener) = apply {
    val lc = YCommandLifecycleListener(onReply)
    lifecycleOwner.lifecycle.addObserver(lc)
    onReply(lc)
}

/** Setup listener for this command that will be automatically discarded based on provided lifecycle. */
fun YeelightCommand.onReply(lifecycleOwner: LifecycleOwner, onReply: (YeelightReply) -> Unit) =
        onReply(lifecycleOwner, YeelightCommand.Listener(onReply))