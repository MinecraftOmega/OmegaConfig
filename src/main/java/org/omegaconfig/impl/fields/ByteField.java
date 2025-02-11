package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

public class ByteField extends NumberField<Byte> implements IntSupplier {
    public final byte min;
    public final byte max;
    private byte primitive;

    protected ByteField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, byte min, byte max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public ByteField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, byte min, byte max, Byte defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Byte> type() {
        return Byte.class;
    }

    @Override
    public void accept(Byte value) {
        super.accept(primitive = value);
    }

    @Override
    public int getAsInt() {
        return primitive;
    }

    public byte getAsByte() {
        return primitive;
    }
}