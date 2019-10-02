package com.github.ppaszkiewicz.yeelight.api

import android.util.Log
import com.github.ppaszkiewicz.yeelight.core.YLog

/** [YLog] for android logcat printing. */
class YLogCat : YLog{
    constructor() : super()
    constructor(isEnabled: Boolean) : super(isEnabled)

    /** Install this object as YLogger. Should keep reference to this to prevent GC. */
    fun install() : YLogCat{
        setInstance(this)
        return this
    }

    override fun error(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }
}