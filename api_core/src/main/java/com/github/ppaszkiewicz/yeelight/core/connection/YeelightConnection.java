package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YLog;
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice;
import com.github.ppaszkiewicz.yeelight.core.utils.ReferenceHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Basic definition of connection with Yeelight device.<br>
 *
 * Connection itself will only keep a weak reference to a single device and automatically deploy
 * updates to it.
 */
public abstract class YeelightConnection {
    private final static String TAG = "YeeDeviceConn";

    public final long deviceId;
    @NotNull
    public final String address;
    public final int port;
    /**
     * Incrementable ID of next message.
     */
    private int messageId = 0;

    /**
     * Parses all listener callbacks for this connection.
     */
    @NotNull
    private final CallbackParser callbackParser = new CallbackParser();

    /**
     * Connection interceptors that will be called before actual listener.
     */
    @NotNull
    private final LinkedList<ListenerInterceptor> interceptors = new LinkedList<>();

    /**
     * Connection listener.
     */
    @Nullable
    private Listener connectionListener;

    /**
     * Device this connection is currently deploying updates to.
     */
    @NotNull
    protected WeakReference<YeelightDevice> device;

    /**
     * If this is raised connection should no longer work and any device trying to use it
     * should instead obtain new connection from its provider.
     */
    private final AtomicBoolean isReleased = new AtomicBoolean(false);

    public YeelightConnection(@NotNull YeelightDevice device) {
        deviceId = device.getId();
        address = device.getAddress();
        port = device.getPort();
        this.device = new WeakReference<>(device);
        initInterceptors();
    }

    public YeelightConnection(long deviceId, @NotNull String address, int port) {
        this.deviceId = deviceId;
        this.address = address;
        this.port = port;
        this.device = new WeakReference<>(null);
        initInterceptors();
    }

    /**
     * Initialize default interceptors.
     */
    protected void initInterceptors() {
        addConnectionListenerInterceptor(ListenerInterceptor.of(new UpdateDeviceListenerDelegate()));
    }

    /**
     * If this connection received {@link #connect()} request but haven't established connection yet.
     */
    public abstract boolean isConnecting();

    /**
     * If device is connected.
     */
    public abstract boolean isConnected();

    /**
     * If this connection received {@link #disconnect()}  request but haven't closed the connection yet.
     */
    public abstract boolean isClosing();

    /**
     * Send messages to the device.
     */
    public abstract void send(@NotNull YeelightCommand... commands);

    /**
     * Connect to the device asynchronously <br>
     * If connection already exists, this call will be ignored.
     *
     * @see #connectSync()
     */
    public abstract void connect();

    /**
     * Connects to the device on current (background) thread.<br>
     * If connection already exists, this call will be ignored.
     *
     * @see #connect()
     */
    public abstract void connectSync() throws Exception;

    /**
     * Disconnect from the device. Does nothing if not connected.
     */
    public abstract void disconnect() throws IOException;

    /**
     * Called after connection closes.
     */
    public void onDisconnected() {
        if (isReleased()) onRelease();
    }

    /**
     * {@link #disconnect()} wrapped in try-catch block.
     *
     * @return true if passed, false if exception was caught.
     */
    public final boolean tryDisconnect() {
        try {
            disconnect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YeelightConnection
                && deviceId == ((YeelightConnection) obj).deviceId;
    }

    /**
     * Add a listener that will intercept data received from the device before forwarding it to actual listener.<br>
     * Same interceptor can't be added twice.<br>
     *
     * @return true if interceptor was added, false if it was already in the stack
     */
    public boolean addConnectionListenerInterceptor(@NotNull ListenerInterceptor listenerInterceptor) {
        // interceptor already in stack
        if (hasConnectionListenerInterceptor(listenerInterceptor)) return false;
        interceptors.add(listenerInterceptor);
        return true;
    }

    /**
     * Check if listener interceptors contain specific object.
     */
    public boolean hasConnectionListenerInterceptor(@NotNull ListenerInterceptor listenerInterceptor) {
        for (ListenerInterceptor interceptor : interceptors) {
            if (listenerInterceptor.equals(interceptor)) return true;
        }
        return false;
    }

    /**
     * Check if listener interceptors contain specific tag.
     */
    public boolean hasConnectionListenerInterceptor(@NotNull String tag) {
        for (ListenerInterceptor interceptor : interceptors) {
            if (tag.equals(interceptor.tag)) return true;
        }
        return false;
    }

    /**
     * Set listener that will receive data received from the device.
     */
    public void setConnectionListener(@Nullable Listener listener) {
        this.connectionListener = listener;

    }

    /**
     * Get connection listener that was set with {@link #setConnectionListener(Listener)}<br><br>
     * <p>
     * This should not receive listener callbacks directly, instead use {@link #getCallbackParser()}.
     */
    @Nullable
    public Listener getConnectionListener() {
        return this.connectionListener;
    }

    /**
     * Callback parser that must receive all listener callbacks.
     */
    @NotNull
    public Listener getCallbackParser() {
        return callbackParser;
    }

    /**
     * Replace device this connection deploys updates to. This only keeps a weak connection to the
     * device and will only update it as long as it exists.
     */
    @NotNull
    public YeelightConnection setDevice(@Nullable YeelightDevice device) {
        if (device != null && device.getId() != deviceId) {
            throw new IllegalArgumentException("Cannot assign this device to this connection: " + device.getId() + " != " + deviceId);
        }
        this.device = new WeakReference<>(device);
        return this;
    }

    /**
     * Get device this connection auto updates.
     */
    @Nullable
    public YeelightDevice getDevice() {
        return device.get();
    }

    /**
     * Incremental message ID.
     */
    public final int nextMessageId() {
        return messageId++;
    }

    /**
     * {@link #isReleased}. Inheriting connections should respect this flag.
     */
    public boolean isReleased() {
        return isReleased.get();
    }

    /**
     * Release the connection. This will not close ongoing connection itself but will prevent
     * reopening this socket.
     */
    public final void release() {
        isReleased.set(true);
        if (!isConnected())
            onRelease();
    }

    /**
     * Called after released connection finishes closing the socket (or instantly if it had no
     * socket open).<br>
     */
    public void onRelease() {
    }

    /**
     * Interceptor that forwards callbacks to the device.
     */
    private final class UpdateDeviceListenerDelegate extends ListenerDelegate {
        @Nullable
        @Override
        public Listener getListener() {
            YeelightDevice d = device.get();
            if(d == null) YLog.e(TAG, "update device @"+deviceId+": device reference lost");
            return d;
        }
    }

    /**
     * Handles propagation of listener callbacks by sending them thru interceptors before deploying them to the listener.
     */
    private final class CallbackParser implements Listener {
        @Override
        public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
            for (ListenerInterceptor i : interceptors) {
                i.isInCallback = true;
                i.onYeelightDeviceResponse(deviceId, deviceReply);
                i.isInCallback = false;
                if (i.preventCallbacks) {
                    i.preventCallbacks = false;
                    break;
                }
            }
            Listener cl = connectionListener;
            if(cl != null)
                cl.onYeelightDeviceResponse(deviceId, deviceReply);
        }

        @Override
        public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, @Nullable YeelightCommand failedCommand) {
            for (ListenerInterceptor i : interceptors) {
                i.isInCallback = true;
                i.onYeelightDeviceConnectionError(deviceId, exception, failedCommand);
                i.isInCallback = false;
                if (i.preventCallbacks) {
                    i.preventCallbacks = false;
                    break;
                }
            }
            Listener cl = connectionListener;
            if(cl != null)
                cl.onYeelightDeviceConnectionError(deviceId, exception, failedCommand);
        }

        @Override
        public void onYeelightDeviceConnected(long deviceID) {
            for (ListenerInterceptor i : interceptors) {
                i.isInCallback = true;
                i.onYeelightDeviceConnected(deviceID);
                i.isInCallback = false;
                if (i.preventCallbacks) {
                    i.preventCallbacks = false;
                    break;
                }
            }
            Listener cl = connectionListener;
            if(cl != null)
                cl.onYeelightDeviceConnected(deviceID);
        }

        @Override
        public void onYeelightDeviceDisconnected(long deviceID, @Nullable Throwable error) {
            for (ListenerInterceptor i : interceptors) {
                i.isInCallback = true;
                i.onYeelightDeviceDisconnected(deviceID, error);
                i.isInCallback = false;
                if (i.preventCallbacks) {
                    i.preventCallbacks = false;
                    break;
                }
            }
            Listener cl = connectionListener;
            if(cl != null)
                cl.onYeelightDeviceDisconnected(deviceID, error);
        }
    }

    /**
     * Listener for updates from the device.
     */
    public interface Listener {
        /**
         * Device replied to command or posted property update.
         * <p><i>NOTE: This is called from async thread.</i></p>
         *  @param deviceId      ID of device that posted the update
         * @param deviceReply reply object*/
        void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply);

        /**
         * Exception occurred when establishing connection or sending commands.
         * <p><i>NOTE: This is called from async thread.</i></p>
         *
         * @param exception     exception that was thrown
         * @param failedCommand last message that was sent before connection had and error. If this is null then this was called
         *                      while establishing connection.
         */
        void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, @Nullable YeelightCommand failedCommand);

        /**
         * Connection to device established without exception.
         * <p><i>NOTE: This is called from async thread.</i></p>
         */
        void onYeelightDeviceConnected(long deviceID);

        /**
         * Disconnected from a device.
         * <p><i>NOTE: This is called from async thread.</i></p>
         *
         * @param error if non-null connection was interrupted exceptionally
         */
        void onYeelightDeviceDisconnected(long deviceID, @Nullable Throwable error);
    }

    /**
     * Empty {@link Listener} for selective overriding.
     */
    public static abstract class ListenerAdapter implements Listener {
        @Override
        public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
        }

        @Override
        public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, YeelightCommand failedCommand) {
        }

        @Override
        public void onYeelightDeviceConnected(long deviceId) {
        }

        @Override
        public void onYeelightDeviceDisconnected(long deviceId, @Nullable Throwable error) {
        }
    }

    /**
     * Delegates all methods of {@link Listener} down to provided listener, or does nothing
     * if listener is null or reference to it is lost.
     */
    public static abstract class ListenerDelegate implements Listener {
        private ReferenceHolder<Listener> listener;

        public ListenerDelegate() {
            listener = ReferenceHolder.ofNull();
        }

        public ListenerDelegate(Listener listener) {
            this.listener = ReferenceHolder.strong(listener);
        }

        public ListenerDelegate(WeakReference<Listener> weakListener) {
            this.listener = ReferenceHolder.of(weakListener);
        }

        /**
         * Listener this object delegates to.
         */
        @Nullable
        public Listener getListener() {
            return listener.get();
        }

        /**
         * Change listener this object delegates to.
         */
        @NotNull
        public ListenerDelegate setListener(Listener listener) {
            this.listener = ReferenceHolder.strong(listener);
            return this;
        }

        /**
         * Change listener this object delegates to using weak reference to it.
         */
        @NotNull
        public ListenerDelegate setWeakListener(Listener listener) {
            this.listener = ReferenceHolder.weak(listener);
            return this;
        }

        @Override
        public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
            Listener cl = getListener();
            if (cl != null)
                cl.onYeelightDeviceResponse(deviceId, deviceReply);
        }

        @Override
        public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, YeelightCommand failedCommand) {
            Listener cl = getListener();
            if (cl != null)
                cl.onYeelightDeviceConnectionError(deviceId, exception, failedCommand);
        }

        @Override
        public void onYeelightDeviceConnected(long deviceID) {
            Listener cl = getListener();
            if (cl != null)
                cl.onYeelightDeviceConnected(deviceID);
        }

        @Override
        public void onYeelightDeviceDisconnected(long deviceID, @Nullable Throwable error) {
            Listener cl = getListener();
            if (cl != null)
                cl.onYeelightDeviceDisconnected(deviceID, error);
        }
    }

    /**
     * Interceptor for {@link Listener} callbacks.<br>
     * Those will be invoked before listener, and they can consume interface callbacks by calling {@link #dispatchConsumeCallback()}.
     */
    public static abstract class ListenerInterceptor extends ListenerAdapter {
        /**
         * Raised if current method callback should be consumed.
         */
        private boolean preventCallbacks = false;
        private boolean isInCallback = false;

        /**
         * Wrap delegate object with an interceptor of specified tag.<br>
         * This will deploy all callbacks to listener and proceed to send them down the chain.
         */
        @NotNull
        public static ListenerInterceptor of(@Nullable String tag, @NotNull Listener listener) {
            return new Wrapper(tag, listener);
        }

        /**
         * Wrap listener object with an interceptor.<br>
         * This will deploy all callbacks to listener and proceed to send them down the chain.
         */
        @NotNull
        public static ListenerInterceptor of(@NotNull Listener listener) {
            return new Wrapper(null, listener);
        }

        /**
         * Tag of this interceptor. If set this should uniquely identify it in interceptor stack.
         * If not set then referential equality will be used.
         */
        @Nullable
        public final String tag;

        public ListenerInterceptor() {
            tag = null;
        }

        public ListenerInterceptor(@Nullable String tag) {
            this.tag = tag;
        }

        /**
         * Prevent propagation of current callback to next interceptors and listener.
         */
        public final void dispatchConsumeCallback() {
            if (!isInCallback)
                throw new IllegalStateException("Can only be called during callback methods");
            preventCallbacks = true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListenerInterceptor)) return false;
            if (tag == null || ((ListenerInterceptor) obj).tag == null) return this == obj;
            return tag.equals(((ListenerInterceptor) obj).tag);
        }

        @Override
        public String toString() {
            return this.getClass().getName() + " ["+tag+"] ";
        }

        /**
         * Interceptor wrapper that will deploy all callbacks to listener.
         */
        private static class Wrapper extends ListenerInterceptor {
            private final Listener wrappedListener;

            public Wrapper(@Nullable String tag, @NotNull Listener wrappedListener) {
                super(tag);
                this.wrappedListener = wrappedListener;
            }

            @Override
            public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
                wrappedListener.onYeelightDeviceResponse(deviceId, deviceReply);
            }

            @Override
            public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, YeelightCommand failedCommand) {
                wrappedListener.onYeelightDeviceConnectionError(deviceId, exception, failedCommand);
            }

            @Override
            public void onYeelightDeviceConnected(long deviceID) {
                wrappedListener.onYeelightDeviceConnected(deviceID);
            }

            @Override
            public void onYeelightDeviceDisconnected(long deviceID, @Nullable Throwable error) {
                wrappedListener.onYeelightDeviceDisconnected(deviceID, error);
            }
        }
    }
}