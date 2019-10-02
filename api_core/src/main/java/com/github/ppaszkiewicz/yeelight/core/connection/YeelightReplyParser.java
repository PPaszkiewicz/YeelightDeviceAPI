package com.github.ppaszkiewicz.yeelight.core.connection;

import com.github.ppaszkiewicz.yeelight.core.PropHashMap;
import com.github.ppaszkiewicz.yeelight.core.YLog;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightCron;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Queue;

/**
 * Parser converting raw messages from device into {@link YeelightReply} objects.
 */
public abstract class YeelightReplyParser {
    /**
     * Obtain default parser implementation that will return given deviceId.
     */
    @NotNull
    public static YeelightReplyParser obtain(long deviceId) {
        return new DefaultImpl(deviceId);
    }

    /**
     * Create item from JSON reply. This matches id of reply in JSON with command queue.
     *
     * @param json     json string received from the device
     * @param comQueue queue of commands that were SENT to device - this is used to match IDs with replies
     * @return parsed reply or null on any error
     */
    @Nullable
    public abstract YeelightReply parse(@NotNull String json, @NotNull Queue<YeelightCommand> comQueue);

    public static class DefaultImpl extends YeelightReplyParser {
        public final long deviceId;
        protected DefaultImpl(long deviceId) {
            this.deviceId = deviceId;
        }

        @Nullable
        @Override
        public YeelightReply parse(@NotNull String json, @NotNull Queue<YeelightCommand> comQueue) {
            try {
                JSONObject reply = new JSONObject(json);
                if (reply.has("id")) {
                    int replyId = reply.getInt("id");
                    if (reply.has("result")) {
                        JSONArray result = reply.getJSONArray("result");
                        if (replyId == YeelightReply.NO_ID) {
                            return parseReply(replyId, result).withRequest(null);
                        }
                        YeelightCommand m = findInQueue(comQueue, replyId);
                        if (m == null) {
                            // did not find this request in request commands, default parsing
                            return parseReply(replyId, result).withRequest(null);
                        } else {
                            YeelightReply r;
                            // special case for two methods, they need to bounce values from request
                            // or else they'll be meaningless:
                            if (m.method == YeelightMethod.get_prop) {
                                r = parseGetPropReply(replyId, result, m.params);
                            } else if (m.method == YeelightMethod.cron_get) {
                                r = parseCronGetReply(replyId, result);
                            } else
                                r = parseReply(replyId, result);
                            // inject request into reply
                            return r.withRequest(m);
                        }
                    } else if (reply.has("error")) {
                        return parseError(replyId, reply).withRequest(findInQueue(comQueue, replyId));
                    }
                } else {
                    // messages without ID field are device updates
                    YeelightReply update = parseDeviceUpdate(reply);
                    if (update != null) update.withRequest(null);
                    return update;
                }
            } catch (JSONException e) {
                YLog.e("YDReplyParser", "failed to parse: " + json);
                e.printStackTrace();
            }
            return null;
        }


        // keep polling the queue until given ID is found. Returns instantly if ID == NO_ID.
        public YeelightCommand findInQueue(@NotNull Queue<YeelightCommand> comQueue, int id) {
            if (id == YeelightReply.NO_ID) return null;
            YeelightCommand m;
            while (!comQueue.isEmpty()) {
                m = comQueue.poll();
                if (m.id == id) {
                    return m;
                }
            }
            return null;
        }

        // for a simple reply, for example  {"id":1, "result":["ok"]}
        public YeelightReply parseReply(int replyId, @NotNull JSONArray result) throws JSONException {
            // result array is usually a single ["ok"]
            Object[] o = new Object[result.length()];
            for (int i = 0; i < result.length(); i++) {
                o[i] = result.get(i);
            }
            return new YeelightReply(deviceId, replyId, o);
        }

        // for getProp exclusively, reply object order must match the request
        public YeelightReply parseGetPropReply(int replyId, @NotNull JSONArray result, @Nullable Object... requestedProps) throws JSONException {
            // this maps keys (from request) with values (from reply)
            PropHashMap p = new PropHashMap();
            if(requestedProps != null) {
                for (int i = 0; i < requestedProps.length; i++) {
                    if (requestedProps[i] instanceof YeelightProp) {   //ignore invalid params
                        YeelightProp dp = (YeelightProp) requestedProps[i];
                        p.put(dp, result.getString(i));
                    }
                }
            }
            return new YeelightReply(deviceId, replyId, p);
        }

        // parse getCron from reply
        public YeelightReply parseCronGetReply(int replyId, @NotNull JSONArray result) throws JSONException {
            if (result.length() < 1) {
                return new YeelightReply(deviceId, replyId, YeelightCron.NONE);
            }
            return new YeelightReply(deviceId, replyId, YeelightCron.fromJSON(result.getJSONObject(0)));
        }

        // error result
        public YeelightReply parseError(int replyId, @NotNull JSONObject reply) throws JSONException {
            JSONObject err = reply.getJSONObject("error");
            return new YeelightReply(deviceId, replyId,
                    err.getInt("code"),
                    err.getString("message"));
        }

        // prop update from the device, not matching any request
        public YeelightReply parseDeviceUpdate(@NotNull JSONObject reply) throws JSONException {
            String method = reply.getString("method");
            if (method.equals("props")) {
                //this is update notification
                JSONObject paramsObj = reply.getJSONObject("params");
                return new YeelightReply(deviceId, new PropHashMap(paramsObj));
            }
            return null;
        }
    }
}
