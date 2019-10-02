package com.github.ppaszkiewicz.yeelight.api.connection

import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightBasicConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightSocket

/*
    More connection types to use.
 */


/** Basic connection that uses coroutines instead of raw Threads. Requires manual connect/disconnect. */
open class YeelightBasicConnectionKt : YeelightBasicConnection {
    constructor(device: YeelightDevice) : super(device)
    constructor(deviceId: Long, address: String, port: Int) : super(deviceId, address, port)

    /** Connection pool of [YeelightBasicConnectionKt]. */
    open class PoolProvider : YeelightConnectionPool<YeelightBasicConnectionKt>() {
        override fun instantiateConnection(device: YeelightDevice): YeelightBasicConnectionKt = YeelightBasicConnectionKt(device)
    }

    override fun createSocket(): YeelightSocket<YeelightBasicConnection> {
        return YeelightSocketCoroutineImpl(this)
    }
}