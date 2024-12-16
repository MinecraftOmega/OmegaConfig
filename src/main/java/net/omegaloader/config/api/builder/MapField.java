package net.omegaloader.config.api.builder;

import java.lang.reflect.Field;
import java.util.Map;

public class MapField<V> extends BaseConfigField<Map<String, V>> {
    public MapField(String name, GroupField parent, Map<String, V> defaultValue) {
        super(name, parent, defaultValue);
    }

    public MapField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }
}
