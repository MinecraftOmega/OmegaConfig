package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class FloatField extends NumberField<Float> {
    private float min = Float.MIN_VALUE;
    private float max = Float.MAX_VALUE;
    private float value;

    public FloatField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public FloatField(String name, GroupField parent, float defaultValue) {
        super(name, parent, defaultValue);
    }

    public float getAsFloat() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(float value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Float value) {
        this.value = value;
        super.set(value);
    }

    public FloatField min(float value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public FloatField max(float value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public float min() {
        this.assertLocked();

        return min;
    }

    public float max() {
        this.assertLocked();

        return max;
    }
}