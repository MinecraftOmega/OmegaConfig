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

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Boolean value) {
        super.accept(this.primitive = value);
    }

    // NO BOUNDS TO CHECK; RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE MUTATIONS)
    @Override
    public void validate() {
        this.primitive = this.get();
    }

    @Override
    public boolean getAsBoolean() {
        return this.primitive;
    }
}
