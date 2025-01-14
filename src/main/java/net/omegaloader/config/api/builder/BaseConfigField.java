package net.omegaloader.config.api.builder;

import net.omegaloader.config.ConfigSpec;
import net.omegaloader.config.Util;
import net.omegaloader.config.api.annotations.FieldComment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseConfigField<T> implements Supplier<T>, Consumer<T> {
    // METADATA
    private final String name;
    private final GroupField parent;
    private final Mode mode;

    // REFLECTION
    protected final Object context;
    protected final Field field;

    // FIELD VALUE
    protected final T defaultValue;
    private T value;

    // FIELD METADATA
    private List<String> comments = new ArrayList<>();

    protected BaseConfigField(String name, GroupField parent) {
        this.name = name;
        this.field = null;
        this.context = null;
        this.parent = parent;
        this.defaultValue = null;
        this.mode = Mode.GROUP;
    }

    protected BaseConfigField(String name, GroupField parent, Object context, Field field) {
        this.name = name;
        this.field = field;
        this.context = context;
        this.parent = parent;
        this.defaultValue = Util.getFieldValue(field, context);
        this.mode = Mode.REFLECTION;
    }

    protected BaseConfigField(String name, GroupField parent, T defaultValue) {
        this.name = name;
        this.field = null;
        this.context = null;
        this.parent = parent;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.mode = Mode.NATIVE;
    }

    public String id() {
        String parentName = this.parent != null ? (this.parent.id() + ".") : ""; // spec:parent.config or spec:config
        return parentName + name + (this.parent != null ? "" : ":");
    }

    public String name() {
        return name;
    }

    public ConfigSpec getSpec() {
        return (getParent() instanceof ConfigSpec spec) ? spec : parent.getSpec();
    }

    public GroupField getParent() {
        if (parent == null) return (GroupField) this;
        else return this.parent;
    }

    @Override
    public void accept(T value) {
        this.set(value);
    }

    public void set(T value) {
        switch (mode) {
            case REFLECTION -> Util.setFieldAsType(field, context, value);
            case NATIVE -> this.value = value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new IllegalArgumentException("TOo many modes!");
        }
    }

    @Override
    public T get() {
        return switch (mode) {
            case REFLECTION -> Util.getFieldValue(field, context);
            case NATIVE -> this.value;
            case ASM -> throw new UnsupportedOperationException("ASM mode");
            case GROUP -> throw new UnsupportedOperationException("Groups are not real fields");
            default -> throw new IllegalArgumentException("TOo many modes!");
        };
    }

    public Mode getMode() {
        return mode;
    }

    public void reset() {
        this.set(this.defaultValue);
    }

    public BaseConfigField<T> comments(FieldComment... comments) {
        for (FieldComment c: comments)
            this.comments(c.value());

        return this;
    }

    public BaseConfigField<T> comments(String... comments) {
        this.assertUnlocked();

        if (this.comments.isEmpty()) this.comments = Arrays.asList(comments);
        else this.comments.addAll(Arrays.asList(comments));

        return this;
    }

    public String[] comments() {
        this.assertLocked();

        return this.comments.toArray(new String[0]);
    }

    public Object context() {
        return context;
    }

    public void assertUnlocked() {
        if (this.getSpec().isLocked())
            throw new IllegalStateException("Configuration spec '" + this.getSpec().name() + "' is already locked");
    }

    public void assertLocked() {
        if (!this.getSpec().isLocked())
            throw new IllegalStateException("Configuration spec '" + this.getSpec().name() + "' is not locked yet");
    }

    public enum Mode {
        /**
         * Retrieves the field from a class using Reflection's API
         */
        REFLECTION,
        /**
         * Visits all fields via ASM
         */
        ASM,
        /**
         * Stores the value in the {@link BaseConfigField} instance
         */
        NATIVE,

        /**
         * None
         */
        GROUP;
    }
}
