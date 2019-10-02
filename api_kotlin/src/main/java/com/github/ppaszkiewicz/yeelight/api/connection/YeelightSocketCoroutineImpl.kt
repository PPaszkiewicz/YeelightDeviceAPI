package com.github.ppaszkiewicz.yeelight.api.connection

import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightSocket
import kotlinx.coroutines.*

/** Yeelight socket implemented on coroutines instead of threads. */
open class YeelightSocketCoroutineImpl<T : YeelightConnection>(
        val connection: T
) : YeelightSocket<T>(connection), CoroutineScope {
    companion object {
        const val TAG = "YSocketCoroutine"
    }

    override val coroutineContext = SupervisorJob()
    private var listenerJob: Job? = null

    override fun writeAsync(vararg msg: YeelightCommand) {
        launch(Dispatchers.IO) { writeImpl(*msg) }
    }

    override fun startListeningOnAsync() {
        listenerJob = launch(Dispatchers.IO) {
            try {
                startBlockingConnection()
            }catch (e : Exception){
             //   e.printStackTrace()
            }
        }
    }

    override fun onLoopReadFinished(throwable: Throwable?) {
        listenerJob = null
    }

    override fun isAsyncRunning(): Boolean {
        return listenerJob?.isActive == true
    }

    override fun isInterrupted(): Boolean {
        return listenerJob?.isActive == false
    }
}