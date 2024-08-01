package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ByteField {
    private ByteField() {}

    public static class Context extends BaseConfigField.Context<Byte> implements BaseConfigField.Primitive {
        public byte value;
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Byte byteValue) {
            Util.setField(field, context, byteValue);
        }

        @Override
        public Byte get() {
            return this.value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }

    public static class Built extends BaseConfigField.Built<Byte> implements BaseConfigField.Primitive {
        public byte value;
        public Built(Object context, Annotation[] annotations, Byte defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Byte aByte) {
            super.value = aByte;
            this.value = aByte;
        }

        @Override
        public Byte get() {
            return value;
        }

        @Override
        public void refresh0() {
            this.value = super.value;
        }
    }
}
