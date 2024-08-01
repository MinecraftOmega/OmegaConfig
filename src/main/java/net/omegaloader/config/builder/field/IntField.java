package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.IntSupplier;

public class IntField {
    private IntField() {}

    public static class Context extends BaseConfigField.Context<Integer> implements BaseConfigField.Primitive, IntSupplier {
        public int value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Integer doubleValue) {
            Util.setField(field, context, doubleValue);
        }

        @Override
        public Integer get() {
            return this.value;
        }

        @Override
        public int getAsInt() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Integer> implements BaseConfigField.Primitive, IntSupplier {
        public int value;
        public Built(Object context, Annotation[] annotations, Integer defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Integer doubleValue) {
            super.value = doubleValue;
            this.value = doubleValue;
        }

        @Override
        public Integer get() {
            return value;
        }

        @Override
        public int getAsInt() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
