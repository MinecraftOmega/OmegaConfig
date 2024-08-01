package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.DoubleSupplier;

public class DoubleField {
    private DoubleField() {}

    public static class Context extends BaseConfigField.Context<Double> implements BaseConfigField.Primitive, DoubleSupplier {
        public double value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Double doubleValue) {
            Util.setField(field, context, doubleValue);
        }

        @Override
        public Double get() {
            return this.value;
        }

        @Override
        public double getAsDouble() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Double> implements BaseConfigField.Primitive, DoubleSupplier {
        public double value;
        public Built(Object context, Annotation[] annotations, Double defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Double doubleValue) {
            super.value = doubleValue;
            this.value = doubleValue;
        }

        @Override
        public Double get() {
            return value;
        }

        @Override
        public double getAsDouble() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
