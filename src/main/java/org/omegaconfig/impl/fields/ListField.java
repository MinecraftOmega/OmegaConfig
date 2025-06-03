package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;
import org.omegaconfig.Tools;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ListField<T> extends CollectionField<List<T>, T> {
    public ListField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, Field field, Object context, Class<T> subType) {
        super(name, group, comments, stringify, singleline, allowEmpty, unique, limit, filter, field, context, subType);
    }

    public ListField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, List<T> defaultValue, Class<T> subType) {
        super(name, group, comments, stringify, singleline, allowEmpty, unique, limit, filter, defaultValue, subType);
    }

    @Override
    public CollectionField<List<T>, T> setArray(Object[] array) {
        List<T> fresh = new ArrayList<>();
        for (Object obj: array) {
            if (!this.subType.isInstance(obj)) {
                throw new IllegalArgumentException("Array contains elements not of type " + this.subType.getName());
            }
            fresh.add(this.subType.cast(obj));
        }
        this.set(fresh);
        return this;
    }

    @Override
    public void validate() {
        if (this.get().isEmpty() && !this.allowEmpty) {
            this.reset();
            return;
        }
        // FILTER
        if (this.unique && this.get().size() != this.get().stream().distinct().count()) {
            this.set(this.get().stream().distinct().collect(Collectors.toCollection(ArrayList::new)));
            return;
        }
        // TRUNCATE VALUES
        if (this.limit > 0 && this.get().size() > this.limit) {
            this.set(this.get().stream().distinct().limit(this.limit).collect(Collectors.toCollection(ArrayList::new)));
        }
    }

    @Override
    @SuppressWarnings("unchecked") // List<T>.class doesn't exist
    public Class<List<T>> type() {
        return (Class<List<T>>) (Class<?>) List.class;
    }

    @Override
    public List<T> get() {
        return Collections.unmodifiableList(super.get());
    }

    @Override
    public ListField<T> add(T element) {
        if (element == null) {
            return this; // Ignore null elements
        }
        List<T> current = super.get();

        try {
            current.add(element);
        } catch (Exception e) {
            var list = new ArrayList<>(current);
            list.add(element);
            this.set(list);
        }
        return this;
    }

    @Override
    public ListField<T> remove(T element) {
        if (element == null) {
            return this; // Ignore null elements
        }
        List<T> current = super.get();

        if (current.isEmpty()) {
            return this; // Nothing to remove
        }

        try {
            current.remove(element);
        } catch (Exception e) {
            var list = new ArrayList<>(current);
            list.remove(element);
            this.set(list);
        }
        return this;
    }

    @Override
    public T remove(int index) {
        List<T> current = super.get();

        try {
            return current.remove(index);
        } catch (Exception e) {
            var list = new ArrayList<>(current);
            T value = list.remove(index);
            this.set(list);
            return value;
        }
    }

    @Override
    public boolean contains(T element) {
        return this.get().contains(element);
    }

    @Override
    public int size() {
        return this.get().size();
    }

    @Override
    public ListField<T> clear() {
        this.set(new ArrayList<>());
        return this;
    }
}
