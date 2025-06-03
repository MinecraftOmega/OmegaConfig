package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public final class EnumField<T extends Enum<T>> extends BaseConfigField<T, T> implements Comparable<T> {

    public EnumField(String name, ConfigGroup group, Set<String> comments, Field field, Object context) {
        super(name, group, comments, field, context);
    }

    public EnumField(String name, ConfigGroup group, Set<String> comments, T defaultValue) {
        super(name, group, comments, defaultValue);
    }

    @Override
    public Class<T> subType() {
        return this.type();
    }

    @Override
    public void validate() {

    }

    @Override
    public Class<T> type() {
        return (Class<T>) this.defaultValue.getClass();
    }

    @Override
    public int compareTo(T o) {
        return this.get().compareTo(o);
    }
}
