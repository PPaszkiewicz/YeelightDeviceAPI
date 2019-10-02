package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.PropHashMap;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightCron;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All messages that come from the devices are parsed into this class.
 */
public class YeelightReply {
    /**
     * No id was provided - only used by notification message.
     */
    public final static int NO_ID = -1;
    /**
     * Result is simply OK, no props received.
     */
    public final static String OK = "ok";
    /** ID of the device that received this reply. */
    public final long deviceId;
    /** ID of the reply. */
    public final int id;
    /** True if first result object is an OK. */
    public final boolean ok;
    /** Results array. */
    @Nullable
    public final Object[] results;
    /** Present on props update, containing updated device values. */
    @Nullable
    public final PropHashMap propHashMap;
    /** Present only after get cron. If there was no cron returned, then this object is {@link YeelightCron#NONE}.*/
    @Nullable
    public final YeelightCron cron;
    /** Present only on error. Otherwise 0. */
    public final int code;
    /** Present only on error. */
    @Nullable
    public final String message;

    /** Command that requested this reply. Might be null. */
    @Nullable
    private YeelightCommand command;

    /** Command can only be set once. */
    private boolean wasCommandSet = false;

    /** Constructor for simple OK */
    public YeelightReply(long deviceId, int id, @NotNull Object[] results) {
        this.deviceId = deviceId;
        this.id = id;
        this.results = results;
        this.ok = results.length > 0 && OK.equals(results[0]);
        this.code = 0;
        this.message = null;
        propHashMap = null;
        cron = null;
    }

    /** Constructor for get_prop result */
    public YeelightReply(long deviceId, int id, @NotNull PropHashMap receivedProps) {
        this.deviceId = deviceId;
        this.id = id;
        this.results = null;
        this.ok = true;
        this.code = 0;
        this.message = null;
        propHashMap = receivedProps;
        cron = null;
    }

    /** Constructor for error */
    public YeelightReply(long deviceId, int id, int code, @NotNull String message){
        this.deviceId = deviceId;
        this.id = id;
        this.ok = false;
        this.code = code;
        this.message = message;
        propHashMap = null;
        results = null;
        cron = null;
    }

    /** Constructor for prop update. */
    public YeelightReply(long deviceId, @NotNull PropHashMap propHashMap){
        this.deviceId = deviceId;
        id = NO_ID;
        ok = true;
        code = 0;
        results = null;
        message = null;
        this.propHashMap = propHashMap;
        cron = null;
    }

    /** Constructor for get cron.*/
    public YeelightReply(long deviceId, int id, @NotNull YeelightCron cron){
        this.deviceId = deviceId;
        this.id = id;
        ok = true;
        code = 0;
        results = null;
        message = null;
        this.propHashMap = null;
        this.cron = cron;
    }

    /** Command that matched this reply. Might be null, especially if ID is NO_ID. */
    @Nullable
    public YeelightCommand getCommand() {
        return command;
    }

    /** Set command that matched this reply. This can only be called once. */
    public YeelightReply withRequest(YeelightCommand request) {
        if(wasCommandSet) throw new IllegalStateException("withRequest can only be called once.");
        this.command = request;
        wasCommandSet = true;
        return this;
    }

    /** Only a single result returned, and it's OK. */
    @SuppressWarnings("ConstantConditions")
    public boolean isSingleResult(){
        return ok && results.length == 1;
    }

    /** Single device prop of this type included. */
    public boolean isProp(YeelightProp prop){
        return propHashMap != null
                && propHashMap.size() == 1
                && propHashMap.get(prop) != null;
    }

    /** True if this message bears any prop updates. */
    public boolean hasProps(){
        return propHashMap != null;
    }

    /** Get method this reply was sent for. */
    @Nullable
    public YeelightMethod getMethod(){
        return command != null ? command.method : null;
    }

    /** True if this represents an error. */
    public boolean isError(){
        return message != null;
    }

    @Override
    public String toString() {
        return "YeelightReply["+deviceId+", "+id+", "+getMethod()+"]";
    }
}
