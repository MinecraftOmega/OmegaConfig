package net.omegaloader.config.api.builder;

import java.lang.reflect.Field;

public abstract class NumberField<T extends Number> extends BaseConfigField<T> {
    private boolean math = false;
    private boolean strictMath = false;

    protected NumberField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    protected NumberField(String name, GroupField parent, T defaultValue) {
        super(name, parent, defaultValue);
    }

    public BaseConfigField<T> math(boolean value) {
        this.assertUnlocked();

        this.math = value;
        return this;
    }

    public boolean math() {
        this.assertLocked();

        return this.math;
    }

    public BaseConfigField<T> strictMath(boolean value) {
        this.assertUnlocked();

        this.strictMath = value;
        return this;
    }

    public boolean strictMath() {
        this.assertLocked();

        return this.strictMath;
    }
}
