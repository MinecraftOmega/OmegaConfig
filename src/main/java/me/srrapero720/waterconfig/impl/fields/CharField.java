package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;

public final class CharField extends BaseConfigField<Character, Void> {

    private char primitive;

    public CharField(String name, ConfigGroup group, Set<String> comments, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, control, suffix);
    }

    public CharField(String name, ConfigGroup group, Set<String> comments, Character defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, control, suffix);
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
