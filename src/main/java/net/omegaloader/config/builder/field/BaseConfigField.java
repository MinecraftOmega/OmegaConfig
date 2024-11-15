package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;
import net.omegaloader.config.annotations.metadata.Comment;
import net.omegaloader.config.builder.ConfigSpec;
import net.omegaloader.config.core.bridges.Field2Line;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseConfigField<T> implements Comparable<T>, Supplier<T>, Consumer<T> {
    // FIELD METADATA
    public final String name; // ej. mymod.group_a.group_b
    public final Object context; // Config holder instance, null when is on static context.
    public final boolean isVolatile;

    public final GroupField parent; // parent tells to parent tell to parent...

    // FIELD METADATA POST-EVALUATED
    public Iterable<Comment> comments = new ArrayList<>(); // attached comments, this got sailed after config evaluation

    // INTERNAL FIELD MANAGMENT
    private final Field2Line line = null; // FIXME: field2line is not ready, should be a refresher

    // FIELD VALUE
    public final Field field;
    public final T defaultValue;
    private final AtomicReference<T> value;

    public BaseConfigField(String name, Object ctx, Field field, boolean isVoltile) {
        this.name = name;
        this.context = ctx;
        this.field = field;
        this.defaultValue = Util.getFieldValue(field, ctx);
        this.value = null;
        this.isVolatile = isVoltile;
    }

    public BaseConfigField(String name, Object ctx, T defaultValue, boolean isVoltile) {
        this.name = name;
        this.context = ctx;
        this.field = null;
        this.defaultValue = defaultValue;
        this.value = new AtomicReference<>(defaultValue);
        this.isVolatile = isVoltile;
    }

    void setField(T value) {
        if (this.field != null) {
            Class<?> c = value.getClass();
            if (c.isAssignableFrom(Number.class)) {
                c = Util.swapNumberClass((Class<? extends Number>) c);
            }
            if (c == byte.class) { // reflect did this check internally, here we skip it and have even more control on data types
                Util.setField(this.field, this.context, (byte) value);
            } else if (c == short.class) {
                Util.setField(this.field, this.context, (short) value);
            } else if (c == int.class) {
                Util.setField(this.field, this.context, (int) value);
            } else if (c == long.class) {
                Util.setField(this.field, this.context, (long) value);
            } else if (c == float.class) {
                Util.setField(this.field, this.context, (float) value);
            } else if (c == double.class) {
                Util.setField(this.field, this.context, (double) value);
            } else if (c == char.class) {
                Util.setField(this.field, this.context, (char) value);
            } else {
                Util.setField(this.field, this.context, value);
            }
        }
    }

    public void reset() {
        this.set(this.defaultValue);
    }

    public BaseConfigField<T> addComments(String[] comments, String... i18n) {
        this.addComment(new DC(comments, i18n));
        return this;
    }

    public BaseConfigField<T> addComment(Comment comment) {
        ((List<Comment>) this.comments).add(comment); // FIXME: assuming we didn't evaluate config yet
        return this;
    }

    public BaseConfigField<T> setComments(Comment[] comments) {
        this.comments = Arrays.asList(comments);
        return this;
    }

    @Override
    public T get() {
        return this.value;
    }

    public void set(T value) {
        this.accept(value);
    }

    public void refresh() {
        if (this.isGroup()) {
            this.getFields().forEach((s, baseConfigField) -> baseConfigField.refresh());
        } else {
            line.refresh();
        }
    }

    public boolean isGroup() {
        return false;
    }

    public Map<String, BaseConfigField<?>> getFields() {
        throw new UnsupportedOperationException(isGroup()
                ? "Field is a group but it doesn't override the default method"
                : "Field is not a group"
        );
    }

    public static void assertFieldIsUnregistered(BaseConfigField<?> field) {
        if (field.line != null) throw new IllegalStateException("Field is already registered, no changes are accepted");
    }

    private record DC(String[] value, String[] i18n) implements Comment {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Comment.class;
        }
    }
}
