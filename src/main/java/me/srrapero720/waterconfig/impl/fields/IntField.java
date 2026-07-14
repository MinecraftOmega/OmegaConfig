package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.IntSupplier;

public final class IntField extends BaseNumberField<Integer> implements IntSupplier {
    public final int min;
    public final int max;
    private int primitive;

    public IntField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, int min, int max, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, math, strictMath, field, context, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    public IntField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, int min, int max, Integer defaultValue, Control control, String suffix) {
        super(name, group, comments, math, strictMath, defaultValue, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Integer value) {
        super.accept(this.primitive = value);
    }

    // RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE EXTERNAL MUTATIONS) AND VALIDATES;
    // CALLED BY ConfigSpec#refresh() SO A CONFIG MANAGER CAN RE-ESTABLISH A VALID, CACHED STATE
    @Override
    public void validate() {
        int value = this.get();
        if (value < this.min || value > this.max) {
            this.reset(); // TODO: MUST CLAMP OR HARD-FAIL ON STRICT
            return;
        }
        this.primitive = value;
    }

    @Override
    public int getAsInt() {
        return this.primitive;
    }

    @Override
    public String maxValueString() {
        return this.max == Integer.MAX_VALUE ? null : String.valueOf(max);
    }

    @Override
    public String minValueString() {
        return this.min == Integer.MIN_VALUE ? null : String.valueOf(min);
    }
}
