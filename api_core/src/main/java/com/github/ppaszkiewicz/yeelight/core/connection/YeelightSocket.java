package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps socket with in/output.<br>
 *
 * Abstract base implementing blocking calls and methods for building async callbacks.
 */
public abstract class YeelightSocket<T extends YeelightConnection> {
    private static final String TAG = "YeelightSocket";
    //array to reflect when using toArray on queue
    private final static YeelightCommand[] COMM_MESSAGE_ARRAY_TYPE = {};

    @NotNull
    protected final T connection;
    private boolean isAsync = false;
    private Socket socket;
    private BufferedOutputStream out;
    private BufferedReader reader;
    private YeelightReplyParser replyParser;
    @NotNull
    protected final AtomicBoolean isOpening = new AtomicBoolean(false);
    @NotNull
    protected final AtomicBoolean isClosing = new AtomicBoolean(false);
    /**
     * Commands to be sent when connection is established.
     */
    private final Queue<YeelightCommand> commWaiting = new LinkedList<>();
    /**
     * Recently sent commands, read to match a response.
     */
    private final Queue<YeelightCommand> commQueue = new LinkedList<>();

    /**
     * Constructor - this has to be bound to a single connection.
     */
    public YeelightSocket(@NotNull T connection) {
        this.connection = connection;
        replyParser = YeelightReplyParser.obtain(connection.deviceId);
    }

    /** True if there was an open request but connection is not established yet. */
    public boolean isConnecting() {
        return isOpening.get();
    }

    /**
     * Whether this socket is connected to device.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /** True if close was requested but socket is not closed yet. */
    public boolean isClosing() {return isClosing.get(); }

    /**
     * Must return true if async is running.
     */
    protected abstract boolean isAsyncRunning();

    /**
     * Send data thru socket asynchronously. Implementation must call {@link #writeImpl(YeelightCommand...)}
     * and pass down the parameters. This should only be called from within {@link #write(YeelightCommand...)}.
     */
    protected abstract void writeAsync(@NotNull YeelightCommand... msg);

    /**
     * Create and start listening for data from device asynchronously. Implementation must call
     * {@link #startBlockingConnection()}.
     */
    protected abstract void startListeningOnAsync();

    /**
     * Called when loop reading is finished. Clean up async data if needed. This is called on loop thread.
     *
     * @param throwable if not null then socket was closed exceptionally
     */
    protected abstract void onLoopReadFinished(@Nullable Throwable throwable);

    /**
     * Checked during {@link #loopRead()}. If this returns true, loop read becomes
     * immediately cancelled.
     */
    protected abstract boolean isInterrupted();

    /**
     * Change reply parser implementation.
     *
     * @param replyParser non-null reply parser implementation.
     */
    public YeelightSocket setReplyParser(@NotNull YeelightReplyParser replyParser) {
        this.replyParser = replyParser;
        return this;
    }

    /**
     * Send commands asynchronously and flush afterwards. If any command fails to send, others won't be sent.
     */
    public synchronized void write(@NotNull YeelightCommand... msg) {
        if(isAsyncRunning()){
            if(isConnected()){
                writeAsync(msg);
            }else{
                //async was requested but not connected yet, store command for when it connects
                commWaiting.addAll(Arrays.asList(msg));
            }
        }else
            YLog.e(TAG, "write@"+connection.deviceId+": cannot write because device is not connected.");
    }

    /**
     * This must be called from {@link #writeAsync(YeelightCommand...)}.
     */
    protected void writeImpl(@NotNull YeelightCommand... msg) {
        YeelightCommand lastSentCommand = null;    // stored for error throw
        try {
            for (YeelightCommand comm : msg) {
                String json = comm.toJSON() + "\r\n";
                YLog.i(TAG, "write@" + connection.deviceId + ": " + json);
                commQueue.add(comm);
                lastSentCommand = comm;
                out.write(json.getBytes());
                out.flush();
            }
        } catch (NullPointerException | IOException ioE) {
            // null pointer can trigger when "out" is null (disconnected)
            YLog.e(TAG, "write@"+ connection.deviceId +": "+ ioE.getMessage());
            //callback error
            connection.getCallbackParser().onYeelightDeviceConnectionError(connection.deviceId, ioE, lastSentCommand);
        }
    }

    /**
     * Internal method that sends all messages that were passed to {@link #write(YeelightCommand...)}
     * before async thread started. Socket must be validated if it's open before being called.
     **/
    private synchronized void sendWaiting() {
        writeImpl(commWaiting.toArray(COMM_MESSAGE_ARRAY_TYPE));
        commWaiting.clear();
    }

    /**
     * Called in edge case where connection was closed before it had time to open.
     */
    private synchronized void clearWaiting() {
        if (commWaiting.size() > 0) {
            YLog.e(TAG, commWaiting.size() + " commands lost for " + connection.deviceId);
            commWaiting.clear();
        }
    }

    /**
     * Open socket asynchronously.
     */
    public synchronized void openAsync() {
        if (!isOpening.compareAndSet(false, true)) {
            YLog.i(TAG, "openAsync: already in process of opening... " + connection.deviceId);
            return;
        }
        YLog.i(TAG, "openAsync: opening OK... " + connection.deviceId);
        isAsync = true;
        isClosing.set(false);
        startListeningOnAsync();
    }


    /**
     * Opens the socket and starts listening on current thread - should not be called from main thread.
     */
    public synchronized void open() throws Exception {
        if (!isOpening.compareAndSet(false, true)) {
            YLog.i(TAG, "open: already in process of opening... " + connection.deviceId);
            return;
        }
        YLog.i(TAG, "open: opening OK... " + connection.deviceId);
        isAsync = false;
        isClosing.set(false);
        startBlockingConnection();
    }

    /**
     * Prepares socket, input/otput streams and begins {@link #loopRead()}.
     */
    protected void startBlockingConnection() throws Exception {
        // conditions that should prevent socket from opening
        if (!isOpening.get()) {
            YLog.e(TAG, "Closed before async started " + connection.deviceId);
            clearWaiting();
            return;
        }
        if (connection.isReleased()) {
            YLog.e(TAG, "Released before async started " + connection.deviceId);
            clearWaiting();
            return;
        }
        try {
            socket = new Socket(connection.address, connection.port);
            socket.setKeepAlive(true);
            out = new BufferedOutputStream(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            // catch all exceptions (IO / security etc)
            connection.getCallbackParser().onYeelightDeviceConnectionError(connection.deviceId, e, null);
            throw e;
        }finally{
            if (!isOpening.compareAndSet(true, false)) {
                YLog.e(TAG, "isOpening lowered while creating socket?? " + connection.deviceId);
            }
            isClosing.set(false);
        }
        connection.getCallbackParser().onYeelightDeviceConnected(connection.deviceId);
        sendWaiting();
        loopRead();
    }

    /**
     * Infinite loop for reading data from device.
     */
    private void loopRead() {
        YLog.d(TAG, "loopRead: connection established " + connection.deviceId);
        //infinite loop, must kill socket to stop
        String line = null;
        Throwable throwable = null;
        try {
            while (isConnected() && !isInterrupted()) {
                line = reader.readLine();
                YLog.i(TAG, "receive@" + connection.deviceId + ":" + line);
                YeelightReply yeelightReply = replyParser.parse(line, commQueue);
                if (yeelightReply == null) {
                    YLog.e(TAG, "loopRead@" + connection.deviceId + ": failed to parse the reply: " + line);
                } else {
                    connection.getCallbackParser().onYeelightDeviceResponse(connection.deviceId, yeelightReply);
                    YeelightCommand c = yeelightReply.getCommand();
                    if(c != null && c.listener != null){
                        c.listener.onReply(yeelightReply);
                    }
                }
            }
        } catch (SocketException sEx) {
            // socket exception is expected if socket closing was requested
            if(!isClosing.get()) {
                throwable = sEx;
            }
        } catch (Exception e) {
            YLog.d(TAG, "loopRead@" + connection.deviceId + ": exception while reading: " + e.getMessage());
            throwable = e;
            try {
                socket.close();
            } catch (Exception ex) {
                // ignore any exception here
            }
        }
        // clear any commands that failed to receive a reply
        if (commQueue.size() > 0) {
            YLog.e(TAG, "loopRead@" + connection.deviceId + ": commands lost due to socket closing: " + commQueue.size());
            commQueue.clear();
        }
        connection.getCallbackParser().onYeelightDeviceDisconnected(connection.deviceId, throwable);
        //finished, clear thread.
        socket = null;
        onLoopReadFinished(throwable);
        YLog.i(TAG, "loopRead@ " + connection.deviceId + " finished");
        isClosing.set(false);
        connection.onDisconnected();
    }

    /**
     * Close the socket, finishing async thread.
     */
    public synchronized void close() throws IOException {
        if (socket != null && isAsyncRunning()) {
            YLog.d(TAG, "close: " + connection.deviceId + " isActive: " + isConnected());
            isClosing.set(true);
            socket.close();
        }
        // cancel opening the socket if it's in progress
        isOpening.set(false);
    }
}
