package com.github.ppaszkiewicz.yeelight.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom logger implementation for Yeelight library. All classes from this library will call those static logging methods.<br>
 * Inject custom logger if needed.
 */
public abstract class YLog {
    private static YLog instance;
    public boolean isEnabled = true;

    public YLog() {
    }

    public YLog(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @NotNull
    public static YLog getInstance() {
        if (instance == null) {
            instance = new PrintLog();
        }
        return instance;
    }

    /**
     * Inject custom logger to use instead. If null is passed custom logger will be cleared.
     */
    public static void setInstance(@Nullable YLog instance) {
        YLog.instance = instance;
    }

    public static void e(@NotNull String tag, @NotNull String message) {
        getInstance().error(tag, message);
    }

    public static void d(@NotNull String tag, @NotNull String message) {
        YLog l = getInstance();
        if(l.isEnabled) l.debug(tag, message);
    }

    public static void i(@NotNull String tag, @NotNull String message) {
        YLog l = getInstance();
        if(l.isEnabled) l.info(tag, message);
    }

    public static void w(@NotNull String tag, @NotNull String message) {
        YLog l = getInstance();
        if(l.isEnabled) l.warning(tag, message);
    }

    public abstract void error(@NotNull String tag, @NotNull String message);

    public abstract void debug(@NotNull String tag, @NotNull String message);

    public abstract void info(@NotNull String tag, @NotNull String message);

    public abstract void warning(@NotNull String tag, @NotNull String message);

    static class PrintLog extends YLog {
        @Override
        public void error(@NotNull String tag, @NotNull String message) {
            System.err.println(tag + ": " + message);
        }

        @Override
        public void debug(@NotNull String tag, @NotNull String message) {
            System.out.println(tag + ": " + message);
        }

        @Override
        public void info(@NotNull String tag, @NotNull String message) {
            System.out.println("I\\" + tag + ": " + message);
        }

        @Override
        public void warning(@NotNull String tag, @NotNull String message) {
            System.out.println("W\\" + tag + ": " + message);
        }
    }
}