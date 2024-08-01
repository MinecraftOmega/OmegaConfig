package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

public class MapField {
    private MapField() {}

    public static class Context<K, V> extends BaseConfigField.Context<Map<K, V>> {
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(Map<K, V> stringValue) {
            Util.setField(field, context, stringValue);
        }

        @Override
        public Map<K, V> get() {
            return this.value;
        }
    }

    public static class Built<K, V> extends BaseConfigField.Built<Map<K, V>> {
        public Built(Object context, Annotation[] annotations, Map<K, V> defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(Map<K, V> aByte) {
            this.value = aByte;
        }

        @Override
        public Map<K, V> get() {
            return value;
        }
    }
}
