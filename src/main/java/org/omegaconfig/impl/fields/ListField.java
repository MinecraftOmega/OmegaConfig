package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ListField<T> extends BaseConfigField<List<T>, T> {
    public final boolean stringify;
    public final boolean singleline;
    public final boolean allowEmpty;
    public final boolean unique;
    public final int limit;
    public final Class<? extends Predicate<T>> filter;

    private final Class<T> subType;
    public ListField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, Class<T> subType, Field field, Object context) {
        super(name, group, comments, field, context);
        this.stringify = stringify;
        this.singleline = singleline;
        this.allowEmpty = allowEmpty;
        this.unique = unique;
        this.limit = limit;
        this.filter = filter;
        this.subType = subType;
    }

    public ListField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, List<T> defaultValue, Class<T> subType) {
        super(name, group, comments, defaultValue);
        this.stringify = stringify;
        this.singleline = singleline;
        this.allowEmpty = allowEmpty;
        this.unique = unique;
        this.limit = limit;
        this.filter = filter;
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
