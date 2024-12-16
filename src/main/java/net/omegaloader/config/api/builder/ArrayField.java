package net.omegaloader.config.api.builder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

public class ArrayField<T> extends BaseConfigField<T[]> implements Iterable<T>, Comparable<T[]>, Predicate<T> {
    private boolean stringify = false;
    private boolean singleline = false;
    private int limit = 0;
    private Predicate<T> filter;
    private Sorting sorting;

    public ArrayField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public ArrayField(String name, GroupField parent, T[] defaultValue) {
        super(name, parent, defaultValue);
    }

    public ArrayField<T> stringify(boolean value) {
        this.assertUnlocked();

        this.stringify = value;
        return this;
    }

    public boolean stringify() {
        this.assertLocked();

        return this.stringify;
    }

    public ArrayField<T> singleLine(boolean value) {
        this.assertUnlocked();

        this.singleline = value;
        return this;
    }

    public boolean singleLine() {
        this.assertLocked();

        return this.singleline;
    }

    public ArrayField<T> limit(int value) {
        this.assertUnlocked();

        if (value < 0) throw new IllegalArgumentException("Limit can't be negative");
        this.limit = value;
        return this;
    }

    public int limit() {
        this.assertLocked();

        return this.limit;
    }

    public ArrayField<T> filter(Predicate<T> value) {
        this.assertUnlocked();
        if (value == null)
            throw new IllegalArgumentException("Filter can't be null");
        this.filter = value;
        return this;
    }

    @Override
    public boolean test(T value) {
        this.assertLocked();

        return this.filter == null || this.filter.test(value);
    }

    public ArrayField<T> sorting(Sorting value) {
        this.assertUnlocked();

        if (value == null)
            throw new IllegalArgumentException("Limit can't be negative");
        this.sorting = value;
        return this;
    }

    public Sorting sorting() {
        this.assertLocked();

        return sorting;
    }

    @Override
    public int compareTo(T[] o) {
        return this.get().length - o.length;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private final T[] clone = Arrays.copyOf(get(), get().length);
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < clone.length;
            }

            @Override
            public T next() {
                return clone[i++];
            }
        };
    }

    /**
     * Sorting mode
     */
    public enum Sorting {
        /**
         * Disables sorting and keep it as-is
         */
        NONE,
        /**
         * Commonly well-known as alphabetical sorting
         * @see <a href="https://en.wikipedia.org/wiki/UTF-8#Codepage_layout">UTF-8 Codepage Layout</a>
         */
        BYTE_WEIGHT,
        /**
         * Commonly well-know as alphabetical sorting, but reversed
         * @see Sorting#BYTE_WEIGHT
         * @see <a href="https://en.wikipedia.org/wiki/UTF-8#Codepage_layout">UTF-8 Codepage Layout</a>
         */
        BYTE_WEIGHT_REV;
    }
}
