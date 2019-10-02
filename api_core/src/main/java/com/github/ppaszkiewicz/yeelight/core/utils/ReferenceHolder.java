package com.github.ppaszkiewicz.yeelight.core.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/** Reference wrapper to store strong and weak references in same variable. */
public abstract class ReferenceHolder<T> {
    private ReferenceHolder(){ }

    /** Create weak reference to object. */
    @NotNull
    public static <T>ReferenceHolder<T> weak(@Nullable T object){
        return new Weak<>(object);
    }

    /** Wrap weak reference object. */
    @NotNull
    public static <T>ReferenceHolder<T> of(@Nullable WeakReference<T> object){
        if(object == null) return new Null<>();
        return new Weak<>(object);
    }

    /** Create hard reference to object. */
    @NotNull
    public static <T>ReferenceHolder<T> strong(@Nullable T object){
        if(object == null) return new Null<>();
        return new Strong<>(object);
    }

    /** Holds no reference and always returns null. */
    @NotNull
    public static <T>ReferenceHolder<T> ofNull(){
        return new Null<>();
    }

    /** Obtain referenced value. */
    @Nullable
    public abstract T get();

    private static final class Weak<T> extends ReferenceHolder<T>{
        private final WeakReference<T> reference;

        private Weak(@Nullable T reference) {
            this.reference = new WeakReference<>(reference);
        }

        private Weak(@NotNull WeakReference<T> weakReference){
            reference = weakReference;
        }

        @Nullable
        @Override
        public T get() {
            return reference.get();
        }
    }

    private static final class Strong<T> extends ReferenceHolder<T>{
        private final T reference;

        private Strong(@Nullable T reference) {
            this.reference = reference;
        }

        @Nullable
        @Override
        public T get() {
            return reference;
        }
    }

    private static final class Null<T> extends ReferenceHolder<T>{
        private Null() { }

        @Nullable
        @Override
        public T get() {
            return null;
        }
    }
}
