package com.github.ppaszkiewicz.yeelight.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link HashMap} of {@link YeelightDevice}, but it can optionally hold loading error.
 */
public class YeelightDeviceMap extends HashMap<Long, YeelightDevice> {
    @Nullable
    private Throwable error;

    /**
     * Create map by copying from source map and setting up optional error.
     */
    public YeelightDeviceMap(@NotNull Map<Long, ? extends YeelightDevice> map, @Nullable Throwable error) {
        super(map);
        this.error = error;
    }

    /**
     * Create map by copying from source map and without error.
     */
    public YeelightDeviceMap(@NotNull Map<Long, ? extends YeelightDevice> map) {
        super(map);
        error = null;
    }

    /**
     * Empty map with error.
     */
    public YeelightDeviceMap(@Nullable Throwable error) {
        super();
        this.error = error;
    }

    /**
     * Empty map without error.
     */
    public YeelightDeviceMap() {
        super();
        error = null;
    }

    /** Get the error associated with loading. */
    @Nullable
    public synchronized Throwable getError() {
        return error;
    }

    /** Replace / clear error.*/
    @NotNull
    public synchronized YeelightDeviceMap setError(@Nullable Throwable error) {
        this.error = error;
        return this;
    }
}