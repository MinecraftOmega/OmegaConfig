package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.IntSupplier;

public final class IntField extends BaseNumberField<Integer> implements IntSupplier {
    public final int min;
    public final int max;
    private int primitive;

    public IntField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, int min, int max, Field field, Object context) {
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
    public void validate() {
        if (this.primitive < this.min || this.primitive > this.max) {
            this.reset(); // TODO: this must clamp, or throw on strict
        }
    }

    @Override
    public void accept(Integer integer) {
        super.accept(primitive = integer);
    }

    @Override
    public int getAsInt() {
        return primitive;
    }

    @Override
    public String maxValueString() {
        return this.max == Integer.MAX_VALUE ? null : String.valueOf(max);
    }

    @Override
    public String minValueString() {
        return this.min == Integer.MIN_VALUE ? null : String.valueOf(min);
    }
}