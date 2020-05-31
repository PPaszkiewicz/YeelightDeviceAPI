package com.github.ppaszkiewicz.yeelight.core.values;

import com.github.ppaszkiewicz.yeelight.core.YLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.bslamp;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.ceiling;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.color;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.mono;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.stripe;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel.Type.unspecified;

/**
 * Model of the device.
 */
public final class YeelightDeviceModel {
    /**
     * Type of this device. If it's not one of the presets, this will be <i>null</i>.
     */
    @Nullable
    public final Type type;
    /**
     * Raw name of this type of device.
     */
    @NotNull
    public final String name;
    /**
     * Readable display name.
     */
    @NotNull
    public final String displayName;


    public final static YeelightDeviceModel MONO = new YeelightDeviceModel(mono);
    public final static YeelightDeviceModel COLOR = new YeelightDeviceModel(color);
    public final static YeelightDeviceModel STRIPE = new YeelightDeviceModel(stripe);
    public final static YeelightDeviceModel CEILING = new YeelightDeviceModel(ceiling);
    public final static YeelightDeviceModel BSLAMP = new YeelightDeviceModel(bslamp);
    public final static YeelightDeviceModel UNSPECIFIED = new YeelightDeviceModel(unspecified);


    // generates one of pre-set models
    private YeelightDeviceModel(@NotNull Type type) {
        this.type = type;
        this.name = type.name();
        this.displayName = type.displayName;
    }

    // creates new model item
    private YeelightDeviceModel(@NotNull String name) {
        this.type = null;
        this.name = name;
        this.displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Generate model from string. Always uses preset objects for known types.
     */
    public static YeelightDeviceModel from(@Nullable String s) {
        // special case for null
        if (s == null) return new YeelightDeviceModel("null");
        try {
            Type t = Type.valueOf(s);
            return from(t);
        } catch (IllegalArgumentException | NullPointerException e) {
            YLog.d("YeelightDeviceModel", "device model not in enum list: " + s);
            return new YeelightDeviceModel(s);
        }
    }

    /**
     * Model from type.
     */
    @NotNull
    public static YeelightDeviceModel from(@NotNull Type type) {
        switch (type) {
            case mono:
                return MONO;
            case color:
                return COLOR;
            case stripe:
                return STRIPE;
            case ceiling:
                return CEILING;
            case bslamp:
                return BSLAMP;
            default:
                throw new IllegalArgumentException("invalid enum");
        }
    }

    /**
     * Raw enum type of known devices.
     */
    public enum Type {
        mono("White bulb"),
        color("RGB bulb"),
        stripe("LED stripe"),
        ceiling("Ceiling light"),
        bslamp("Bedside lamp"),
        unspecified("unspecified");

        /**
         * User friendly name, returned in {@link #toString()}.
         */
        @NotNull
        public final String displayName;

        Type(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        /**
         * Null safe wrapper for <code>valueOf(String)</code>
         */
        @Nullable
        public static Type from(String s) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }
        }
    }
}