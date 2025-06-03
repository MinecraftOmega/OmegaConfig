package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.LongSupplier;

public final class LongField extends BaseNumberField<Long> implements LongSupplier {
    public final long min;
    public final long max;
    private long primitive;

    public LongField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, long min, long max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public LongField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, long min, long max, Long defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Long> type() {
        return Long.class;
    }

    @Override
    public void validate() {
        if (this.primitive < this.min || this.primitive > this.max) {
            this.reset(); // Reset to default if out of bounds
        }
    }

    @Override
    public void accept(Long value) {
        super.accept(primitive = value);
    }

    @Override
    public long getAsLong() {
        return primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Long.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Long.MAX_VALUE ? null : String.valueOf(max);
    }
}
