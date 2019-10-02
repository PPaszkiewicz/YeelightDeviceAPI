package com.github.ppaszkiewicz.yeelight.api.viewModel

import com.github.ppaszkiewicz.yeelight.api.YeelightCache
import com.github.ppaszkiewicz.yeelight.api.connection.YeelightBasicConnectionKt
import com.github.ppaszkiewicz.yeelight.core.YLog
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool
import kotlinx.coroutines.CoroutineScope

/**
 * [YeelightLiveData] that automatically manages connections based on observer state.
 * */
open class YeelightActiveLiveData(
        invalidateOnUpdates: Boolean,
        scope: CoroutineScope,
        deviceCache : YeelightCache? = null)
    : YeelightLiveData(
        YeelightBasicConnectionKt.PoolProvider(),
        invalidateOnUpdates,
        scope,
        deviceCache
) {
    companion object {
        const val TAG = "YeelightLiveLD"
    }

    private val connectionPool
        get() = connectionProvider as YeelightConnectionPool<out YeelightConnection>

    override fun onDiscovered(result: YeelightDeviceMap) {
        super.onDiscovered(result)
        result.values.forEach {
            connectIfPossible(it)
        }
    }

    override fun onAnnounced(device: YeelightDevice) {
        super.onAnnounced(device)
        connectIfPossible(device)
    }

    private fun connectIfPossible(device: YeelightDevice) {
        if (device.isConnected) {
            device.isOnline = true
        } else {
            device.isOnline = false
            if (hasActiveObservers())
                device.connect()
        }
    }

    override fun onActive() {
        super.onActive()
        if (!deviceScanner.wasDiscoveryRan()) {
            start()
        } else {
            if (!rescan()) {
                YLog.d(TAG, "no rescan needed discoverLocalDevices")
            } else
                YLog.d(TAG, "rescanning in discoverLocalDevices")
            // force start all obtained connections
            connectionPool.connections.values.forEach {
                it.connect()
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        // force stop all obtained connections
        connectionPool.connections.values.forEach {
            it.tryDisconnect()
        }
    }

    override fun onInstantiateConnection(connection: YeelightConnection) {
        super.onInstantiateConnection(connection)
        connection.addConnectionListenerInterceptor(connectionStateListener)
    }

    /** Used to put isOnline into devices. */
    private val connectionStateListener = object : YeelightConnection.ListenerInterceptor("_VM_ConnectionInterceptor") {
        override fun onYeelightDeviceConnected(deviceID: Long) {
            YLog.d(TAG, "device connected $deviceID")
            // it's possible value is not set yet if user connected to connection provider
            // without running the scan and is reusing cached devices
            value?.get(deviceID)?.let{
                it.isOnline = true
                if (invalidateOnUpdates) delayedInvalidate()
            }
        }

        //todo: this doesn't get dispatched in time?
        override fun onYeelightDeviceDisconnected(deviceID: Long, error: Throwable?) {
            YLog.d(TAG, "device disconnected $deviceID")
            // it's possible value is not set yet if user connected to connection provider
            // without running the scan and is reusing cached devices
            value?.get(deviceID)?.let{
                it.isOnline = false
                if (invalidateOnUpdates) delayedInvalidate()
            }
        }
    }
}