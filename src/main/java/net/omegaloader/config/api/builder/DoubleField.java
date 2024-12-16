package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class DoubleField extends NumberField<Double> {
    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;
    private double value;

    public DoubleField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public DoubleField(String name, GroupField parent, double defaultValue) {
        super(name, parent, defaultValue);
    }

    public double getAsDouble() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(double value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Double value) {
        this.value = value;
        super.set(value);
    }

    public DoubleField min(double value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public DoubleField max(double value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public double min() {
        this.assertLocked();

        return min;
    }

    public double max() {
        this.assertLocked();

        return max;
    }
}

