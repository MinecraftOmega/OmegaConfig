package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.IntSupplier;

public class FloatField {
    private FloatField() {}

    public static class Context extends BaseConfigField.Context<Float> implements BaseConfigField.Primitive {
        public float value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Float doubleValue) {
            Util.setField(field, context, doubleValue);
        }

        @Override
        public Float get() {
            return this.value;
        }

        public float getAsFloat() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Float> implements BaseConfigField.Primitive {
        public float value;
        public Built(Object context, Annotation[] annotations, Float defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Float doubleValue) {
            super.value = doubleValue;
            this.value = doubleValue;
        }

        @Override
        public Float get() {
            return value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
