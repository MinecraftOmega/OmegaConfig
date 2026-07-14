package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;

public final class CharField extends BaseConfigField<Character, Void> {
    private char primitive;

    public CharField(String name, ConfigGroup group, Set<String> comments, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, control, suffix);
        this.primitive = this.defaultValue;
    }

    public CharField(String name, ConfigGroup group, Set<String> comments, Character defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, control, suffix);
        this.primitive = this.defaultValue;
    }

    @Override
    public Class<Character> type() {
        return Character.class;
    }

    // CACHES THE VALUE ON EVERY WRITE THROUGH THE FIELD API
    @Override
    public void accept(Character value) {
        super.accept(this.primitive = value);
    }

    // NO BOUNDS TO CHECK; RE-SYNCS THE CACHE FROM THE FIELD (CATCHING REFLECT-MODE MUTATIONS)
    @Override
    public void validate() {
        this.primitive = this.get();
    }

    public char getAsChar() {
        return this.primitive;
    }
}
