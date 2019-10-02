package com.github.ppaszkiewicz.yeelight.api

import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap
import com.github.ppaszkiewicz.yeelight.core.YeelightScanner
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionProvider
import kotlinx.coroutines.*

/**
 * Device scanner implementation with coroutines wrapper for async implementation.
 *
 * @param scope coroutine scope to use
 * @param connectionProvider optional connection provider to inject into all obtained devices
 * */
class YeelightScannerAsync : YeelightScanner {
    /** Scope used by all jobs of this scanner. */
    private val scope: CoroutineScope

    // internal tracker of current async task
    private var currentJob: Job? = null

    constructor(connectionProvider: YeelightConnectionProvider<*>?) : super(connectionProvider) {
        scope = GlobalScope
    }

    constructor(scope: CoroutineScope, connectionProvider: YeelightConnectionProvider<*>?) : super(connectionProvider) {
        this.scope = scope
    }


    /** Discover devices asynchronously (using coroutines). [block] will be invoked on UI thread when
     * results are ready. */
    fun discoverDevices(timeout: Int = DEFAULT_TIMEOUT_MS, block: (YeelightDeviceMap) -> Unit) {
        if (isScanning()) return  // no need to scan since scan is in progress
        stop()
        isScanning.set(true)
        currentJob = scope.launch(Dispatchers.Main) {
            doDiscoverDevices(timeout, block)
        }
    }

    /** Start listening for announcement from devices. [block] will be invoked on UI thread. */
    fun startListeningAsync(block: (YeelightDevice) -> Unit) {
        currentJob = scope.launch(Dispatchers.Main) {
            doListen(block.toUIListener())
        }
    }

    /** Start listening for announcement from devices. This will only work properly if announcement listener
     * was set earlier. */
    fun startListeningAsync() {
        stop()
        currentJob = scope.launch(Dispatchers.Main) {
            doListen()
        }
    }

    /** Discover devices asynchronously and start listening afterwards. */
    fun discoverAndStartListeningAsync(timeout: Int = DEFAULT_TIMEOUT_MS, scanResult: (YeelightDeviceMap) -> Unit,
                                       announcementListener: (YeelightDevice) -> Unit) {
        stop()
        isScanning.set(true)
        currentJob = scope.launch(Dispatchers.Main) {
            doDiscoverDevices(timeout, scanResult)
            doListen(announcementListener.toUIListener())
        }
    }

    /** Rescan for any devices asynchronously and deploy them to [block] on UI thread.
     * If scan is currently in progress this does nothing and returns false. */
    fun rescan(timeout: Int = DEFAULT_TIMEOUT_MS, block: (YeelightDeviceMap) -> Unit): Boolean {
        if (isScanning()) return false  // no need to rescan since scan is in progress
        stop()
        isScanning.set(true)
        currentJob = scope.launch(Dispatchers.Main) {
            doDiscoverDevices(timeout, block)
            doListen()
        }
        return true
    }

    override fun stop() {
        super.stop()
        currentJob?.cancel()
        currentJob = null
    }

    // internal
    private suspend fun doDiscoverDevices(timeout: Int, block: (YeelightDeviceMap) -> Unit) {
        val devices = withContext(Dispatchers.IO) {
            discoverLocalDevices(timeout)
        }
        yield()
        block(devices)
    }

    // internal
    private suspend fun doListen(block: (YeelightDevice) -> Unit) {
        withContext(Dispatchers.IO) {
            if (isActive)
                startListening(block)
        }
    }

    // internal
    private suspend fun doListen() {
        withContext(Dispatchers.IO) {
            if (isActive)
                startListening()
        }
    }

    // listener that is forced to dispatch into UI thread
    private fun <T> ((T) -> Unit).toUIListener() = { item: T ->
        scope.launch(Dispatchers.Main) {
            this@toUIListener(item)
        }
        Unit
    }
}