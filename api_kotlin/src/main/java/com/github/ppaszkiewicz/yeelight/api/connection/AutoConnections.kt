package com.github.ppaszkiewicz.yeelight.api.connection

import android.os.Handler
import android.os.Looper
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightAutoConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightSocket
import kotlinx.coroutines.*

/*
    More connection types to use.
 */

/**
 * Implementation that uses coroutine dispatchers to host connection and
 * determine when connection times out.
 * */
open class YeelightAutoConnectionKt(device: YeelightDevice)
    : YeelightAutoConnection(device) {
    companion object {
        const val CONN_TIMEOUT_BASE_MS = 3000L    // base timeout for connection
    }

    /** Connection pool of [YeelightAutoConnectionKt]. */
    open class PoolProvider : YeelightConnectionPool<YeelightAutoConnectionKt>() {
        override fun instantiateConnection(device: YeelightDevice) = YeelightAutoConnectionKt(device)
    }

    override fun createSocket(): YeelightSocket<YeelightAutoConnection> {
        return YeelightSocketCoroutineImpl(this)
    }

    private var timeoutJob: Job? = null

    override fun startTimeout(runnable: Runnable) {
        timeoutJob?.cancel()
        // dispatch to UI thread to prevent async issues
        timeoutJob = GlobalScope.launch(Dispatchers.Main) {
            delay(CONN_TIMEOUT_BASE_MS)
            if (isActive)
                runnable.run()
        }
    }

    override fun cancelTimeout(runnable: Runnable) {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}

/**
 * Implementation that uses android looper to determine when connection times out.
 * */
open class YeelightAutoConnectionHandlers(device: YeelightDevice)
    : YeelightAutoConnection(device) {
    companion object {
        const val CONN_TIMEOUT_BASE_MS = 3000L    // base timeout for connection
    }

    /** Connection pool of [YeelightAutoConnectionHandlers]. */
    open class PoolProvider : YeelightConnectionPool<YeelightAutoConnectionHandlers>() {
        override fun instantiateConnection(device: YeelightDevice) = YeelightAutoConnectionHandlers(device)
    }

    private val mHandler = Handler(Looper.getMainLooper())   //timeout handler works on UI thread

    override fun startTimeout(runnable: Runnable) {
        mHandler.removeCallbacks(runnable)
        mHandler.postDelayed(runnable, CONN_TIMEOUT_BASE_MS)
    }

    override fun cancelTimeout(runnable: Runnable) {
        mHandler.removeCallbacks(runnable)
    }
}