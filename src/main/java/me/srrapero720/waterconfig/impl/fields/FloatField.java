package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.DoubleSupplier;

public final class FloatField extends BaseNumberField<Float> implements DoubleSupplier {
    public final float min;
    public final float max;
    private float primitive;

    public FloatField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, float min, float max, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, math, strictMath, field, context, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    public FloatField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, float min, float max, Float defaultValue, Control control, String suffix) {
        super(name, group, comments, math, strictMath, defaultValue, control, suffix);
        this.min = min;
        this.max = max;
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Float> type() {
        return Float.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Float value) {
        super.accept(this.primitive = value);
    }

    // RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE EXTERNAL MUTATIONS) AND VALIDATES
    @Override
    public void validate() {
        float value = this.get();
        if (value < this.min || value > this.max) {
            this.reset();
            return;
        }
        this.primitive = value;
    }

    @Override
    public double getAsDouble() {
        return this.primitive;
    }

    public float getAsFloat() {
        return this.primitive;
    }

    @Override
    public String minValueString() {
        return this.min == -Float.MAX_VALUE ? null : String.valueOf(min);
    }

    @Override
    public String maxValueString() {
        return this.max == Float.MAX_VALUE ? null : String.valueOf(max);
    }
}
