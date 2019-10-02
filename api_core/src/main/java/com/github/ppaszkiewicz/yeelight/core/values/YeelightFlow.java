package com.github.ppaszkiewicz.yeelight.core.values;

import com.github.ppaszkiewicz.yeelight.core.YLog;
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Device flow. Use builder to initialize.
 */
public final class YeelightFlow implements YeelightCommand.CustomParam {
    public static final int REPEAT_INFINITE = 0;
    public static final int BRIGHTNESS_IGNORE = -1;

    @NotNull
    private final EndAction endAction;
    private final int stepCount;
    @NotNull
    private final Element[] elements;

    /**
     * JsonArray to string (used in flow)
     */
    @NotNull
    private static String jsArrayToString(@NotNull JSONArray arr) {
        String s = arr.toString();
        return s.substring(1, s.length() - 1);
    }

    /**
     * JSonArray from quoted string (used in flow)
     */
    @NotNull
    private static JSONArray jsArrayFromString(@NotNull String s) throws JSONException {
        return new JSONArray("[" + s + "]");
    }

    private YeelightFlow(@NotNull EndAction endAction, int stepCount, @NotNull Element[] elements) {
        this.endAction = endAction;
        this.stepCount = stepCount;
        this.elements = elements;
    }

    /**
     * This restores flow from JSON array. Use {@link Builder} to create new flow.
     */
    public YeelightFlow(@NotNull String json) throws JSONException {
        JSONArray root = new JSONArray(json);
        stepCount = root.getInt(0);
        endAction = EndAction.from(root.getInt(1));

        JSONArray arr = jsArrayFromString(root.getString(2));
        elements = new Element[arr.length() / 4];
        for (int i = 0; i < arr.length(); i += 4) {
            elements[i] = new Element(
                    arr.getInt(i),
                    ElementMode.from(arr.getInt(i + 1)),
                    arr.getInt(i + 2),
                    arr.getInt(i + 3));
        }
    }

    /**
     * This flow as Json string.
     */
    @NotNull
    public String toJson() {
        JSONArray array = new JSONArray();
        addToJSONArray(array);
        return array.toString();
    }

    @Override
    public void addToJSONArray(@NotNull JSONArray jsonArray) {
        jsonArray.put(stepCount);
        jsonArray.put(endAction.ordinal());
        JSONArray params = new JSONArray();
        for (Element e : elements) {
            e.addToJSONArray(params);
        }
        //params are added without leading or trailing [ ]
        jsonArray.put(jsArrayToString(params));
    }


    /**
     * Flow builder. By default flow runs once and recovers to current state.
     */
    public static class Builder {
        private EndAction endAction = EndAction.recover;
        private int repeatCount = 1;
        private int count = 0;
        private final ArrayList<Element> elements = new ArrayList<>();

        /**
         * Change end action. Default is {@link EndAction#recover}.
         */
        @NotNull
        public Builder endAction(@NotNull EndAction endAction) {
            this.endAction = endAction;
            return this;
        }

        /**
         * Set how many flow steps should be performed before end action is called.
         * <p>Count is exclusive against {@link #repeatCount(int)}</p>
         */
        @NotNull
        public Builder count(int count) {
            if (count == REPEAT_INFINITE) {
                repeatCount(0);
            } else
                repeatCount = 1;
            return this;
        }

        /**
         * Set repeat count of all steps - by default 1.
         * <p>Repeat count is exclusive against {@link #count(int)}</p>
         */
        @NotNull
        public Builder repeatCount(int repeatCount) {
            this.repeatCount = repeatCount;
            count = 0;
            return this;
        }

        /**
         * Change color, keeping current brightness value.
         *
         * @param duration in milliseconds
         * @param value    rgb color
         */
        @NotNull
        public Builder color(int duration, int value) {
            value = value & 0x00FFFFFF;   //drop alpha info
            return color(duration, value, BRIGHTNESS_IGNORE);
        }

        /**
         * Change color and brightness.
         *
         * @param duration   in milliseconds
         * @param value      rgb color
         * @param brightness 0 - 100
         */
        @NotNull
        public Builder color(int duration, int value, int brightness) {
            value = value & 0x00FFFFFF;   //drop alpha info
            elements.add(new Element(duration, ElementMode.color, value, brightness));
            return this;
        }

        /**
         * Change color in multiple steps, keeping current brightness value.
         *
         * @param stepDuration of each step in milliseconds
         * @param values   rgb colors
         */
        @NotNull
        public Builder colorChain(int stepDuration, int... values) {
            for (int value : values) {
                elements.add(new Element(stepDuration, ElementMode.color, value & 0x00FFFFFF, BRIGHTNESS_IGNORE));
            }
            return this;
        }

        /**
         * Change color temperature, keeping current brightness value.
         *
         * @param duration in milliseconds
         * @param value    temperature ( 1700 - 6500 )
         */
        @NotNull
        public Builder temp(int duration, int value) {
            return temp(duration, value, BRIGHTNESS_IGNORE);
        }

        /**
         * Change color temperature and brightness.
         *
         * @param duration   in milliseconds
         * @param value      temperature ( 1700 - 6500 )
         * @param brightness 0 - 100
         */
        @NotNull
        public Builder temp(int duration, int value, int brightness) {
            elements.add(new Element(duration, ElementMode.temp, value, brightness));
            return this;
        }

        /**
         * Change color temperature in multiple steps, keeping current brightness value.
         *
         * @param stepDuration of each step in milliseconds
         * @param values   temperature ( 1700 - 6500 )
         */
        @NotNull
        public Builder tempChain(int stepDuration, int... values) {
            for (int value : values) {
                elements.add(new Element(stepDuration, ElementMode.temp, value, BRIGHTNESS_IGNORE));
            }
            return this;
        }

        /**
         * Keep current color for set duration
         *
         * @param duration in milliseconds
         */
        @NotNull
        public Builder sleep(int duration) {
            elements.add(new Element(duration, ElementMode.sleep, 0, 0));
            return this;
        }

        @NotNull
        public YeelightFlow build() {
            int countToUse;
            if (count == 0)
                countToUse = repeatCount * elements.size();
            else
                countToUse = count;
            return new YeelightFlow(endAction, countToUse, elements.toArray(new Element[0]));
        }
    }

    public enum EndAction {
        //ordinal is used when sending JSON to bulb - keep order
        recover,
        stay,
        turn_off;


        /**
         * Null safe ordinal dereference. On error returns {@link #recover}.
         */
        @NotNull
        public static EndAction from(int id) {
            try {
                return values[id];
            } catch (ArrayIndexOutOfBoundsException e) {
                YLog.e("EndAction", "unknown end action: " + id);
            }
            return recover;
        }

        @NotNull
        public static EndAction[] values = values();
    }

    /**
     * Type of {@link Element}.
     */
    private enum ElementMode {
        color(1),
        temp(2),
        sleep(7);
        private final int i;

        ElementMode(int i) {
            this.i = i;
        }

        /**
         * Null safe int dereference. On error returns {@link #sleep}.
         */
        @NotNull
        public static ElementMode from(int id) {
            switch (id) {
                case 1:
                    return color;
                case 2:
                    return temp;
                case 7:
                default:
                    return sleep;
            }
        }

        @NotNull
        public static ElementMode[] values = values();
    }

    /**
     * Element of a flow, created by Builder.
     */
    private static class Element implements YeelightCommand.CustomParam {
        private final int duration;
        @NotNull
        private final ElementMode elementMode;
        private final int value;
        private final int brightness;

        private Element(int duration, @NotNull ElementMode elementMode, int value, int brightness) {
            this.duration = duration;
            this.elementMode = elementMode;
            this.value = value;
            this.brightness = brightness;
        }

        @Override
        public void addToJSONArray(@NotNull JSONArray jsonArray) {
            jsonArray.put(duration);
            jsonArray.put(elementMode.i);
            jsonArray.put(value);
            jsonArray.put(brightness);
        }
    }
}
