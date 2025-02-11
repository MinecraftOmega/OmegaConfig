package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

public class IntField extends NumberField<Integer> implements IntSupplier {
    public final int min;
    public final int max;
    private int primitive;

    protected IntField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, int min, int max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public IntField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, int min, int max, Integer defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }

    @Override
    public void accept(Integer integer) {
        super.accept(primitive = integer);
    }

    @Override
    public int getAsInt() {
        return primitive;
    }
}