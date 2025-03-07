package org.omegaconfig.impl.fields;

import org.omegaconfig.ConfigGroup;
import org.omegaconfig.ConfigSpec;
import org.omegaconfig.Tools;
import org.omegaconfig.api.IConfigField;

import java.lang.reflect.Field;
import java.util.Set;

public abstract class BaseConfigField<T, S> implements IConfigField<T, S> {
    // METADATA
    private final String name;
    private final ConfigGroup group;
    private final Mode mode;
    private final Set<String> comments;
    public final T defaultValue;

    // FIELD
    private final Object context;
    private final Field field;
    private T value;

    protected BaseConfigField(String name, ConfigGroup group, Set<String> comments, Field field, Object context) {
        this.name = name;
        this.group = group;
        this.comments = comments;
        this.defaultValue = Tools.getFieldValue(field, context);
        this.mode = Mode.REFLECT;
        this.field = field;
        this.context = context;
        this.group.append(this);
    }

    protected BaseConfigField(String name, ConfigGroup group, Set<String> comments, T defaultValue) {
        this.name = name;
        this.group = group;
        this.comments = comments;
        this.defaultValue = this.value = defaultValue;
        this.mode = Mode.NATIVE;
        this.field = null;
        this.context = null;
        this.group.append(this);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ConfigGroup group() {
        return this.group;
    }

    @Override
    public ConfigSpec spec() {
        return (this.group instanceof ConfigSpec spec) ? spec : this.group.spec();
    }

    @Override
    public Class<S> subType() { // the default
        return null;
    }

    @Override
    public String[] comments() {
        return comments.toArray(new String[0]);
    }

    @Override
    public void reset() {
        this.set(this.defaultValue);
    }

    @Override
    public T get() {
        return switch (this.mode) {
            case REFLECT -> Tools.getFieldValue(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM not implemented yet");
        };
    }

    @Override
    public void accept(T t) {
        switch (this.mode) {
            case REFLECT -> Tools.setField(this.field, this.context, t);
            case NATIVE -> this.value = t;
            case ASM -> throw new UnsupportedOperationException("ASM not implemented yet");
        }
        this.group.markDirty(this);
    }

    @Override
    public boolean reflected() {
        if (this.mode != Mode.REFLECT) return true;
        T o = Tools.getFieldValue(field, context);
        return value.equals(o);
    }

    private enum Mode {
        /**
         * Uses Java reflection to set values
         */
        REFLECT,
        /**
         * Uses integrated field value in class
         */
        NATIVE,
        /**
         * Uses in-runtime generated methods via ASM
         */
        ASM,
    }
}
