package com.github.ppaszkiewicz.yeelight.api

import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap

/**
 * Cache that can be used to save/restore devices without performing any scan.
 */
interface YeelightCache {
    /** Restore cached device map. This should return null in case of any errors. */
    fun getCachedDevices(): YeelightDeviceMap?

    /** Store current devices. */
    fun cacheDevices(map: YeelightDeviceMap?)
}