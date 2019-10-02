package com.github.ppaszkiewicz.yeelight.api.connection

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection

/** Wrapper for [YeelightConnection.Listener] that will discard wrapped listener once lifecycle is destroyed. */
open class YLifecycleListener(listener: YeelightConnection.Listener?) : YeelightConnection.ListenerDelegate(listener), LifecycleObserver{
    /** True if wrapped listener was discarded due to lifecycle destruction. */
    var isDestroyed = false
        private set

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onLifecycleDestroyed(){
        listener = null
        isDestroyed = true
    }
}

/** Wrapper for [YeelightCommand.Listener] that will discard wrapped listener once lifecycle is destroyed. */
open class YCommandLifecycleListener(listener: YeelightCommand.Listener?) : YeelightCommand.ListenerDelegate(listener), LifecycleObserver{
    /** True if wrapped listener was discarded due to lifecycle destruction. */
    var isDestroyed = false
        private set

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onLifecycleDestroyed(){
        listener = null
        isDestroyed = true
    }
}