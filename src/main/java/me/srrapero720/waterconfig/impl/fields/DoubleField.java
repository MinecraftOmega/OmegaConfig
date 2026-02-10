package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.DoubleSupplier;

public final class DoubleField extends BaseNumberField<Double> implements DoubleSupplier {
    public final double min;
    public final double max;
    private double primitive;

    public DoubleField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, double min, double max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public DoubleField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, double min, double max, Double defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Double> type() {
        return Double.class;
    }

    @Override
    public void validate() {
        if (this.primitive < this.min || this.primitive > this.max) {
            this.reset();
        }
    }

    @Override
    public void accept(Double value) {
        super.accept(primitive = value);
    }

    @Override
    public double getAsDouble() {
        return primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Double.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Double.MAX_VALUE ? null : String.valueOf(max);
    }
}
