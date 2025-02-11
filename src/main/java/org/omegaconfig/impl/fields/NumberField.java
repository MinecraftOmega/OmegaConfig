package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public abstract class NumberField<T extends Number> extends BaseConfigField<T, Void> {
    private final boolean math;
    private final boolean strictMath;

    protected NumberField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, Field field, Object context) {
        super(name, group, comments, field, context);
        this.math = math;
        this.strictMath = strictMath;
    }

    protected NumberField(String name, ConfigGroup group, Set<String> comments, boolean math, boolean strictMath, T defaultValue) {
        super(name, group, comments, defaultValue);
        this.math = math;
        this.strictMath = strictMath;
    }

    public boolean math() {
        return math;
    }

    public boolean strictMath() {
        return strictMath;
    }
}
