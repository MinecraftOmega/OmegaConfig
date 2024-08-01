package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseConfigField<T> implements Supplier<T>, Consumer<T> {

    public static abstract class Built<T> extends BaseConfigField<T> {
        public final Object context;
        public final Annotation[] annotations;
        public final T defaultValue;
        public T value;

        public Built(Object context, Annotation[] annotations, T defaultValue) {
            this.context = context;
            this.annotations = annotations;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }
    }

    public static abstract class Context<T> extends BaseConfigField<T> {
        public final Object context;
        public final Annotation[] annotations;
        public final Field field;
        public T value;

        public Context(Object context, Annotation[] annotations, Field field) {
            this.context = context;
            this.annotations = annotations;
            this.field = field;
            this.field.setAccessible(true);
            this.value = Util.getFieldValue(field, context);
        }
    }

    public void set(T value) {
        this.accept(value);
    }

    public void refresh() {
        if (this instanceof Primitive) {
            ((Primitive) this).refresh0();
        }
    }

    public interface Primitive {
        void refresh0();
    }
}
