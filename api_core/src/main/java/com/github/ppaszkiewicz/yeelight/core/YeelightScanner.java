package com.github.ppaszkiewicz.yeelight.core;

import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discovers Yeelight devices on local network.
 */
public class YeelightScanner {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    private static final String TAG = "YeelightScanner";
    private static final String UDP_HOST = "239.255.255.250";
    private static final int UDP_PORT = 1982;
    private static final String DISCOVERY_MESSAGE =
            "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST:239.255.255.250:1982\r\n" +
                    "MAN:\"ssdp:discover\"\r\n" +
                    "ST:wifi_bulb\r\n";

    // internal flag
    private final AtomicLong lastDiscovery = new AtomicLong(0L);
    // for tracking if any socket is open
    private final AtomicBoolean isSocketOpen = new AtomicBoolean(false);
    protected final AtomicBoolean isScanning = new AtomicBoolean(false);

    /**
     * Socket used in {@link #discoverLocalDevices(int)}.
     */
    private DatagramSocket mDiscoverySocket;

    /**
     * Socket used in {@link #startListening()}.
     */
    private MulticastSocket mListenerSocket;

    /**
     * Connection provider injected into all scanned devices.
     */
    private YeelightConnectionProvider connectionProvider;

    /**
     * Listener for {@link #startListening()}.
     */
    protected OnDeviceAnnouncementListener onDeviceAnnouncementListener;

    /**
     * Scanner without connection factory.
     */
    public YeelightScanner() {
    }

    /**
     * Scanner using provided connection factory.
     */
    public YeelightScanner(@Nullable YeelightConnectionProvider factory) {
        connectionProvider = factory;
    }

    /**
     * True if successful discovery was performed at least once.
     */
    public boolean wasDiscoveryRan() {
        return lastDiscovery.get() != 0L;
    }

    /**
     * Timestamp of last discovery, value of currentTimeMillis.
     */
    public long getLastDiscoveryTimestamp() {
        return lastDiscovery.get();
    }

    /**
     * True if this is active.
     */
    public boolean isSocketOpen() {
        return isSocketOpen.get();
    }

    /**
     * True if this is currently scanning.
     * */
    public boolean isScanning(){ return isScanning.get(); }

    /**
     * Connection provider injected into all scanned devices.
     */
    @Nullable
    public YeelightConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    /**
     * Discover devices in local network. This function is blocking and performs network calls.
     */
    @NotNull
    public YeelightDeviceMap discoverLocalDevices() {
        return discoverLocalDevices(DEFAULT_TIMEOUT_MS);
    }

    /**
     * Set listener that will receive devices from {@link #startListening()}.
     */
    public void setOnDeviceAnnouncementListener(OnDeviceAnnouncementListener onDeviceAnnouncementListener) {
        this.onDeviceAnnouncementListener = onDeviceAnnouncementListener;
    }

    /**
     * Discover devices in local network. This function is blocking and performs network calls.
     */
    @NotNull
    public YeelightDeviceMap discoverLocalDevices(int timeOut) {
        isScanning.set(true);
        YLog.d(TAG, "discoverLocalDevices: started ");
        HashMap<Long, YeelightDevice> localDevices = new HashMap<>();
        Throwable error = null;
        SocketAddress addr = new InetSocketAddress(UDP_HOST, UDP_PORT);
        try {
            isSocketOpen.set(true);
            mDiscoverySocket = new DatagramSocket();
            DatagramPacket outPacket = new DatagramPacket(
                    DISCOVERY_MESSAGE.getBytes(),
                    DISCOVERY_MESSAGE.getBytes().length,
                    addr
            );
            long now = System.currentTimeMillis();
            YLog.d(TAG, "discoverLocalDevices: opened ");
            mDiscoverySocket.send(outPacket);

            YLog.d(TAG, "discoverLocalDevices: announcement sent ");
            //loop until timeout
            while (System.currentTimeMillis() < now + timeOut && isSocketOpen()) {
                byte[] buffer = new byte[1024];
                DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
                mDiscoverySocket.setSoTimeout(timeOut);
                //blocks until response
                mDiscoverySocket.receive(inPacket);

                HashMap<String, String> dataMap = internalParsePacket(inPacket);
                if (!dataMap.isEmpty()) {
                    long id = Utils.parseLong(dataMap.get("id"));
                    if(!localDevices.containsKey(id))
                        localDevices.put(id, YeelightDevice.fromDiscoveryMap(dataMap).setConnectionProvider(connectionProvider));
                }
                lastDiscovery.set(System.currentTimeMillis());
            }
        } catch (SocketTimeoutException e) {
            //   e.printStackTrace(); silent socket timeout exceptions
        } catch (Exception e) {
            e.printStackTrace();
            error = e;
        }
        mDiscoverySocket = null;
        isSocketOpen.set(false);
        isScanning.set(false);
        YLog.d(TAG, "discoverLocalDevices: finished ");
        return new YeelightDeviceMap(localDevices, error);
    }

    /**
     * Start listening for local advertisements. <br>
     * This allows discovery of newly connected devices in real time. <br>
     * This function is blocking and performs network calls.
     */
    public void startListening(@NotNull OnDeviceAnnouncementListener listener) {
        onDeviceAnnouncementListener = listener;
        startListening();
    }

    /**
     * Start listening for local advertisements. <br>
     * This allows discovery of newly connected devices in real time. <br>
     * This function is blocking and performs network calls.
     */
    public void startListening() {
        if (!isSocketOpen.compareAndSet(false, true)) {
            throw new IllegalStateException("Already open");
        }
        try {
            InetAddress group = InetAddress.getByName(UDP_HOST);
            mListenerSocket = new MulticastSocket(UDP_PORT);
            mListenerSocket.setLoopbackMode(true);
            mListenerSocket.joinGroup(group);
            YLog.d(TAG, "startListening: socket opened");
            while (isSocketOpen()) {
                YLog.d(TAG, "startListening: listening for message...");
                byte[] buffer = new byte[1024];
                DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
                mListenerSocket.receive(inPacket);

                HashMap<String, String> dataMap = internalParsePacket(inPacket);
                if (!dataMap.isEmpty() && onDeviceAnnouncementListener != null)
                    onDeviceAnnouncementListener.onDeviceDiscovered(YeelightDevice.fromDiscoveryMap(dataMap).setConnectionProvider(connectionProvider));
            }
        } catch (SocketException e) {
            // socket exception can trigger if user calls stop?
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mListenerSocket = null;
            isSocketOpen.set(false);
        }
    }

    // maps packet into actual data map
    @NotNull
    private HashMap<String, String> internalParsePacket(@NotNull DatagramPacket inPacket) {
        HashMap<String, String> dataMap = new HashMap<>();
        byte[] responseData = inPacket.getData();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inPacket.getLength(); i++) {
            // parse /r
            if (responseData[i] == '\r') {
                continue;
            }
            sb.append((char) responseData[i]);
        }
        String infoString = sb.toString();
        // safeguard to filter invalid messages
        if (!infoString.contains("yeelight")) {
            YLog.e(TAG, "Invalid message: " + infoString);
            return dataMap; // return empty map
        }
        // map response values
        YLog.d(TAG, infoString);
        String[] infoStrings = infoString.split("\n");

        for (String it : infoStrings) {
            int index = it.indexOf(":");
            if (index != -1) {
                dataMap.put(it.substring(0, index).trim(),
                        it.substring(index + 1).trim());
            }
        }
        return dataMap;
    }

    /**
     * Stop listening for local advertisements or  cancel ongoing discovery.
     */
    public void stop() {
        // this should be a safe call (no exceptions)
        DatagramSocket d = mDiscoverySocket;
        MulticastSocket m = mListenerSocket;
        if (d != null && d.isConnected()) {
            d.close();
        }
        if (m != null && m.isConnected()) {
            m.close();
        }
        isSocketOpen.set(false);
    }

    /**
     * Listener notified when device sent advertisement.
     */
    public interface OnDeviceAnnouncementListener {
        void onDeviceDiscovered(@NotNull YeelightDevice device);
    }
}
