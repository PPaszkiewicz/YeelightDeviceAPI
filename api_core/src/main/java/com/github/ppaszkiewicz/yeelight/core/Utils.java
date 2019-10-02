package com.github.ppaszkiewicz.yeelight.core;

/** Package private utils. */
abstract class Utils {
    private Utils(){}

    static long parseLong(String longStr){
        if(longStr.startsWith("0x")){
            return Long.parseLong(longStr.substring(2), 16);
        }
        return Long.parseLong(longStr);
    }

    /**
     * Convert on/off string to boolean. Unknown values return false as well.
     */
    static boolean isOn(String s){
        return "on".equals(s);
    }

    /** Parse int returning {@link YeelightDevice#UNDEFINED_VALUE} on exception. */
    static int parseInt(String s){
        return parseInt(s, YeelightDevice.UNDEFINED_VALUE);
    }

    /** Parse int returning default value on exception. */
    static int parseInt(String s, int defaultVal){
        try {
            return Integer.parseInt(s);
        }catch (NumberFormatException | NullPointerException npe){
            return defaultVal;
        }
    }

    /**
     * Convert true/false to on/off.
     */
    static String isOnFromBoolean(boolean on){
        return on ? "on" : "off";
    }

    /** Append [key = value \n] to string builder. */
    static void append(StringBuilder sb, Object key, Object value){
        sb.append(key).append(" = ").append(value).append('\n');
    }

    /** Drop alpha info from color int leaving 24 byte value. */
    static int toRGB(int color){
        return color & 0xFFFFFF;
    }

    /** Change colors alpha to 255. */
    static int toARGB(int color){
        return color | 0xFF000000;
    }
}
