package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;

public final class EnumField<T extends Enum<T>> extends BaseConfigField<T, T> implements Comparable<T> {

    public EnumField(String name, ConfigGroup group, Set<String> comments, Field field, Object context, Control control) {
        super(name, group, comments, field, context, coherent(name, control));
    }

    public EnumField(String name, ConfigGroup group, Set<String> comments, T defaultValue, Control control) {
        super(name, group, comments, defaultValue, coherent(name, control));
    }

    private static Control coherent(String name, Control control) {
        return switch (control) {
            case DEFAULT, DROPDOWN -> Control.DROPDOWN;
            case RADIO_BUTTON -> Control.RADIO_BUTTON;
            case LIST_SPINNER -> Control.LIST_SPINNER;
            case SEGMENTED -> Control.SEGMENTED;
            default -> throw incoherentControl(name, control);
        };
    }

    @Override
    public Class<T> subType() {
        return this.type();
    }

    @Override
    public void validate() {

    }

    @Override
    public Class<T> type() {
        return (Class<T>) this.defaultValue.getClass();
    }

    @Override
    public int compareTo(T o) {
        return this.get().compareTo(o);
    }
}
