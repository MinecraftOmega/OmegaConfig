package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.DoubleSupplier;

public final class FloatField extends BaseNumberField<Float> implements DoubleSupplier {
    public final float min;
    public final float max;
    private float primitive;

    public FloatField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, float min, float max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public FloatField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, float min, float max, Float defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Float> type() {
        return Float.class;
    }

    @Override
    public void validate() {
        if (this.primitive < this.min || this.primitive > this.max) {
            this.reset();
        }
    }

    @Override
    public void accept(Float value) {
        super.accept(primitive = value);
    }

    @Override
    public double getAsDouble() {
        return primitive;
    }

    public double getAsFloat() {
        return primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Float.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Float.MAX_VALUE ? null : String.valueOf(max);
    }
}
