package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

public class DoubleField extends NumberField<Double> implements DoubleSupplier {
    public final double min;
    public final double max;
    private double primitive;

    protected DoubleField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, double min, double max, Field field, Object context) {
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
    public void accept(Double value) {
        super.accept(primitive = value);
    }

    @Override
    public double getAsDouble() {
        return primitive;
    }
}
