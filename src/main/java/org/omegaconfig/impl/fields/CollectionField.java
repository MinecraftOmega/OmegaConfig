package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

public abstract class CollectionField<T, S> extends BaseConfigField<T, S> {
    public final boolean stringify;
    @Deprecated(forRemoval = true)
    public final boolean singleline;
    public final boolean allowEmpty;
    public final boolean unique;
    public final int limit;
    public final Class<? extends Predicate<S>> filter;
    public final Class<S> subType;

    protected CollectionField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<S>> filter, Field field, Object context, Class<S> subType) {
        super(name, group, comments, field, context);
        this.stringify = stringify;
        this.singleline = singleline;
        this.allowEmpty = allowEmpty;
        this.unique = unique;
        this.limit = limit;
        this.filter = filter;
        this.subType = subType;
    }

    protected CollectionField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<S>> filter, T defaultValue, Class<S> subType) {
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
    public Class<S> subType() {
        return this.subType;
    }

    public abstract CollectionField<T, S> setArray(Object[] array);

    public abstract CollectionField<T, S> add(S element);

    public abstract CollectionField<T, S> remove(S element);

    public abstract S remove(int index);

    public abstract boolean contains(S element);

    public abstract int size();

    public abstract CollectionField<T, S> clear();
}
