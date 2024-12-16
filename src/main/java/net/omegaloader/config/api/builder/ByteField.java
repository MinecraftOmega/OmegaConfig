package net.omegaloader.config.api.builder;

import net.omegaloader.config.Util;

import java.lang.reflect.Field;

public class ByteField extends NumberField<Byte> /* implements ByteSupplier */ {
    private byte min = Byte.MIN_VALUE;
    private byte max = Byte.MAX_VALUE;
    private byte value;

    public ByteField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public ByteField(String name, GroupField parent, byte defaultValue) {
        super(name, parent, defaultValue);
    }

    public byte getAsByte() {
        return switch (getMode()) {
            case REFLECTION -> Util.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new Error("What its happening");
        };
    }

    public void set(byte value) {
        this.value = value;
        super.set(value);
    }

    @Override
    public void set(Byte value) {
        this.value = value;
        super.set(value);
    }

    public ByteField min(byte value) {
        this.assertUnlocked();

        this.min = value;
        return this;
    }

    public ByteField max(byte value) {
        this.assertUnlocked();

        this.max = value;
        return this;
    }

    public byte min() {
        this.assertLocked();

        return min;
    }

    public byte max() {
        this.assertLocked();

        return max;
    }
}