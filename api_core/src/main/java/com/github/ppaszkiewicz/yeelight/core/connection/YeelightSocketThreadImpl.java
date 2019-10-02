package com.github.ppaszkiewicz.yeelight.core.connection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** YeelightSocket that opens new threads for async listening. */
public class YeelightSocketThreadImpl<T extends YeelightConnection> extends YeelightSocket<T> {
    private static final String TAG = "YeelightSocketThread";
    private final static ThreadGroup YEELIGHT_THREAD_GROUP = new ThreadGroup("YeelightThreadGroup");

    /**
     * Runnable running the socket in async connection.
     */
    private final Runnable asyncRunnable = new AsyncRunnable();
    private Runnable sendRunnable;

    // thread holding the runnable
    private Thread socketThread;
    private Thread sendThread;

    public YeelightSocketThreadImpl(T connection) {
        super(connection);
    }

    @Override
    protected boolean isAsyncRunning() {
        return socketThread != null;
    }

    @Override
    protected void writeAsync(@NotNull YeelightCommand... msg) {
        sendRunnable = new AsyncSendRunnable(msg);
        sendThread =  new Thread(YEELIGHT_THREAD_GROUP, sendRunnable, "YeelightSendThread " + connection.deviceId);
        sendThread.start();
    }

    @Override
    protected void startListeningOnAsync() {
        socketThread = new Thread(YEELIGHT_THREAD_GROUP, asyncRunnable, "YeelightThread " + connection.deviceId);
        socketThread.start();
    }

    @Override
    protected void onLoopReadFinished(@Nullable Throwable t) {
        if (socketThread != null) {
            socketThread = null;
        }
        // this should conclude the thread
    }

    @Override
    protected boolean isInterrupted() {
        return false;   // does not handle this case
    }

    /**
     * Runnable that opens socket on async thread.
     */
    private final class AsyncRunnable implements Runnable {
        @Override
        public void run() {
            try {
                startBlockingConnection();
            } catch (Exception e) {
            //    e.printStackTrace();
            }
        }
    }

    /**
     * Runnable that sends data on async thread.
     */
    private final class AsyncSendRunnable implements Runnable {
        private final YeelightCommand[] commands;

        private AsyncSendRunnable(YeelightCommand[] commands) {
            this.commands = commands;
        }

        @Override
        public void run() {
            writeImpl(commands);
            if(sendThread == Thread.currentThread())
                sendThread = null;
        }
    }

}
