package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class LongField extends NumberField<Long> {
    private long min = Long.MIN_VALUE;
    private long max = Long.MAX_VALUE;
    private long value;

    public LongField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public LongField(String name, GroupField parent, long defaultValue) {
        super(name, parent, defaultValue);
    }

    public long getAsLong() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(long value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Long value) {
        this.value = value;
        super.set(value);
    }

    public LongField min(long value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public LongField max(long value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public long min() {
        this.assertLocked();

        return min;
    }

    public long max() {
        this.assertLocked();

        return max;
    }
}
