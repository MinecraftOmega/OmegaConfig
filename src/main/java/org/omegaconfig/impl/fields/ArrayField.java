package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

public class ArrayField<T> extends CollectionField<T[], T> {
    public ArrayField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, Field field, Object context, Class<T> subType) {
        super(name, group, comments, stringify, singleline, allowEmpty, unique, limit, filter, field, context, subType);
    }

    public ArrayField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<T>> filter, T[] defaultValue, Class<T> subType) {
        super(name, group, comments, stringify, singleline, allowEmpty, unique, limit, filter, defaultValue, subType);
    }

    @Override
    public CollectionField<T[], T> setArray(Object[] array) {
        T[] fresh = (T[]) Array.newInstance(this.subType, array.length);
        for (int i = 0; i < array.length; i++) {
            if (!this.subType.isInstance(array[i])) {
                throw new IllegalArgumentException("Array contains elements not of type " + this.subType.getName());
            }
            fresh[i] = this.subType.cast(array[i]);
        }
        this.set(fresh);
        return this;
    }

    @Override
    public ArrayField<T> add(T element) {
        T[] current = this.get();
        if (current == null || current.length == 0) {
            this.set((T[]) Array.newInstance(this.subType, 1));
            this.get()[0] = element;
        } else {
            T[] newArray = (T[]) Array.newInstance(this.subType, current.length + 1);
            System.arraycopy(current, 0, newArray, 0, current.length);
            newArray[current.length] = element;
            this.set(newArray);
        }
        return this;
    }

    @Override
    public ArrayField<T> remove(T element) {
        T[] current = this.get();
        if (current == null || current.length == 0) {
            return this; // Nothing to remove
        }
        int index = -1;
        for (int i = 0; i < current.length; i++) {
            if (current[i] == element || current[i].equals(element)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            T[] newArray = (T[]) Array.newInstance(this.subType, current.length - 1);
            System.arraycopy(current, 0, newArray, 0, index);
            System.arraycopy(current, index + 1, newArray, index, current.length - index - 1);
            this.set(newArray);
        }
        return this;
    }

    @Override
    public T remove(int index) {
        T[] current = this.get();
        if (current == null || index < 0 || index >= current.length) {
            return null; // Invalid index or empty array
        }
        T removedElement = current[index];
        if (current.length == 1) {
            this.clear(); // If it's the only element, clear the array
        } else {
            T[] newArray = (T[]) Array.newInstance(this.subType, current.length - 1);
            System.arraycopy(current, 0, newArray, 0, index);
            System.arraycopy(current, index + 1, newArray, index, current.length - index - 1);
            this.set(newArray);
        }
        return removedElement;
    }

    @Override
    public boolean contains(T element) {
        for (T item: this.get()) {
            if (item == element || item.equals(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return this.get().length;
    }

    @Override
    public ArrayField<T> clear() {
        this.set((T[]) Array.newInstance(this.subType, 0));
        return this;
    }

    @Override
    public Class<T[]> type() {
        return (Class<T[]>) this.defaultValue.getClass();
    }

    @Override
    public void validate() {
        if (this.get() == null || this.get().length == 0) {
            if (!this.allowEmpty) {
                this.reset();
            }
            return;
        }
        // FILTER
        if (this.unique && this.get().length != Arrays.stream(this.get()).distinct().count()) {
            T[] distinctArray = Arrays.stream(this.get()).distinct().toArray(size -> (T[]) Array.newInstance(this.subType, size));
            this.set(distinctArray);
            return;
        }
        // TRUNCATE VALUES
        if (this.limit > 0 && this.get().length > this.limit) {
            T[] truncatedArray = Arrays.stream(this.get()).distinct().limit(this.limit).toArray(size -> (T[]) Array.newInstance(this.subType, size));
            this.set(truncatedArray);
        }
    }
}
