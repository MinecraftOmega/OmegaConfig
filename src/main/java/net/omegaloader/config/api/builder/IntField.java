package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class IntField extends NumberField<Integer> {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private int value;

    public IntField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public IntField(String name, GroupField parent, int defaultValue) {
        super(name, parent, defaultValue);
    }

    public int getAsInt() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(int value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Integer value) {
        this.value = value;
        super.set(value);
    }

    public IntField min(int value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public IntField max(int value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public int min() {
        this.assertLocked();

        return min;
    }

    public int max() {
        this.assertLocked();

        return max;
    }
}
