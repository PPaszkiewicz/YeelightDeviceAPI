package com.github.ppaszkiewicz.yeelight.core;

import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * HashMap with typed gets and sets for properties of device.
 */
public final class PropHashMap extends HashMap<YeelightProp, Object> {
    /**
     * Empty map
     */
    public PropHashMap() {
    }

    /**
     * From key - value map (discover/announcement)
     */
    public PropHashMap(@NotNull Map<String, String> map) {
        for (String s : map.keySet()) {
            put(s, map.get(s));
        }
    }

    /**
     * Deserialize from json object.
     */
    public PropHashMap(@NotNull JSONObject json) {
        try {
            Iterator<String> it = json.keys();
            while (it.hasNext()) {
                String key = it.next();
                put(key, json.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Put by parsing key and value from strings. If key is not a valid prop, no put happens and null is returned.
     */
    @Nullable
    public Object put(@NotNull String key, @Nullable String value) {
        YeelightProp p = YeelightProp.valueOfOrNull(key);
        if (p != null) {
            return put(p, value);
        }
        return null;
    }

    /**
     * Put by parsing value from string.
     *
     * @return previous value associated with the key
     */
    @Nullable
    public Object put(@NotNull YeelightProp key, @Nullable String value) {
        Object o;
        switch (key.type) {
            case YeelightProp.TYPE_INT:
                o = Utils.parseInt(value);
                break;
            case YeelightProp.TYPE_ON_OFF:
                o = Utils.isOn(value);
                break;
            case YeelightProp.TYPE_COLOR_MODE:
                o = YeelightProp.ColorMode.from(Utils.parseInt(value, 0));
                break;
            case YeelightProp.TYPE_ARRAY:
                //change to parse the array? (params of color flow)
                o = value;
                break;
            default: //default (also string type)
                o = value;
        }
        return super.put(key, o);
    }

    /**
     * Get int prop or undefined.
     */
    public int getInt(@NotNull YeelightProp prop) {
        return get(prop, YeelightDevice.UNDEFINED_VALUE);
    }

    /**
     * Get color int prop (with 255 alpha) or undefined.
     */
    public int getColorInt(@NotNull YeelightProp prop) {
        int rawColor = get(prop, YeelightDevice.UNDEFINED_VALUE);
        if (rawColor == YeelightDevice.UNDEFINED_VALUE)
            return rawColor;
        return Utils.toARGB(rawColor);
    }

    /**
     * Get color int prop (with 255 alpha) as readable hex string.
     */
    public String getColorString(@NotNull YeelightProp prop) {
        int rawColor = get(prop, YeelightDevice.UNDEFINED_VALUE);
        if (rawColor == YeelightDevice.UNDEFINED_VALUE)
            return "0x00000000";
        rawColor = Utils.toARGB(rawColor);
        return String.format("0x%08X", rawColor);
    }

    /**
     * Get prop with casted type, or default value if missing.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T get(@NotNull YeelightProp prop, @NotNull T defaultValue) {
        Object o = super.get(prop);
        if (o == null || o.getClass() != defaultValue.getClass())
            return defaultValue;
        return (T) o;
    }

    @NotNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            for (YeelightProp p : keySet()) {
                Object o = get(p);
                switch (p.type) {
                    case YeelightProp.TYPE_ON_OFF:
                        o = Utils.isOnFromBoolean((boolean) o);
                        break;
                    case YeelightProp.TYPE_COLOR_MODE:
                        o = ((YeelightProp.ColorMode) o).ordinal();
                        break;
                    case YeelightProp.TYPE_ARRAY:
                        // o = o;  //todo: unparse the array?
                        break;
                    default:
                        // do nothing (o is a String)
                }
                json.put(p.name(), o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
