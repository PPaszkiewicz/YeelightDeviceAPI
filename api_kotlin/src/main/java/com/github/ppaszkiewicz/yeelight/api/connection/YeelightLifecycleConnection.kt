package com.github.ppaszkiewicz.yeelight.api.connection

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.github.ppaszkiewicz.yeelight.core.YLog
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool

/** Lifecycle aware connection that starts/pauses alongside lifecycle. */
open class YeelightLifecycleConnection(lifecycle: Lifecycle, device: YeelightDevice) : YeelightBasicConnectionKt(device), LifecycleObserver {
    init {
        lifecycle.addObserver(this)
    }

    /** Connection pool of [YeelightLifecycleConnection]. When [lifecycleOwner] gets destroyed, this connection pool is automatically released. */
    open class PoolProvider(val lifecycleOwner: LifecycleOwner) : YeelightConnectionPool<YeelightLifecycleConnection>() , LifecycleObserver {
        init {
            lifecycleOwner.lifecycle.addObserver(this)
        }
        override fun instantiateConnection(device: YeelightDevice) = YeelightLifecycleConnection(lifecycleOwner.lifecycle, device)

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy(){
            release()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart(){
        YLog.d("YLifeConn", "connecting")
        connect()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop(){
        YLog.d("YLifeConn", "disconnecting: $isConnected")
        tryDisconnect()
    }

    override fun onRelease() {
        super.onRelease()
        // release any listeners set by the user
        connectionListener = null
    }
}