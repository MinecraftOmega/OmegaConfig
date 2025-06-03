package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public final class CharField extends BaseConfigField<Character, Void> {

    private char primitive;

    public CharField(String name, ConfigGroup group, Set<String> comments, Field field, Object context) {
        super(name, group, comments, field, context);
    }

    public CharField(String name, ConfigGroup group, Set<String> comments, Character defaultValue) {
        super(name, group, comments, defaultValue);
    }

    @Override
    public Class<Character> type() {
        return Character.class;
    }

    @Override
    public void validate() {
        // No validation needed for char
    }

    @Override
    public void accept(Character character) {
        super.accept(primitive = character);
    }

    public char getAsChar() {
        return primitive;
    }
}
