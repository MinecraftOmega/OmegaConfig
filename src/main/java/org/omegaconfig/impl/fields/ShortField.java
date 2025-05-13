package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.IntSupplier;

public final class ShortField extends BaseNumberField<Short> implements IntSupplier {
    public final short min;
    public final short max;
    private short primitive;

    public ShortField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, short min, short max, Field field, Object context) {
        super(name, group, comments, math, strictMath, field, context);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    public ShortField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, short min, short max, Short defaultValue) {
        super(name, group, comments, math, strictMath, defaultValue);
        this.primitive = this.defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public Class<Short> type() {
        return Short.class;
    }

    @Override
    public void accept(Short value) {
        super.accept(primitive = value);
    }

    @Override
    public int getAsInt() {
        return primitive;
    }

    public short getAsShort() {
        return primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Short.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override

    public String maxValueString() {
        return this.max == Short.MAX_VALUE ? null : String.valueOf(max);
    }
}
