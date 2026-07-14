package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.ConfigSpec;
import me.srrapero720.waterconfig.Tools;
import me.srrapero720.waterconfig.api.Control;
import me.srrapero720.waterconfig.api.IConfigField;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

public abstract class BaseConfigField<T, S> implements IConfigField<T, S> {
    // METADATA
    private final String name;
    private final ConfigGroup group;
    private final Mode mode;
    private final Set<String> comments;
    private final Control control;
    private final String suffix;
    public final T defaultValue;

    // FIELD
    private final Object context;
    private final Field field;
    private String[] aliases = NO_ALIASES;
    private T value;

    protected BaseConfigField(String name, ConfigGroup group, Set<String> comments, Field field, Object context, Control control, String suffix) {
        // ONE-TIME OVERRIDE: REMOVES THE CALLER-STACK ACCESS CHECK FROM EVERY REFLECTIVE GET/SET
        field.setAccessible(true);
        this.name = name;
        this.group = group;
        this.comments = comments;
        this.defaultValue = Tools.valueFrom(field, context);
        this.mode = Mode.REFLECT;
        this.field = field;
        this.context = context;
        this.control = (control == null || control == Control.DEFAULT) ? Control.INPUT : control;
        this.suffix = suffix == null ? "" : suffix;
        this.group.append(this);
    }

    protected BaseConfigField(String name, ConfigGroup group, Set<String> comments, T defaultValue, Control control, String suffix) {
        if (defaultValue == null) {
            throw new IllegalArgumentException("Field '" + name + "' requires a non-null default value");
        }
        this.name = name;
        this.group = group;
        this.comments = comments;
        this.defaultValue = this.value = defaultValue;
        this.mode = Mode.NATIVE;
        this.field = null;
        this.context = null;
        this.control = (control == null || control == Control.DEFAULT) ? Control.INPUT : control;
        this.suffix = suffix == null ? "" : suffix;
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

    private String[] commentsArray;

    @Override
    public String[] comments() {
        // COMMENTS ARE FIXED AFTER BUILD: CACHE THE ARRAY INSTEAD OF COPYING PER SAVE
        if (this.commentsArray == null) {
            this.commentsArray = this.comments.toArray(new String[0]);
        }
        return this.commentsArray;
    }

    @Override
    public Control control() {
        return this.control;
    }

    @Override
    public String suffix() {
        return this.suffix;
    }

    @Override
    public String[] aliases() {
        return this.aliases;
    }

    /**
     * Internal: assigned once by the field builder.
     */
    public void aliases(String[] aliases) {
        this.aliases = (aliases == null) ? NO_ALIASES : aliases;
    }

    // BUILDS THE EXCEPTION THROWN BY SUBCLASSES WHEN A CONTROL DOES NOT FIT THE FIELD
    protected static IllegalArgumentException incoherentControl(String name, Control control) {
        return new IllegalArgumentException("Control '" + control + "' is not coherent for field '" + name + "'");
    }

    @Override
    public void reset() {
        this.set(this.defaultValue);
    }

    @Override
    public T get() {
        return switch (this.mode) {
            case REFLECT -> Tools.valueFrom(this.field, this.context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM not implemented yet");
        };
    }

    @Override
    public void accept(T t) {
        // EQUALITY GUARD: A NO-OP SET MUST NOT MARK THE SPEC DIRTY NOR REWRITE THE FILE
        if (Objects.equals(this.get(), t)) {
            return;
        }
        switch (this.mode) {
            case REFLECT -> Tools.setFieldValue(this.field, this.context, t);
            case NATIVE -> this.value = t;
            case ASM -> throw new UnsupportedOperationException("ASM not implemented yet");
        }
        this.group.markDirty(this);
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
