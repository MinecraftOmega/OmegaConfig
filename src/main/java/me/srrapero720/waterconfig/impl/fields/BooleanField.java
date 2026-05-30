package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class BooleanField extends BaseConfigField<Boolean, Void> implements BooleanSupplier {
    private boolean primitive;

    public BooleanField(String name, ConfigGroup group, Set<String> comments, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, coherent(name, control), suffix);
        this.primitive = this.defaultValue;
    }

    public BooleanField(String name, ConfigGroup group, Set<String> comments, Boolean defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, coherent(name, control), suffix);
        this.primitive = this.defaultValue;
    }

    private static Control coherent(String name, Control control) {
        return switch (control) {
            case DEFAULT, SWITCH -> Control.SWITCH;
            case CHECKBOX -> Control.CHECKBOX;
            default -> throw incoherentControl(name, control);
        };
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public void validate() {
        // No validation needed for boolean fields
    }

    @Override
    public void accept(Boolean aBoolean) {
        super.accept(primitive = aBoolean);
    }

    @Override
    public boolean getAsBoolean() {
        return primitive;
    }
}
