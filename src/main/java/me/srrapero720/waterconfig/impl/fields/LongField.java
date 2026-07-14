package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.LongSupplier;

public final class LongField extends BaseNumberField<Long> implements LongSupplier {
    public final long min;
    public final long max;
    private long primitive;

    public LongField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, long min, long max, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, math, strictMath, field, context, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    public LongField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, long min, long max, Long defaultValue, Control control, String suffix) {
        super(name, group, comments, math, strictMath, defaultValue, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Long> type() {
        return Long.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Long value) {
        super.accept(this.primitive = value);
    }

    // RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE EXTERNAL MUTATIONS) AND VALIDATES
    @Override
    public void validate() {
        long value = this.get();
        if (value < this.min || value > this.max) {
            this.reset(); // RESET TO DEFAULT IF OUT OF BOUNDS
            return;
        }
        this.primitive = value;
    }

    @Override
    public long getAsLong() {
        return this.primitive;
    }

    @Override
    public String minValueString() {
        return this.min == Long.MIN_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Long.MAX_VALUE ? null : String.valueOf(max);
    }
}
