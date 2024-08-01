package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.IntSupplier;

public class ShortField {
    private ShortField() {}

    public static class Context extends BaseConfigField.Context<Short> implements BaseConfigField.Primitive {
        public short value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Short shortValue) {
            Util.setField(field, context, shortValue);
        }

        @Override
        public Short get() {
            return this.value;
        }

        public short getAsShort() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Short> implements BaseConfigField.Primitive {
        public short value;
        public Built(Object context, Annotation[] annotations, Short defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Short shortValue) {
            super.value = shortValue;
            this.value = shortValue;
        }

        @Override
        public Short get() {
            return value;
        }

        public short getAsShort() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
