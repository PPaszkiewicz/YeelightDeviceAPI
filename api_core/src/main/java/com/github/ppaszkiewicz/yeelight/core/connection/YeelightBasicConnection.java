package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YLog;
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Basic connection managed by the user implemented using Threads.<br>
 * It must be manually connected and disconnected.
 */
public class YeelightBasicConnection extends YeelightConnection {
    private final static String TAG = "YeeDeviceBasicConn";
    private final YeelightSocket socket;

    public static class PoolProvider extends YeelightConnectionPool<YeelightBasicConnection> {
        @Override
        protected YeelightBasicConnection instantiateConnection(@NotNull YeelightDevice device) {
            return new YeelightBasicConnection(device);
        }
    }

    public YeelightBasicConnection(@NotNull YeelightDevice device) {
        super(device);
        socket = createSocket();
        connect();
    }

    public YeelightBasicConnection(long deviceId, @NotNull String address, int port) {
        super(deviceId, address, port);
        socket = createSocket();
    }

    /**
     * Create socket implementation for this connection. By default Thread implementation is used.<br>
     * Note that this is called during constructor so not all fields might be initialized yet.
     */
    @NotNull
    protected YeelightSocket<YeelightBasicConnection> createSocket() {
        return new YeelightSocketThreadImpl<>(this);
    }

    /**
     * Send commands.
     */
    @Override
    public void send(@NotNull YeelightCommand... commands) {
        if (isReleased())
            throw new IllegalStateException("This socket was released already " + deviceId);
        socket.write(commands);
    }

    @Override
    public void connect() {
        if (isReleased())
            throw new IllegalStateException("This socket was released already " + deviceId);
        if (isConnected()) {
            YLog.i(TAG, "connect: already connected " + deviceId);
        } else {
            socket.openAsync();
        }
    }

    @Override
    public void connectSync() throws Exception {
        if (isReleased())
            throw new IllegalStateException("This socket was released already " + deviceId);
        if (isConnected()) {
            YLog.i(TAG, "connect: already connected" + deviceId);
        } else {
            socket.open();
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (socket.isConnected()) {
            socket.close();
        } else
            YLog.i(TAG, "disconnect(): Socket already disconnected " + deviceId);
    }

    @Override
    public boolean isConnecting() {
        return socket.isConnecting();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isClosing() {
        return socket.isClosing();
    }
}