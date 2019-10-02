package com.github.ppaszkiewicz.yeelight.core;

import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand;
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection;
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionProvider;
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightCron;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightFlow;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod;
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.Adjust;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.MusicMode;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.PowerMode;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.Scene;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.adjust_bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.adjust_color;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.adjust_ct;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_adjust_bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_adjust_color;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_adjust_ct;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_adjust;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_ct_abx;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_default;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_hsv;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_power;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_rgb;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_set_scene;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_start_cf;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_stop_cf;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.bg_toggle;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.cron_add;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.cron_del;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.cron_get;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.dev_toggle;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.get_prop;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_adjust;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_ct_abx;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_default;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_hsv;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_music;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_name;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_power;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_rgb;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.set_scene;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.start_cf;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.stop_cf;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightMethod.toggle;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.ColorMode;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.bg_bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.bg_ct;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.bg_power;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.bg_rgb;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.bright;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.color_mode;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.ct;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.name;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.power;
import static com.github.ppaszkiewicz.yeelight.core.values.YeelightProp.rgb;

/**
 * Yeelight device object. Holds data of each device and contains connection methods.<br><br>
 *
 *  There are two ways of setting connection to a device:
 *  <ol>
 *      <li>Set it explicitly using {@link #setDeviceConnection(YeelightConnection)}</li>
 *      <li>Set up connection provider with {@link #setConnectionProvider(YeelightConnectionProvider)}</li>
 *  </ol>
 *  When using connection provider connection will be lazily fetched the moment any connection method
 *  is called.<br>
 *  If neither connection or provider is set, {@link #canConnect()} will return false and all connection
 *  methods will throw an exception.
 */
public class YeelightDevice implements YeelightConnection.Listener {
    /**
     * Returned by most get methods if there's no data for that key.
     */
    public final static int UNDEFINED_VALUE = -1;
    public final static int TEMP_MIN = 1700;
    public final static int TEMP_MAX = 6500;
    /**
     * Mask of RGB colors without alpha.
     */
    private final static int RGB_MASK = 0x00FFFFFF;

    private final long id;
    @NotNull
    private final YeelightDeviceModel model;
    @NotNull
    private final String address;
    private final int port;
    private int fw_ver;
    private long supportedMethods;
    @NotNull
    private final PropHashMap props;
    @NotNull
    private YeelightCommand.Effect defaultEffect = YeelightCommand.Effect.DEFAULT;

    /**
     * If this device is online (connected to power and in the same wifi). This has to be manually
     * set unless it's explicitly handled by the connection.
     */
    private boolean isOnline = false;

    /**
     * Raised if this device was discovered during last scan/announced itself recently.
     * */
    private boolean isDiscovered = false;

    /**
     * Provider to obtain connections from.
     */
    @Nullable
    private YeelightConnectionProvider connectionProvider;
    /**
     * Provided or created connection.
     */
    @Nullable
    private YeelightConnection deviceConnection;

    /**
     * Parses fields from provided map (discover or announcement message).
     */
    static YeelightDevice fromDiscoveryMap(@NotNull Map<String, String> map) {
        return new YeelightDevice(map);
    }

    /**
     * Parses fields from provided map (discover or announcement message).
     */
    protected YeelightDevice(@NotNull Map<String, String> map) {
        id = Utils.parseLong(map.get("id"));
        model = YeelightDeviceModel.from(map.get("model"));
        props = new PropHashMap(map);
        fw_ver = Utils.parseInt(map.get("fw_ver"));
        supportedMethods = YeelightMethod.parseToLong(map.get("support"));

        //split location string into ip and port
        String[] location = map.get("Location").split("//")[1].split(":");
        address = location[0];
        port = Integer.parseInt(location[1]);
    }

    /**
     * Restore from json.
     */
    public static YeelightDevice fromJSON(@NotNull JSONObject json) throws JSONException {
        return new YeelightDevice(json);
    }

    /**
     * Restore from json string.
     */
    public static YeelightDevice fromJSONString(@NotNull String json) throws JSONException {
        return new YeelightDevice(new JSONObject(json));
    }

    /**
     * Restore from json string.
     */
    protected YeelightDevice(@NotNull JSONObject json) throws JSONException {
        id = json.getLong("id");
        model = YeelightDeviceModel.from(json.getString("model"));
        address = json.getString("address");
        port = json.getInt("port");
        fw_ver = json.getInt("fw_ver");
        supportedMethods = YeelightMethod.parseToLong(json.getString("supp"));
        if (json.has("props"))
            props = new PropHashMap(json.getJSONObject("props"));
        else
            props = new PropHashMap();
    }

    /**
     * Basic device object with empty properties map.
     *
     * @param id      devices id
     * @param address local address
     * @param port    local port (default is <code>55443</code>)
     */
    public YeelightDevice(long id, @NotNull YeelightDeviceModel model, @NotNull String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.model = model;
        this.props = new PropHashMap();
    }

    /**
     * Full constructor.
     */
    public YeelightDevice(long id, @NotNull YeelightDeviceModel model, @NotNull String address,
                          int port, int fw_ver, long supportedMethods, @Nullable PropHashMap props) {
        this.id = id;
        this.model = model;
        this.address = address;
        this.port = port;
        this.fw_ver = fw_ver;
        this.supportedMethods = supportedMethods;
        if (props != null)
            this.props = props;
        else
            this.props = new PropHashMap();
    }


    /**
     * Store device data as json.
     *
     * @param includeProps true to include props (current device state), false to exclude them
     */
    @NotNull
    public JSONObject toJson(boolean includeProps) {
        JSONObject root = new JSONObject();
        try {
            root.put("id", id);
            root.put("model", model.name);
            root.put("fw_ver", fw_ver);
            root.put("supp", supportedMethods);
            root.put("address", address);
            root.put("port", port);
            if (includeProps)
                root.put("props", props.toJson());
        } catch (JSONException jsonEx) {
            jsonEx.printStackTrace();
        }
        return root;
    }

    /** Copy all props from given device, for example after it was rescanned/announced.
     * @return true if anything was updated, false if props were the same */
    public boolean copyProps(@NotNull YeelightDevice device){
        if(!props.equals(device.props)) {
            props.putAll(device.props);
            return true;
        }
        return false;
    }

    /**
     * Store entire device data as json.
     */
    @NotNull
    public JSONObject toJson() {
        return toJson(true);
    }


    @Override
    public String toString() {
        return "YeelightDevice [" + model + " " + id + " @ " + address + ":" + port + "]@" + System.identityHashCode(this);
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YeelightDevice && hashCode() == obj.hashCode();
    }

    /**
     * True if device is connected to power and same wifi network.<br>
     * This value is always false for new objects and have to be set by user or connection.
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Set if this device should appear as connected to power and same wifi network.
     */
    @NotNull
    public YeelightDevice setOnline(boolean online) {
        isOnline = online;
        return this;
    }

    /**
     * If this device was discovered or announced itself.
     * */
    public boolean isDiscovered() {
        return isDiscovered;
    }

    /**
     * Raise if this device should be treated as recently discovered / announced.
     */
    @NotNull
    public YeelightDevice setDiscovered(boolean discovered) {
        isDiscovered = discovered;
        return this;
    }

    /**
     * Set provider that will be queried for connection if it doesn't exist. This must be used before
     * any connection is established.
     *
     * @return self
     */
    @NotNull
    public YeelightDevice setConnectionProvider(YeelightConnectionProvider connectionProvider) {
        if (deviceConnection != null)
            throw new IllegalStateException("Connection to device was already established.");
        this.connectionProvider = connectionProvider;
        return this;
    }

    /**
     * Current connection provider.
     */
    @Nullable
    public YeelightConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    /**
     * Set update listener of the underlying {@link YeelightConnection}.<br>
     * If underlying connection is not created and listener is not null, connection will be
     * queried from the provider.
     */
    public void setConnectionListener(@Nullable YeelightConnection.Listener connectionListener) {
        // don't initialize connection if it doesn't exist and this is used to clear listener
        if (connectionListener == null && deviceConnection == null)
            return;
        getDeviceConnection().setConnectionListener(connectionListener);
    }

    /**
     * Connect to the device asynchronously. Some connections do not require this method call,
     * but if they do ensure they are properly closed as well.
     *
     * @return devices connection
     */
    @NotNull
    public YeelightConnection connect() {
        YeelightConnection conn = getDeviceConnection();
        conn.connect();
        return conn;
    }

    /**
     * Explicitly set or replace this devices connection instead of relying on provider.
     *
     * @return false if connection was rejected (not for this device)
     */
    public boolean setDeviceConnection(@Nullable YeelightConnection conn) {
        if (conn != null && conn.deviceId != id) {
            throw new IllegalArgumentException("this connection is not for this device: " + conn.deviceId + " != " + id);
        }
        deviceConnection = conn;
        return true;
    }

    /**
     * Underlying device connection. If not set this will use {@link #connectionProvider} to create it. If provider
     * is not set either this throws an exception.
     */
    @NotNull
    public YeelightConnection getDeviceConnection() {
        YeelightConnection conn = deviceConnection;
        if (conn != null && !conn.isReleased()) return conn;
        YeelightConnectionProvider provider = connectionProvider;
        if (provider != null) {
            conn = provider.getConnection(this);
            deviceConnection = conn;
        } else
            throw new IllegalStateException("Cannot obtain a valid connection - neither connection nor provider is set!");
        return conn;
    }

    /**
     * Underlying device connection. Unlike {@link #getDeviceConnection()} this will not invoke
     * connection provider and it can return released connections as well.
     */
    @Nullable
    public YeelightConnection getExistingConnection() {
        return deviceConnection;
    }

    /**
     * True if underlying connection is connected and active.<br>
     *
     * Note that if this device does not have a valid connection it will create one now.
     */
    public boolean isConnected() {
        return getDeviceConnection().isConnected();
    }

    /**
     * Check whether {@link #getDeviceConnection()} can succeed. If not, any connection methods will
     * throw an exception.
     */
    public boolean canConnect() {
        return (deviceConnection != null && !deviceConnection.isReleased()) || connectionProvider != null;
    }

    /**
     * Set effect used to animate between all device changes.
     *
     * @param effect effect to use
     */
    public void setDefaultEffect(@NotNull YeelightCommand.Effect effect) {
        defaultEffect = effect;
    }

    /**
     * Effect used to animate between changes.
     */
    @NotNull
    public YeelightCommand.Effect getDefaultEffect() {
        return defaultEffect;
    }

    /**
     * ID of a Yeelight WiFi LED device, 3rd party device should use this value to
     * uniquely identified a Yeelight WiFi LED device.
     */
    public long getId() {
        return id;
    }

    /**
     * Product model of a Yeelight smart device. Currently it can be "mono",
     * "color", “stripe”, “ceiling”, “bslamp”. For "mono", it represents device that only supports
     * brightness adjustment. For "color", it represents device that support both color and color
     * temperature adjustment. “Stripe” stands for Yeelight smart LED stripe. “Ceiling” stands
     * for Yeelight Ceiling Light. More values may be added in future
     */
    @NotNull
    public YeelightDeviceModel getModel() {
        return model;
    }

    /**
     * Current local IP of the device.
     */
    @NotNull
    public String getAddress() {
        return address;
    }

    /**
     * Current local port of the device.
     */
    public int getPort() {
        return port;
    }

    /**
     * All supported methods. This is a long containing flag values of {@link YeelightMethod}.
     */
    public long getSupportedMethods() {
        return supportedMethods;
    }

    /**
     * See if device supports given method.
     */
    public boolean supportsMethod(@NotNull YeelightMethod method) {
        return (supportedMethods & method.flag) == method.flag;
    }

    /* Below are methods wrapping around actions from YeelightMethod. */

    /* ************************************************************
        1 : get_prop
     *************************************************************/

    /**
     * Map of cached properties of this device.
     * Modifying it will not change state of the device.
     */
    @NotNull
    public PropHashMap getProps() {
        return props;
    }

    /**
     * Request property updates from the device.<br>
     * Connection should automatically update this device, and you will receive callback to the listener.
     *
     * @param props properties to update
     * @return id of request that will be sent, NOT the requested property.
     */
    @NotNull
    public YeelightCommand updateProps(@NotNull YeelightProp... props) {
        return command(get_prop, (Object[]) props);
    }

    /* ************************************************************
        2 : set_ct_abx
     *************************************************************/

    /**
     * Color temperature
     *
     * @return temp (1700 - 6500)
     */
    public int getColorTemp() {
        return props.getInt(ct);
    }

    /**
     * Set temperature
     *
     * @param temperature 1700 ~ 6500(k)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setColorTemp(int temperature) {
        return setColorTemp(temperature, defaultEffect.duration);
    }

    @NotNull
    public YeelightCommand setColorTemp(int temperature, int fadeTime) {
        return commandFade(set_ct_abx, fadeTime, temperature);
    }

    /* ************************************************************
        3 : set_rgb
     *************************************************************/

    /**
     * Get rgb color of the device.
     *
     * @return color int, or -1 if there's no color data
     */
    public int getColor() {
        return props.getColorInt(rgb);
    }

    /**
     * Change RGB color of the device
     *
     * @param color color int (alpha is ignored)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setColor(int color) {
        return setColor(color, defaultEffect.duration);
    }

    /**
     * Change RGB color of the device
     *
     * @param color color int (alpha is ignored)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setColor(int color, int fadeTime) {
        return commandFade(set_rgb, fadeTime, color & RGB_MASK);
    }

    /* ************************************************************
        4 : set_hsv
     *************************************************************/

    public int getHue() {
        return props.getInt(YeelightProp.hue);
    }

    public int getSaturation() {
        return props.getInt(YeelightProp.sat);
    }

    /**
     * Return HSV float array of [hue (0-359), saturation (0.0 - 1.0)].
     */
    public float[] getHSVColor() {
        return new float[]{(float) getHue(), getSaturation() / 100f, 1f};
    }

    /**
     * Set HSV color
     *
     * @param hue        0 - 359
     * @param saturation 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setHSVColor(int hue, int saturation) {
        return setHSVColor(hue, saturation, defaultEffect.duration);
    }

    /**
     * Set HSV color
     *
     * @param hue        0 - 359
     * @param saturation 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setHSVColor(int hue, int saturation, int fadeTime) {
        return commandFade(set_hsv, fadeTime, hue, saturation);
    }

    /* ************************************************************
        5 : set_bright
     *************************************************************/

    /**
     * Brightness in 0-100 range.
     */
    public int getBrightness() {
        return props.getInt(bright);
    }

    /**
     * Change brightness.
     *
     * @param brightness between 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBrightness(int brightness) {
        return setBrightness(brightness, defaultEffect.duration);
    }

    /**
     * Change brightness.
     *
     * @param brightness between 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBrightness(int brightness, int fadeTime) {
        return commandFade(set_bright, fadeTime, brightness);
    }

    /* ************************************************************
        6 : set_power
     *************************************************************/

    /**
     * If device is ON. False means software-managed OFF. (unpowered devices are inaccessible)
     */
    public boolean getPower() {
        return props.get(power, false);
    }

    /**
     * This method is used to switch on or off the smart LED (software managed on/off).
     */
    @NotNull
    public YeelightCommand setPower(boolean on) {
        return setPower(on, defaultEffect.duration, PowerMode.normal);
    }

    /**
     * This method is used to switch on or off the smart LED (software managed on/off).
     */
    @NotNull
    public YeelightCommand setPower(boolean on, @NotNull PowerMode mode) {
        return setPower(on, defaultEffect.duration, mode);
    }

    /**
     * This method is used to switch on or off the smart LED (software managed on/off).
     */
    @NotNull
    public YeelightCommand setPower(boolean on, int fadeTime) {
        return setPower(on, fadeTime, PowerMode.normal);
    }

    /**
     * This method is used to switch on or off the smart LED (software managed on/off).
     */
    @NotNull
    public YeelightCommand setPower(boolean on, int fadeTime, @NotNull PowerMode mode) {
        return commandFade(set_power, fadeTime, Utils.isOnFromBoolean(on), mode);
    }

    /* ************************************************************
        7 : toggle
     *************************************************************/

    @NotNull
    public YeelightCommand togglePower() {
        return command(toggle);
    }

    /* ************************************************************
        8 : set_default
     *************************************************************/

    /**
     * Save current state of smart LED in persistent memory. So if user powers off and then powers on the smart LED again (hard power reset),
     * the smart LED will show last saved state
     *
     * @return id of sent command
     */
    @NotNull
    public YeelightCommand setDefault() {
        return command(set_default);
    }

    /* ************************************************************
        9 : start_cf
     *************************************************************/

    /**
     * Start a color flow. Color flow is a series of smart LED visible state changing.
     */
    @NotNull
    public YeelightCommand startColorFlow(@NotNull YeelightFlow flow) {
        return command(start_cf, flow);
    }

    /**
     * Device current flowing state.
     */
    public boolean isFlowing() {
        return props.getInt(YeelightProp.flowing) == 1;
    }

    /* ************************************************************
        10: stop_cf
     *************************************************************/

    /**
     * Stop a running color flow.
     */
    @NotNull
    public YeelightCommand stopColorFlow() {
        return command(stop_cf);
    }

    /* ************************************************************
        11: set_scene
     *************************************************************/

    /**
     * Set scene - temp and brightness together, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setSceneColorTemp(int temperature, int brightness) {
        return command(set_scene, Scene.ct, temperature, brightness);
    }

    /**
     * Set scene - RGB color and brightness, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setSceneColor(int color, int brightness) {
        return command(set_scene, Scene.color, color & RGB_MASK, brightness);
    }

    /**
     * Set scene - HSV color and brightness, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setSceneHSVColor(int hue, int saturation, int brightness) {
        return command(set_scene, Scene.hsv, hue, saturation, brightness);
    }

    /**
     * Start a color flow but defined as a scene turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setSceneColorFlow(@NotNull YeelightFlow flow) {
        return command(set_scene, Scene.cf, flow);
    }

    /**
     * Set specified brightness turning ON the device if needed and turn off after delay (in minutes).
     */
    @NotNull
    public YeelightCommand setDelayedOff(int delay, int brightness) {
        return command(set_scene, Scene.auto_delay_off, brightness, delay);
    }

    /* ************************************************************
        12: cron_add
     *************************************************************/

    /**
     * Add timed job.
     *
     * @param task    job to run
     * @param minutes delay of job
     * @return id of sent request
     */
    @NotNull
    public YeelightCommand setCron(@NotNull YeelightCron.Type task, int minutes) {
        return command(cron_add, task, minutes);
    }

    /* ************************************************************
        13: cron_get
     *************************************************************/

    /**
     * Request Type task from the device. Note this returns command ID, value comes to listener.
     */
    @NotNull
    public YeelightCommand requestCron(@NotNull YeelightCron.Type task) {
        return command(cron_get, task);
    }

    /* ************************************************************
        14: cron_del
     *************************************************************/

    /**
     * Remove timed job.
     *
     * @param task job to remove
     * @return id of sent request
     */
    @NotNull
    public YeelightCommand delCron(@NotNull YeelightCron.Type task) {
        return command(cron_del, task);
    }

    /* ************************************************************
        15: set_adjust
     *************************************************************/

    @NotNull
    public YeelightCommand increaseBrightness() {
        return setAdjust(Adjust.Action.increase, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand decreaseBrightness() {
        return setAdjust(Adjust.Action.decrease, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand circleBrightness() {
        return setAdjust(Adjust.Action.circle, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand increaseColorTemp() {
        return setAdjust(Adjust.Action.increase, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand decreaseColorTemp() {
        return setAdjust(Adjust.Action.decrease, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand circleColorTemp() {
        return setAdjust(Adjust.Action.circle, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand circleColor() {
        return setAdjust(Adjust.Action.circle, Adjust.Prop.color);
    }

    /**
     * Adjust props. Only {@link Adjust.Action#circle} is valid for {@link Adjust.Prop#color}.
     */
    @NotNull
    public YeelightCommand setAdjust(@NotNull Adjust.Action action, @NotNull Adjust.Prop prop) {
        return command(set_adjust, action, prop);
    }

    /* ************************************************************
        16: set_music
     *************************************************************/

    /**
     * Start music mode.
     */
    @NotNull
    public YeelightCommand startMusicMode(String hostName, int port) {
        return command(set_music, MusicMode.turn_on, hostName, port);
    }

    /**
     * Finish music mode.
     */
    @NotNull
    public YeelightCommand endMusicMode() {
        return command(set_music, MusicMode.turn_off);
    }

    /* ************************************************************
        17: set_name
     *************************************************************/

    /**
     * Get device name. Note that this is not the same as the one set in Yeelight app.
     */
    @NotNull
    public String getName() {
        return props.get(name, "Unknown");
    }

    /**
     * Set device name. Note that this is not the same as the one set in Yeelight app.
     */
    @NotNull
    public YeelightCommand setName(@NotNull String name) {
        return command(set_name, name);
    }

    /* ************************************************************
        18: bg_set_rgb
     *************************************************************/

    /**
     * Get background rgb color of the device.
     *
     * @return color int, or -1 if there's no color data
     */
    public int getBgColor() {
        return props.getColorInt(bg_rgb);
    }

    /**
     * Change background RGB color of the device
     *
     * @param color color int (alpha is ignored)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgColor(int color) {
        return setBgColor(color, defaultEffect.duration);
    }


    /**
     * Change RGB color of the device
     *
     * @param color color int (alpha is ignored)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgColor(int color, int fadeTime) {
        return commandFade(bg_set_rgb, fadeTime, color & RGB_MASK);
    }

    /* ************************************************************
        19: bg_set_hsv
     *************************************************************/

    public int getBgHue() {
        return props.getInt(YeelightProp.bg_hue);
    }

    public int getBgSaturation() {
        return props.getInt(YeelightProp.bg_sat);
    }

    /**
     * Return HSV float array of [hue (0-359), saturation (0.0 - 1.0)].
     */
    public float[] getBgHSVColor() {
        return new float[]{(float) getBgHue(), getBgSaturation() / 100f, 1f};
    }

    /**
     * Set background HSV color
     *
     * @param hue        0 - 359
     * @param saturation 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgHSVColor(int hue, int saturation) {
        return setBgHSVColor(hue, saturation, defaultEffect.duration);
    }

    /**
     * Set background HSV color
     *
     * @param hue        0 - 359
     * @param saturation 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgHSVColor(int hue, int saturation, int fadeTime) {
        return commandFade(bg_set_hsv, fadeTime, hue, saturation);
    }

    /* ************************************************************
        20: bg_set_ct_abx
     *************************************************************/

    /**
     * Background Color temperature
     *
     * @return temp (1700 - 6500)
     */
    public int getBgColorTemp() {
        return props.getInt(bg_ct);
    }

    /**
     * Set temperature
     *
     * @param temperature 1700 ~ 6500(k)
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgColorTemp(int temperature) {
        return setBgColorTemp(temperature, defaultEffect.duration);
    }

    @NotNull
    public YeelightCommand setBgColorTemp(int temperature, int fadeTime) {
        return commandFade(bg_set_ct_abx, fadeTime, temperature);
    }

    /* ************************************************************
        21: bg_start_cf
     *************************************************************/

    /**
     * Start a color flow on background leds. Color flow is a series of smart LED visible state changing.
     */
    @NotNull
    public YeelightCommand startBgColorFlow(@NotNull YeelightFlow flow) {
        return command(bg_start_cf, flow);
    }

    /**
     * Device current background flowing state.
     */
    public boolean isBgFlowing() {
        return props.getInt(YeelightProp.bg_flowing) == 1;
    }

    /* ************************************************************
        22: bg_stop_cf
     *************************************************************/

    /**
     * Stop background color flow.
     */
    @NotNull
    public YeelightCommand stopBgColorFlow() {
        return command(bg_stop_cf);
    }

    /* ************************************************************
        23: bg_set_scene
     *************************************************************/

    /**
     * Set scene - temp and brightness together, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setBgSceneColorTemp(int temperature, int brightness) {
        return command(YeelightMethod.bg_set_scene, Scene.ct, temperature, brightness);
    }

    /**
     * Set scene - RGB color and brightness, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setBgSceneColor(int color, int brightness) {
        return command(bg_set_scene, Scene.color, color & RGB_MASK, brightness);
    }

    /**
     * Set scene - HSV color and brightness, turning ON the device if needed.
     */
    @NotNull
    public YeelightCommand setBgSceneHSVColor(int hue, int saturation, int brightness) {
        return command(bg_set_scene, Scene.hsv, hue, saturation, brightness);
    }

    /**
     * Start color flow but defined as a scene
     */
    @NotNull
    public YeelightCommand setBgSceneColorFlow(@NotNull YeelightFlow flow) {
        return command(bg_set_scene, Scene.cf, flow);
    }

    /**
     * Set specified brightness and turn off background after delay (in minutes)
     */
    @NotNull
    public YeelightCommand setBgDelayedOff(int delay, int brightness) {
        return command(bg_set_scene, Scene.auto_delay_off, brightness, delay);
    }

    /* ************************************************************
        24: bg_set_default
     *************************************************************/

    /**
     * @see #setDefault()
     */
    @NotNull
    public YeelightCommand setBgDefault() {
        return command(bg_set_default);
    }

    /* ************************************************************
        25: bg_set_power
     *************************************************************/

    /**
     * If device is ON. False means software-managed OFF. (unPowered devices are inaccessible)
     */
    public boolean getBgPower() {
        return props.get(bg_power, false);
    }

    /**
     * Change software managed ON-OFF background state.
     */
    @NotNull
    public YeelightCommand setBgPower(boolean on) {
        return setBgPower(on, defaultEffect.duration);
    }

    /**
     * Change software managed ON-OFF background state.
     */
    @NotNull
    public YeelightCommand setBgPower(boolean on, int fadeTime) {
        return commandFade(bg_set_power, fadeTime, Utils.isOnFromBoolean(on));
    }

    /* ************************************************************
        26: bg_set_bright
     *************************************************************/

    /**
     * BgBrightness in 0-100 range.
     */
    public int getBgBrightness() {
        return props.getInt(bg_bright);
    }

    /**
     * Change background brightness.
     *
     * @param brightness between 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgBrightness(int brightness) {
        return setBgBrightness(brightness, defaultEffect.duration);
    }

    /**
     * Change brightness.
     *
     * @param brightness between 0 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand setBgBrightness(int brightness, int fadeTime) {
        return commandFade(bg_set_bright, fadeTime, brightness);
    }

    /* ************************************************************
        27: bg_set_adjust
     *************************************************************/

    /**
     * Adjust props. Only {@link Adjust.Action#circle} is valid for {@link Adjust.Prop#color}.
     */
    @NotNull
    public YeelightCommand setBgAdjust(@NotNull Adjust.Action action, @NotNull Adjust.Prop prop) {
        return command(bg_set_adjust, action, prop);
    }

    // convenience methods
    @NotNull
    public YeelightCommand increaseBgBrightness() {
        return setBgAdjust(Adjust.Action.increase, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand decreaseBgBrightness() {
        return setBgAdjust(Adjust.Action.decrease, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand circleBgBrightness() {
        return setBgAdjust(Adjust.Action.circle, Adjust.Prop.bright);
    }

    @NotNull
    public YeelightCommand increaseBgColorTemp() {
        return setBgAdjust(Adjust.Action.increase, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand decreaseBgColorTemp() {
        return setBgAdjust(Adjust.Action.decrease, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand circleBgColorTemp() {
        return setBgAdjust(Adjust.Action.circle, Adjust.Prop.ct);
    }

    @NotNull
    public YeelightCommand circleBgColor() {
        return setBgAdjust(Adjust.Action.circle, Adjust.Prop.color);
    }

    /* ************************************************************
        28: bg_toggle
     *************************************************************/

    @NotNull
    public YeelightCommand toggleBgPower() {
        return command(bg_toggle);
    }

    /* ************************************************************
        29: dev_toggle
     *************************************************************/

    @NotNull
    public YeelightCommand toggleDevPower() {
        return command(dev_toggle);
    }

    /* ************************************************************
        30: adjust_bright
     *************************************************************/

    /**
     * Adjust brightness by specified percentage.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBrightness(int percentage) {
        return adjustBrightness(percentage, defaultEffect.duration);
    }

    /**
     * Adjust brightness by specified percentage over duration.
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBrightness(int percentage, int duration) {
        return command(adjust_bright, percentage, duration);
    }

    /* ************************************************************
        31: adjust_ct
     *************************************************************/

    /**
     * Adjust color temperature by specified percentage.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustColorTemp(int percentage) {
        return adjustColorTemp(percentage, defaultEffect.duration);
    }

    /**
     * Adjust color temperature by specified percentage over duration.<br>
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustColorTemp(int percentage, int duration) {
        return command(adjust_ct, percentage, duration);
    }

    /* ************************************************************
        32: adjust_color
     *************************************************************/

    /**
     * Adjust color.<br>
     * NOTE: The percentage parameter will be ignored and the color is internally
     * defined and can’t specified.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustColor(int percentage) {
        return adjustColor(percentage, defaultEffect.duration);
    }

    /**
     * Adjust color over duration.<br>
     * NOTE: The percentage parameter will be ignored and the color is internally
     * defined and can’t specified.
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustColor(int percentage, int duration) {
        return command(adjust_color, percentage, duration);
    }

    /* ************************************************************
        33: bg_adjust_bright
     *************************************************************/

    /**
     * Adjust background brightness by specified percentage.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgBrightness(int percentage) {
        return adjustBgBrightness(percentage, defaultEffect.duration);
    }

    /**
     * Adjust background brightness by specified percentage over duration.
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgBrightness(int percentage, int duration) {
        return command(bg_adjust_bright, percentage, duration);
    }

    /* ************************************************************
        34: bg_adjust_ct
     *************************************************************/

    /**
     * Adjust background color temperature by specified percentage.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgColorTemp(int percentage) {
        return adjustBgColorTemp(percentage, defaultEffect.duration);
    }

    /**
     * Adjust background color temperature by specified percentage over duration.
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgColorTemp(int percentage, int duration) {
        return command(bg_adjust_ct, percentage, duration);
    }

    /* ************************************************************
        35: bg_adjust_color
     *************************************************************/

    /**
     * Adjust background color.<br>
     * NOTE: The percentage parameter will be ignored and the color is internally
     * defined and can’t specified.
     *
     * @param percentage between -100 - 100
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgColor(int percentage) {
        return adjustBgColor(percentage, defaultEffect.duration);
    }

    /**
     * Adjust background color over duration.<br>
     * NOTE: The percentage parameter will be ignored and the color is internally
     * defined and can’t specified.
     *
     * @param percentage between -100 - 100
     * @param duration   duration in ms
     * @return command that was sent
     */
    @NotNull
    public YeelightCommand adjustBgColor(int percentage, int duration) {
        return command(bg_adjust_color, percentage, duration);
    }

    /* ************************************************************
        END OF YEELIGHT DEVICE METHODS
     *************************************************************/

    /**
     * Current color mode.
     */
    @NotNull
    public YeelightProp.ColorMode getColorMode() {
        return props.get(color_mode, ColorMode.mode_unknown);
    }

    /**
     * Send raw string message to the device.
     */
    @NotNull
    public YeelightCommand sendRawMessage(@NotNull String message) {
        return sendCommandMessage(new YeelightCommand.Raw(message));
    }

    /**
     * Send single raw command straight to the device.
     */
    @NotNull
    public YeelightCommand sendCommandMessage(@NotNull YeelightCommand command) {
        YeelightConnection conn = getDeviceConnection();
        try {
            conn.send(command);
        } catch (NullPointerException npe) {
            throw new IllegalStateException("Device must have connection or connection factory set.");
        }
        return command;
    }

    /**
     * Helper to create and send YeelightCommand for this device (with fade effect)
     *
     * @param fadeTime time to fade
     * @return sent message
     */
    @NotNull
    private YeelightCommand commandFade(@NotNull YeelightMethod m, int fadeTime, @Nullable Object... params) {
        return sendCommandMessage(new YeelightCommand(nextCommandID(), m, YeelightCommand.Effect.of(fadeTime), params));
    }

    /**
     * Helper to create and send YeelightCommand for this device (without fade effect)
     */
    @NotNull
    private YeelightCommand command(@NotNull YeelightMethod m, @Nullable Object... params) {
        return sendCommandMessage(new YeelightCommand(nextCommandID(), m, null, params));
    }

    /**
     * Unused ID for next {@link YeelightCommand}. This requires valid connection or connection provider.
     */
    public int nextCommandID() {
        return getDeviceConnection().nextMessageId();
    }

    /**
     * Dump device info as multiline string.
     */
    @NotNull
    public String dumpDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        Utils.append(sb, "id", id);
        Utils.append(sb, "address", address);
        Utils.append(sb, "port", port);
        Utils.append(sb, "fw_ver", fw_ver);
        Utils.append(sb, "model", model);
        Utils.append(sb, "isOnline", isOnline);
        Utils.append(sb, "isDiscovered", isDiscovered);

        for (YeelightProp p : props.keySet()) {
            if (p == rgb || p == bg_rgb) {
                // get readable rgb
                Utils.append(sb, p, props.getColorString(p));
            } else
                Utils.append(sb, p, props.get(p));
        }
        sb.append('\n').append("support: ").append(supportedMethods);
        sb.append('\n').append("support: ").append(YeelightMethod.parseToString(supportedMethods));
        return sb.toString();
    }

    @Override
    public void onYeelightDeviceResponse(long deviceId, @NotNull YeelightReply deviceReply) {
        // basic implementation that lets device consume any prop updates from replies
        if (deviceId == id) {
            if (deviceReply.propHashMap != null)
                props.putAll(deviceReply.propHashMap);    //update device data
        } else {
            YLog.e("YeeDevice", "onYeelightDeviceResponse: invalid ID supplied to the device, ignoring data.");
        }
    }

    @Override
    public void onYeelightDeviceConnectionError(long deviceId, @NotNull Throwable exception, YeelightCommand failedCommand) {
        // ignored
    }

    @Override
    public void onYeelightDeviceConnected(long deviceID) {
        // ignored on purpose - some connection types do not stay connected even if device is online
    }

    @Override
    public void onYeelightDeviceDisconnected(long deviceID, @Nullable Throwable error) {
        // ignored on purpose - some connection types do not stay connected even if device is online
    }
}
