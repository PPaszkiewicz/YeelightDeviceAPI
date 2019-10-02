package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YLog;
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Connection that automatically opens temporary socket and disconnects after few seconds so state
 * updates can be properly received.<br><br>
 *
 * This is an abstract base, inheriting classes should use desired delay/schedule implementations.
 */
public abstract class YeelightAutoConnection extends YeelightConnection {
    private final static String TAG = "YeeDeviceAutoConn";
    private final TimeoutRunnable mTimeoutRunnable = new TimeoutRunnable();
    private final YeelightSocket<?> socket;

    /**
     * Stateless connection.
     * @param device device this connection is being created for
     * */
    protected YeelightAutoConnection(YeelightDevice device) {
        super(device);
        socket = createSocket();
    }

    @Override
    protected void initInterceptors() {
        super.initInterceptors();
        addConnectionListenerInterceptor(ListenerInterceptor.of(new DisconnectListenerDelegate()));
    }

    /** Create socket implementation for this connection. By default Thread implementation is used.<br>
     * Note that this is called during constructor so not all fields might be initialized yet. */
    @NotNull
    protected YeelightSocket<? extends YeelightAutoConnection> createSocket(){
        return new YeelightAutoSocket(this);
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

    @Override
    public void send(@NotNull YeelightCommand... commands) {
        if(isReleased())
            throw new IllegalStateException("This socket was released already");
        if(!isConnected()){
            socket.openAsync();
        }
        socket.write(commands);
        // handler that kills the connection after small delay
        startTimeout(mTimeoutRunnable);
    }

    @Override
    public void connect() {
        //it should not be possible to call this method
        YLog.e(TAG, "invalid call - default connection performs automatic connect.");
    }

    @Override
    public void connectSync() {
        //it should not be possible to call this method
        YLog.e(TAG, "invalid call - default connection performs automatic connect.");
    }

    @Override
    public void disconnect() throws IOException {
        if(socket.isConnected()) {
            socket.close();
        }
    }

    private class TimeoutRunnable implements Runnable{
        @Override
        public void run() {
            cancelTimeout(this);
            try {
                disconnect();
            }catch (IOException ioE){
                //don't crash here
                ioE.printStackTrace();
            }
            onConnectionTimedOut();
        }
    }

    /** Run given runnable after timeout. This should also cancel previous timeout. */
    public abstract void startTimeout(Runnable runnable);
    /** Cancel timeout of runnable. */
    public abstract void cancelTimeout(Runnable runnable);

    /** Called when connection is timed out and auto-disconnects. */
    public void onConnectionTimedOut(){}

    /** Disconnects if device reference is lost. */
    private class DisconnectListenerDelegate extends ListenerAdapter {
        @Override
        public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
            if(device.get() == null){
                //device lost
                tryDisconnect();
            }
        }

        @Override
        public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, YeelightCommand failedCommand) {
            if(device.get() == null) {
                //device lost
                tryDisconnect();
            }
        }
    }

    /** Thread socket that stops if weak reference to device is lost. */
    public static class YeelightAutoSocket extends YeelightSocketThreadImpl<YeelightAutoConnection>{
        public YeelightAutoSocket(YeelightAutoConnection connection) {
            super(connection);
        }

        @Override
        protected boolean isInterrupted() {
            // stop listening if device reference is lost
            return connection.device.get() == null;
        }
    }
}
