package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.YeelightDevice;

import org.jetbrains.annotations.NotNull;

/** Provides connections to devices. */
public interface YeelightConnectionProvider<T extends YeelightConnection> {
    /** Create (or return existing) connection to the device. */
    @NotNull
    T getConnection(@NotNull YeelightDevice device);

    /** Clean up any data and disconnect any active connections. */
    void release();
}