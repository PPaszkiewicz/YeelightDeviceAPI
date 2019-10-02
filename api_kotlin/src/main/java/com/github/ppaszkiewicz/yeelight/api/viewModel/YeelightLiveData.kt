package com.github.ppaszkiewicz.yeelight.api.viewModel

import androidx.lifecycle.LiveData
import com.github.ppaszkiewicz.yeelight.api.YeelightCache
import com.github.ppaszkiewicz.yeelight.api.YeelightScannerAsync
import com.github.ppaszkiewicz.yeelight.core.YLog
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import kotlinx.coroutines.*

/**
 * LiveData for [YeelightDeviceMap].
 *
 * - Upon starting, runs a scan to discover all local devices.
 * - After discovery dispatch those devices to observers.
 * - Begin listening for announcement messages from devices.
 * - When [onCleared] is called stop listening for announcements.
 *
 * Whenever rescan is received, this LiveData obtains new map and sends it to observers.<br>
 * In case of device announcement, it updates existing results and notifies observers if needed. <br>
 *
 * New devices (after rescan) will always "take over" existing device connection and will be the one that has its
 * prop values automatically updated from that point.
 * */
open class YeelightLiveData(
        /** Pool of connections, used whenever they're discovered. */
        val connectionProvider: YeelightConnectionPool<out YeelightConnection>?,
        /** Whether this livedata will send new data upon updates. This can only be true if [connectionProvider] is not null. */
        val invalidateOnUpdates: Boolean = false,
        /** Scope to use when running coroutines. */
        protected val scope: CoroutineScope = GlobalScope,
        /** Cache to use to store/restore devices. This can be used to setup value before scan finishes. */
        val deviceCache: YeelightCache? = null
) : LiveData<YeelightDeviceMap>(), YeelightConnectionPool.Extension {
    companion object {
        const val TAG = "YeelightLiveData"
        /** How long updates can be delayed (to batch multiple replies together). */
        const val UPDATE_PERIOD_MS = 250L
    }

    // scanner used to find the devices
    protected val deviceScanner = YeelightScannerAsync(scope, connectionProvider)
    // job of delayed invalidate
    private var delayedInvalidateJob: Job? = null

    init {
        require(!(connectionProvider == null && invalidateOnUpdates)) { "invalidateOnUpdates cannot be true if connectionProvider is null." }
        connectionProvider?.setExtension(this)
    }

    protected val wasStarted
        get() = deviceScanner.wasDiscoveryRan()

    /** Begin this livedata. This should be called once, for example after viewmodel finishes init.
     * @param restoreCache true to query [deviceCache] and immediately emit values from it before scan finishes. */
    fun start(restoreCache: Boolean = true) {
        check(!deviceScanner.wasDiscoveryRan()) { "start() can only be called once" }
        // obtain devices from cache before scan finishes
        if (restoreCache) restoreFromCache()
        deviceScanner.discoverAndStartListeningAsync(
                scanResult = ::onDiscoveredAndUpdate,
                announcementListener = ::onAnnounced
        )
    }

    /** Obtain devices from the cache and set them us as value. This is called from [start], use this
     * instead to prevent initial device scan and only emit cached devices. */
    fun restoreFromCache(): Boolean {
        // obtain devices from cache before scan finishes
        return deviceCache?.getCachedDevices()?.let { devices ->
            // inject connection provider into obtained devices
            devices.values.forEach { d ->
                d.connectionProvider = connectionProvider
            }
            value = devices
            true
        } ?: false
    }

    /**
     * Force rescan of available devices and start listening afterwards.
     * Returns false is scan is currently active.
     *
     * If [start] was not called before it calls it instead.
     * */
    fun rescan(): Boolean {
        return if (!wasStarted) {
            start(false)
            true
        } else
            deviceScanner.rescan(block = ::onDiscoveredAndUpdate)
    }

    /** Runs device scan without starting announcement listener. */
    fun scanOnly() {
        deviceScanner.discoverDevices(block = ::onDiscoveredAndUpdate)
    }

    /** Call overridable callback and post livedata value update. */
    private fun onDiscoveredAndUpdate(result: YeelightDeviceMap) {
        // ignore errors in livedata?
        if (result.error != null) {

        }
        onDiscovered(result)
        // replace old values
        value = result
    }

    /** Callback function for when devices are discovered. This is called right before update is deployed
     * to observers. */
    protected open fun onDiscovered(result: YeelightDeviceMap) {
        result.values.forEach { device ->
            // take over connection updates if possible
            value?.get(device.id)?.existingConnection?.device = device
            device.setDiscovered(true)
        }
    }

    /** Callback function for when device announces itself. */
    protected open fun onAnnounced(device: YeelightDevice) {
        val oldDevice = value!![device.id]
        if (oldDevice != null) {
            // this device is already in the list
            var needUpdate = !oldDevice.isDiscovered
            oldDevice.isDiscovered = true
            if (oldDevice.copyProps(device)) {
                // there was an update
                needUpdate = true
            }
            if(needUpdate)
                value = value
        } else {
            // new device
            value!![device.id] = device.setDiscovered(true)
            // dispatch update to observers immediately
            value = value
        }
    }

    /** Should be called when ViewModel is destroyed. */
    open fun onCleared() {
        deviceScanner.stop()
        connectionProvider?.release()
        // store devices for later
        value?.let {
            deviceCache?.cacheDevices(it)
        }
    }

    override fun onInstantiateConnection(connection: YeelightConnection) {
        if (invalidateOnUpdates)
            connection.addConnectionListenerInterceptor(deviceListener)
    }

    /** Listener used if [invalidateOnUpdates]. */
    private val deviceListener = object : YeelightConnection.ListenerInterceptor("_VM_InvalidateInterceptor") {
        override fun onYeelightDeviceResponse(deviceId: Long, deviceRepy: YeelightReply) {
            if (deviceScanner.isScanning) return // do not update anything while scanning

            YLog.d(TAG, "Update: $deviceRepy")
            if (deviceRepy.hasProps()) {
                // connection itself will update props of the device, just notify about change (no diffs)
                delayedInvalidate()
            }
        }
    }

    /**
     * Invalidate the values after up to [UPDATE_PERIOD_MS].
     *
     * Effectively batches multiple updates into single observer callback.
     * */
    protected fun delayedInvalidate() {
        if (!wasStarted || delayedInvalidateJob?.isActive == true) return
        delayedInvalidateJob = scope.launch {
            delay(UPDATE_PERIOD_MS)
            value = value
        }
    }
}