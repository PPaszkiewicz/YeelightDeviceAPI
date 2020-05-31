package com.github.ppaszkiewicz.yeelight.core.values;

import com.github.ppaszkiewicz.yeelight.core.YLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Available device props - enum names match values from documentation.
 */
public enum YeelightProp {
    /** on: smart LED is turned on / off: smart LED is turned off */
    power(2),
    /** Brightness percentage. Range 1 ~ 100 */
    bright,
    /** Color temperature. Range 1700 ~ 6500(k) */
    ct,
    /** Color. Range 1 ~ 16777215 */
    rgb,
    /** Hue. Range 0 ~ 359 */
    hue,
    /** Saturation. Range 0 ~ 100 */
    sat,
    /** @see ColorMode */
    color_mode(3),
    /** 0: no flow is running / 1:color flow is running */
    flowing,
    /** The remaining time of a sleep timer. Range 1 ~ 60 (minutes) */
    delayoff,
    /** Current flow parameters (only meaningful when 'flowing' is 1) */
    flow_params(4),     // 10
    /** 1: Music mode is on / 0: Music mode is off */
    music_on,
    /** The name of the device set by “set_name” command */
    name(1),
    /** Background light power status */
    bg_power(2),
    /** Background light is flowing */
    bg_flowing,
    /** Current flow parameters of background light */
    bg_flow_params(4),
    /** Color temperature of background light */
    bg_ct,
    /** @see ColorMode */
    bg_lmode(3),
    /** Brightness percentage of background light (0 - 100) */
    bg_bright,
    /** Color of background light (1 ~ 16777215) */
    bg_rgb,
    /** Hue of background light (0 - 369) */
    bg_hue,                 // 20
    /** Saturation of background light (0 - 100) */
    bg_sat,
    /** Brightness of night mode light (0 - 100) */
    nl_br,
    /** 0: daylight mode / 1: moonlight mode (ceiling light only) */
    active_mode;            // 23

    public static final int TYPE_INT = 0;
    public static final int TYPE_STRING = 1;
    public static final int TYPE_ON_OFF = 2;
    public static final int TYPE_COLOR_MODE = 3;
    public static final int TYPE_ARRAY = 4;

    /** Type of this object */
    public final int type;

    // prop with custom type
    YeelightProp(int type){
        this.type = type;
    }

    // by default type is an INT
    YeelightProp() {
        this(TYPE_INT);
    }

    /**
     * Null safe valueOf(String). Returns null if name doesn't match any enum.
     */
    @Nullable
    public static YeelightProp valueOfOrNull(@Nullable String s){
        try{
            return valueOf(s);
        }catch (IllegalArgumentException e){
          //  YLog.e("YeelightProp","unknown device prop: "+s);
        }
        return null;
    }

    /** All props that can be updated. */
    // invalid command error if all 23 props are provided
    public static YeelightProp[] allValues = Arrays.copyOfRange(values(), 0, 22);

    /** Color modes of {@link #color_mode} and {@link #bg_lmode}. Their ordinal values match specs modes. */
    public enum ColorMode{
        mode_unknown("Unknown"), // dummy enum added for padding
        /** 1 - rgb mode */
        mode_rgb("RGB"),
        /** 2 - color temperature mode*/
        mode_color_temp("Temp"),
        /** 3 - hsv mode */
        mode_hsv("HSV");

        @NotNull
        private final String display_name;

        ColorMode(@NotNull String display_name) {
            this.display_name = display_name;
        }

        /**
         * Null safe ordinal dereference. Returns {@link #mode_unknown} on errors.
         */
        @NotNull
        public static YeelightProp.ColorMode from(int id){
            try{
                return values[id];
            }catch (ArrayIndexOutOfBoundsException e){
                YLog.e("YeelightProp","unknown color mode: "+id);
            }
            return mode_unknown;
        }

        @NotNull
        public static YeelightProp.ColorMode[] values = values();

        @Override
        public String toString() {
            return display_name;
        }
    }
}