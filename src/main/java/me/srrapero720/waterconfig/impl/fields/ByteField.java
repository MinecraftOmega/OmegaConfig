package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.IntSupplier;

public final class ByteField extends BaseNumberField<Byte> implements IntSupplier {
    public final byte min;
    public final byte max;
    private byte primitive;

    public ByteField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, byte min, byte max, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, math, strictMath, field, context, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    public ByteField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, byte min, byte max, Byte defaultValue, Control control, String suffix) {
        super(name, group, comments, math, strictMath, defaultValue, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Byte> type() {
        return Byte.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Byte value) {
        super.accept(this.primitive = value);
    }

    // RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE EXTERNAL MUTATIONS) AND VALIDATES
    @Override
    public void validate() {
        byte value = this.get();
        if (value < this.min || value > this.max) {
            this.reset(); // RESET TO DEFAULT IF OUT OF BOUNDS
            return;
        }
        this.primitive = value;
    }

    @Override
    public int getAsInt() {
        return this.primitive;
    }

    public byte getAsByte() {
        return this.primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Byte.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Byte.MAX_VALUE ? null : String.valueOf(max);
    }
}
