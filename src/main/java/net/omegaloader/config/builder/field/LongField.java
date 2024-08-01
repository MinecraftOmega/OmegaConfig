package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.LongSupplier;

public class LongField {
    private LongField() {}

    public static class Context extends BaseConfigField.Context<Long> implements BaseConfigField.Primitive, LongSupplier {
        public long value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Long doubleValue) {
            Util.setField(field, context, doubleValue);
        }

        @Override
        public Long get() {
            return this.value;
        }

        @Override
        public long getAsLong() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Long> implements BaseConfigField.Primitive, LongSupplier {
        public long value;
        public Built(Object context, Annotation[] annotations, Long defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Long doubleValue) {
            super.value = doubleValue;
            this.value = doubleValue;
        }

        @Override
        public Long get() {
            return value;
        }

        @Override
        public long getAsLong() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
