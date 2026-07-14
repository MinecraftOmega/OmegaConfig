package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.IntSupplier;

public final class ShortField extends BaseNumberField<Short> implements IntSupplier {
    public final short min;
    public final short max;
    private short primitive;

    public ShortField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, short min, short max, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, math, strictMath, field, context, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    public ShortField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, short min, short max, Short defaultValue, Control control, String suffix) {
        super(name, group, comments, math, strictMath, defaultValue, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Short> type() {
        return Short.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Short value) {
        super.accept(this.primitive = value);
    }

    // RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE EXTERNAL MUTATIONS) AND VALIDATES
    @Override
    public void validate() {
        short value = this.get();
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

    public short getAsShort() {
        return this.primitive;
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
