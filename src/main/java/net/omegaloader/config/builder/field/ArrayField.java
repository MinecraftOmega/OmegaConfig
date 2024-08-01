package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ArrayField {
    private ArrayField() {}

    public static class Context<T> extends BaseConfigField.Context<T[]> {
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(T[] stringValue) {
            Util.setField(field, context, stringValue);
        }

        @Override
        public T[] get() {
            return this.value;
        }
    }

    public static class Built<T> extends BaseConfigField.Built<T[]> {
        public Built(Object context, Annotation[] annotations, T[] defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(T[] aByte) {
            this.value = aByte;
        }

        @Override
        public T[] get() {
            return value;
        }
    }
}
