package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YeelightDevice;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Provider that keeps references to created connections.
 */
public abstract class YeelightConnectionPool<T extends YeelightConnection> implements YeelightConnectionProvider<T> {
    /**
     * Pool of connections created by this provider.
     */
    private final HashMap<Long, T> mOngoingConnections = new HashMap<>();

    /**
     * Extension that can modify each newly instantiated connection.
     */
    private Extension extension;

    /**
     * Obtain connection from pool or create new one if needed.<br>
     * That connection will update provided device from now on.
     */
    @NotNull
    @Override
    public synchronized T getConnection(@NotNull YeelightDevice device) {
        T conn = mOngoingConnections.get(device.getId());
        if (conn == null) {
            conn = instantiateConnection(device);
            if (extension != null) extension.onInstantiateConnection(conn);
            mOngoingConnections.put(device.getId(), conn);
        } else {
            // replace target device of this connection
            conn.setDevice(device);
        }
        return conn;
    }

    /**
     * Create new connection instance when not found in pool.
     */
    protected abstract T instantiateConnection(@NotNull YeelightDevice device);

    /**
     * Force close and release all connections. Clears connection pool.
     */
    @Override
    public synchronized void release() {
        YeelightConnection[] conns = new YeelightConnection[mOngoingConnections.size()];
        mOngoingConnections.values().toArray(conns);
        for (YeelightConnection conn : conns) {
            conn.release();
            conn.tryDisconnect();
        }
    }

    /**
     * Get a soft-copy of current connections.
     */
    @NotNull
    public synchronized HashMap<Long, T> getConnections() {
        return new HashMap<>(mOngoingConnections);
    }

    /**
     * Set extension that can modify each newly instantiated connection.
     */
    public YeelightConnectionPool setExtension(Extension extension) {
        this.extension = extension;
        return this;
    }

    /**
     * Extension that can be added into {@link YeelightConnectionPool} to modify each newly instantiated connection.
     */
    public interface Extension {
        /**
         * Modify newly created connection.
         */
        void onInstantiateConnection(@NotNull YeelightConnection connection);
    }
}