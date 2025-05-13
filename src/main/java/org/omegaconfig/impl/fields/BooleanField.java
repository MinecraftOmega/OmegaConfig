package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class BooleanField extends BaseConfigField<Boolean, Void> implements BooleanSupplier {
    private boolean primitive;

    public BooleanField(String name, ConfigGroup group, Set<String> comments, Field field, Object context) {
        super(name, group, comments, field, context);
    }

    public BooleanField(String name, ConfigGroup group, Set<String> comments, Boolean defaultValue) {
        super(name, group, comments, defaultValue);
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
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
