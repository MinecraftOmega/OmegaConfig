package net.omegaloader.config.api.builder;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.reflect.Field;
import java.util.Optional;

public class EnumField<T extends Enum<T>> extends BaseConfigField<T> implements Comparable<T>, Constable {

    public EnumField(String name, GroupField parent, Object context, Field field) {
        super(name, parent, context, field);
    }

    public EnumField(String name, GroupField parent, T defaultValue) {
        super(name, parent, defaultValue);
    }

    public int ordinal() {
        return this.get().ordinal();
    }

    @Override
    public int compareTo(T o) {
        return this.get().compareTo(o);
    }

    @Override
    public Optional<? extends ConstantDesc> describeConstable() {
        return this.get().describeConstable();
    }
}
