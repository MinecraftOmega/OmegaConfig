package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Set;

public class StringField extends BaseConfigField<String, Void> {
    public StringField(String name, ConfigGroup group, Set<String> comments, Field field, Object context) {
        super(name, group, comments, field, context);
    }

    public StringField(String name, ConfigGroup group, Set<String> comments, String defaultValue) {
        super(name, group, comments, defaultValue);
    }

    @Override
    public Class<String> type() {
        return String.class;
    }
}
