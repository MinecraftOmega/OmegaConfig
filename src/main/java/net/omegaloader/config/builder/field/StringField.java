package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class StringField {
    private StringField() {}

    public static class Context extends BaseConfigField.Context<String> {
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(String stringValue) {
            Util.setField(field, context, stringValue);
        }

        @Override
        public String get() {
            return this.value;
        }
    }

    public static class Built extends BaseConfigField.Built<String> {
        public Built(Object context, Annotation[] annotations, String defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(String aByte) {
            this.value = aByte;
        }

        @Override
        public String get() {
            return value;
        }
    }
}
