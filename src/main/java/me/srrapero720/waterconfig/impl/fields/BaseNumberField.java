package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;

public sealed abstract class BaseNumberField<T extends Number> extends BaseConfigField<T, Void> permits ByteField, DoubleField, FloatField, IntField, LongField, ShortField {
    private final boolean math;
    private final boolean strictMath;

    protected BaseNumberField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, coherent(name, control), suffix);
        this.math = math;
        this.strictMath = strictMath;
    }

    protected BaseNumberField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, T defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, coherent(name, control), suffix);
        this.math = math;
        this.strictMath = strictMath;
    }

    private static Control coherent(String name, Control control) {
        return switch (control) {
            case DEFAULT, NUMBER_SPINNER -> Control.NUMBER_SPINNER;
            case SEEKBAR -> Control.SEEKBAR;
            case KNOB -> Control.KNOB;
            case RANGE_SLIDER -> Control.RANGE_SLIDER;
            default -> throw incoherentControl(name, control);
        };
    }

    public boolean math() {
        return math;
    }

    public boolean strictMath() {
        return strictMath;
    }

    /**
     * Provides a stringified version of the minimum value accepted of this field.
     * @return The minimum value accepted as a string. When the minimum value is the minimum value of the type, null is returned.
     */
    public abstract String minValueString();

    /**
     * Provides a stringified version of the maximum value accepted of this field.
     * @return The maximum value accepted as a string. When the maximum value is the maximum value of the type, null is returned.
     */
    public abstract String maxValueString();
}