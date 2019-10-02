package com.github.ppaszkiewicz.yeelight.core.values;

import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Available methods. Enum names are mapped 1:1 with device commands.
 */
public enum YeelightMethod {
    none(0), // placeholder value when no method is used
    get_prop(1),
    set_ct_abx(1 << 2),
    set_rgb(1 << 3),
    set_hsv(1 << 4),
    set_bright(1 << 5),
    set_power(1 << 6),
    toggle(1 << 7),
    set_default(1 << 8),
    start_cf(1 << 9),
    stop_cf(1 << 10),
    set_scene(1 << 11),
    cron_add(1 << 12),
    cron_get(1 << 13),
    cron_del(1 << 14),
    set_adjust(1 << 15),
    set_music(1 << 16),
    set_name(1 << 17),
    bg_set_rgb(1 << 18),
    bg_set_hsv(1 << 19),
    bg_set_ct_abx(1 << 20),
    bg_start_cf(1 << 21),
    bg_stop_cf(1 << 22),
    bg_set_scene(1 << 23),
    bg_set_default(1 << 24),
    bg_set_power(1 << 25),
    bg_set_bright(1 << 26),
    bg_set_adjust(1 << 27),
    bg_toggle(1 << 28),
    dev_toggle(1 << 29),
    adjust_bright(1 << 30),
    adjust_ct(1L << 31),
    adjust_color(1L << 32),
    bg_adjust_bright(1L << 33),
    bg_adjust_ct(1L << 34),
    bg_adjust_color(1L << 35);

    /** Flag representing availability of this command.*/
    public final long flag;

    YeelightMethod(long flag){
        this.flag = flag;
    }

    /** Static values array without {@link #none} enum. */
    @NotNull
    public final static YeelightMethod[] values = initMethodsArray();
    private static YeelightMethod[] initMethodsArray(){
        YeelightMethod[] values = values();
        return Arrays.copyOfRange(values, 1, values.length);
    }

    // lazy init map
    private static HashMap<String, Long> nameToFlagMap;
    private static synchronized void buildNameToFlagMap(){
        if(nameToFlagMap != null) return;
        HashMap<String ,Long> map = new HashMap<>();
        for (YeelightMethod method : values) {
            map.put(method.name(), method.flag);
        }
        nameToFlagMap = map;
    }

    /** Parse string of commands returned from device into single flag based long. */
    public static long parseToLong(@Nullable String commands){
        if(commands == null || commands.isEmpty()) return none.flag;
        if(nameToFlagMap == null) buildNameToFlagMap();
        long availableCommands = none.flag;
        Long methodFlag;
        for (String s : commands.split(" ")) {
            methodFlag = nameToFlagMap.get(s);
            if(methodFlag != null)
                availableCommands |= methodFlag;
        }
        return availableCommands;
    }

    /** Parse long containing method flags into readable string. */
    @NotNull
    public static String parseToString(long methodFlags){
        if(methodFlags == 0) return none.name();
        StringBuilder sb = new StringBuilder();
        for (YeelightMethod method : values) {
            if((method.flag & methodFlags) == method.flag)
                sb.append(method.name()).append(" ");
        }
        sb.deleteCharAt(sb.length()-1); // delete trailing space
        return sb.toString();
    }

    /** Available optional "mode" values for {@link #set_power}.
     * Note: order is important (using ordinal). */
    public enum PowerMode implements YeelightCommand.CustomParam {
        /** Normal turn on operation (default value) */
        normal,
        ct_mode,
        rgb_mode,
        hsv_mode,
        /** Color flow mode. */
        cf_mode,
        nl_mode;

        @Override
        public void addToJSONArray(@NotNull JSONArray jsonArray) {
            jsonArray.put(ordinal());
        }
    }

    /** Available Scene values. */
    public enum Scene{
        /** rgb + brightness*/
        color,
        /** hue + sat + brightness*/
        hsv,
        /** color temp + brightness*/
        ct,
        /** see color flow */
        cf,
        /** brightness + delay [min] to turn off*/
        auto_delay_off
    }

    /** Available adjust values. */
    public static abstract class Adjust{
        /** Adjust actions*/
        public enum Action{
            increase,
            decrease,
            circle
        }

        /** Adjust props */
        public enum Prop{
            bright,
            ct,
            color
        }
    }

    /** Available action for {@link #set_music} command.
     * Note: order is important (using ordinal). */
    public enum MusicMode implements YeelightCommand.CustomParam{
        turn_off,
        turn_on;

        @Override
        public void addToJSONArray(@NotNull JSONArray jsonArray) {
            jsonArray.put(ordinal());
        }
    }
}
