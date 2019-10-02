package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.utils.ReferenceHolder;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Command that can be send to the device.
 */
public class YeelightCommand {
    private final static String TAG = "YeelightCommand";
    public final int id;
    @NotNull
    public final YeelightMethod method;
    @Nullable
    public final Object[] params;
    @Nullable
    public final Effect effect;
    /** Optional listener for this commands reply. */
    @Nullable
    Listener listener;

    /**
     * Message to send to the device
     *
     * @param id     id that will be returned in callback
     * @param method method to send
     * @param effect effect to use. If null change is immediate
     * @param params method params to inject. If any of them implement {@link CustomParam} that interface will be used for injection.
     */
    public YeelightCommand(int id, @NotNull YeelightMethod method, @Nullable Effect effect, @Nullable Object... params) {
        this.id = id;
        this.method = method;
        this.params = params;
        this.effect = effect;
    }

    /**
     * Name of the method.
     */
    @NotNull
    protected String getMethodName() {
        return method.name();
    }

    /**
     * Convert this object to JSON that will be sent to device.
     */
    @NotNull
    public String toJSON() {
        JSONObject root = new JSONObject();
        try {
            root.put("id", id);
            root.put("method", getMethodName());
            JSONArray jsonParams = new JSONArray();
            if (params != null) {
                for (Object o : params) {
                    if (o instanceof CustomParam)
                        ((CustomParam) o).addToJSONArray(jsonParams);
                    else
                        jsonParams.put(o);
                }
            }
            if (effect != null)
                effect.addToJSONArray(jsonParams);
            root.put("params", jsonParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return root.toString();
    }

    @Override
    public String toString() {
        return "YeelightCommand["+id+", "+method+", params["+(params != null ? params.length : "null")+"]]";
    }

    /**
     * Set reply listener for this command. This will be invoked from async thread.<br>
     * This will not be called if device encounters an error or disconnects.<br><br>
     * Note that command keeps hard reference to listener, and connection holds hard reference to unsent
     * commands so it's possible to have a memory leak here.
     */
    @NotNull
    public YeelightCommand onReply(@Nullable Listener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Set reply listener for this command. This will be invoked from async thread.<br>
     * This will not be called if device encounters an error or disconnects.<br><br>
     * This sets up provided listener with a weak reference.
     */
    @NotNull
    public YeelightCommand onReplyWeak(@Nullable Listener listener) {
        this.listener = new ListenerDelegate(new WeakReference<>(listener));
        return this;
    }

    /**
     * Send command with custom method name (missing in enum list).
     */
    public static class Custom extends YeelightCommand {
        private final String methodName;

        public Custom(int id, @NotNull String method, @Nullable Effect effect, @Nullable Object... params) {
            super(id, YeelightMethod.none, effect, params);
            methodName = method;
        }

        @NotNull
        @Override
        protected String getMethodName() {
            return methodName;
        }
    }

    /**
     * Use this subclass to create raw commands. <br>
     * Note that using raw messages will prevent proper reply parsing for some commands (like
     * get_prop and get_cron) and listener callbacks unless command id is explicitly set.<br>
     */
    public static class Raw extends YeelightCommand {
        @NotNull
        private final String raw;

        /**
         * Send raw command. It should have ID 0.
         */
        public Raw(@NotNull String raw) {
            super(0, YeelightMethod.none, null);
            this.raw = raw;
        }

        /**
         * Send raw command. Id in the raw string must be equal to id argument.
         */
        public Raw(int id, @NotNull String raw) {
            super(id, YeelightMethod.none, null);
            this.raw = raw;
        }

        @NotNull
        @Override
        public String toJSON() {
            return raw;
        }
    }

    /**
     * Fade effect of command messages.<br>
     * Currently only sudden (no effect) or fade effect is supported.
     */
    public static final class Effect implements CustomParam {
        public final boolean smooth;
        public final int duration;

        /**
         * Instant change effect.
         */
        @NotNull
        public static final Effect SUDDEN = new Effect(false, 0);
        /**
         * Default effect of 500ms fade.
         */
        @NotNull
        public static final Effect DEFAULT = new Effect(true, 500);

        @NotNull
        public static Effect of(int duration) {
            if (duration == 0)
                return Effect.SUDDEN;
            return new Effect(true, duration);
        }

        private Effect(boolean smooth, int duration) {
            this.smooth = smooth;
            this.duration = duration;
        }

        @Override
        public void addToJSONArray(@NotNull JSONArray jsonArray) {
            jsonArray.put(smooth ? "smooth" : "sudden");
            jsonArray.put(duration);
        }
    }

    /**
     * Special interface that allows custom behavior when serializing to JSON.<br><br>
     * <p>
     * By default, objects are serialized using their toString method.
     */
    public interface CustomParam {
        /**
         * Append this object to JSONArray.<br>
         * This can put multiple values into params array of {@link YeelightCommand} if needed.
         */
        void addToJSONArray(@NotNull JSONArray jsonArray);
    }

    /**
     * Listener for replies to this command.<br>
     * This will not be called if device encounters an error or disconnects.
     */
    public interface Listener {
        /**
         * Provided command got a reply.
         */
        void onReply(@NotNull YeelightReply reply);
    }

    /**
     * Delegates all methods of {@link Listener} down to provided listener, or does nothing
     * if listener is null or reference to it is lost.
     */
    public static class ListenerDelegate implements Listener {
        private ReferenceHolder<Listener> listener;

        public ListenerDelegate() {
            listener = ReferenceHolder.ofNull();
        }

        public ListenerDelegate(@Nullable Listener listener) {
            this.listener = ReferenceHolder.strong(listener);
        }

        public ListenerDelegate(@Nullable WeakReference<Listener> listener) {
            this.listener = ReferenceHolder.of(listener);
        }

        /**
         * Change listener this object delegates to.
         */
        @NotNull
        public ListenerDelegate setListener(@Nullable Listener listener) {
            this.listener = ReferenceHolder.strong(listener);
            return this;
        }

        /**
         * Change listener this object delegates to using weak reference to it.
         */
        @NotNull
        public ListenerDelegate setWeakListener(@Nullable Listener listener) {
            this.listener = ReferenceHolder.weak(listener);
            return this;
        }

        @Nullable
        public Listener getListener() {
            return listener.get();
        }

        @Override
        public void onReply(@NotNull YeelightReply reply) {
            Listener l = getListener();
            if (l != null)
                l.onReply(reply);
        }
    }
}