package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * A comparable temporal field with optional inclusive {@code [from, to]} bounds; a value outside
 * the range resets to default, mirroring the numeric fields' min/max behavior.
 */
public final class TemporalField<T extends Comparable<T>> extends BaseConfigField<T, Void> {
    private final Class<T> type;
    public final T from; // NULLABLE
    public final T to;   // NULLABLE

    public TemporalField(String name, ConfigGroup group, Set<String> comments, Class<T> type, T from, T to, Field field, Object context, Control control, String suffix) {
        super(name, group, comments, field, context, control, suffix);
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public TemporalField(String name, ConfigGroup group, Set<String> comments, Class<T> type, T from, T to, T defaultValue, Control control, String suffix) {
        super(name, group, comments, defaultValue, control, suffix);
        this.type = type;
        this.from = from;
        this.to = to;
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public void validate() {
        T value = this.get();
        if (value == null) {
            return;
        }
        if (from != null && value.compareTo(from) < 0) {
            this.reset();
            return;
        }
        if (to != null && value.compareTo(to) > 0) {
            this.reset();
        }
    }
}
