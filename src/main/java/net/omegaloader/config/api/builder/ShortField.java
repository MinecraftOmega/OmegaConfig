package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class ShortField extends NumberField<Short> {
    private short min = Short.MIN_VALUE;
    private short max = Short.MAX_VALUE;
    private short value;

    public ShortField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public ShortField(String name, GroupField parent, short defaultValue) {
        super(name, parent, defaultValue);
    }

    public short getAsShort() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(short value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Short value) {
        this.value = value;
        super.set(value);
    }

    public ShortField min(short value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public ShortField max(short value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public short min() {
        this.assertLocked();

        return min;
    }

    public short max() {
        this.assertLocked();

        return max;
    }
}
