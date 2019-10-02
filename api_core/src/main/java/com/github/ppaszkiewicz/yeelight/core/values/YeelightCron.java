package com.github.ppaszkiewicz.yeelight.core.values;

import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Delayed job task. */
public final class YeelightCron {
    /** Special CRON object present in replies when there's no CRON.*/
    public static final YeelightCron NONE = new YeelightCron(null, 0);
    /** Type of cron or null if there was no cron. */
    @Nullable
    public final Type type;
    public final int delay;
    @Nullable
    public final Object[] params;

    public YeelightCron(@Nullable Type type, int delay, @Nullable Object... params) {
        this.type = type;
        this.delay = delay;
        this.params = params;   //for power_off single param mix (?)
    }

    /**
     * Available delayed jobs.
     * Note: order is important (using ordinal).
     */
    public enum Type implements YeelightCommand.CustomParam {
        // uses ordinal value when sending
        /**
         * delay [minutes]
         */
        power_off;

        final static Type[] values = values();

        @Override
        public void addToJSONArray(@NotNull JSONArray jsonArray) {
            jsonArray.put(ordinal());
        }
    }

    @NotNull
    public static YeelightCron fromJSON(@NotNull JSONObject json) throws JSONException{
        return new YeelightCron(
                Type.values[json.getInt("type")],
                json.getInt("delay"),
                json.getInt("mix")
        );
    }
}
