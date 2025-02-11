package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ListField<T> extends BaseConfigField<List<T>, T> {

    private final Class<T> subType;
    public ListField(String name, ConfigGroup group, Set<String> comments, Class<T> subType, Field field, Object context) {
        super(name, group, comments, field, context);
        this.subType = subType;
    }

    public ListField(String name, ConfigGroup group, Set<String> comments, List<T> defaultValue, Class<T> subType) {
        super(name, group, comments, defaultValue);
        this.subType = subType;
    }

    @Override
    public Class<T> subType() {
        return this.subType;
    }

    @Override
    @SuppressWarnings("unchecked") // List<T>.class doesn't exists
    public Class<List<T>> type() {
        return (Class<List<T>>) (Class<?>) List.class;
    }

    @Override
    public List<T> get() {
        return Collections.unmodifiableList(super.get());
    }
}
