package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.api.Control;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

public abstract class CollectionField<T, S> extends BaseConfigField<T, S> {
    public final boolean stringify;
    @Deprecated(forRemoval = true)
    public final boolean singleline;
    public final boolean allowEmpty;
    public final boolean unique;
    public final int limit;
    public final Class<? extends Predicate<S>> filter;
    public final Class<S> subType;

    protected CollectionField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<S>> filter, Field field, Object context, Class<S> subType, Control control, String suffix) {
        super(name, group, comments, field, context, coherent(name, control), suffix);
        this.stringify = stringify;
        this.singleline = singleline;
        this.allowEmpty = allowEmpty;
        this.unique = unique;
        this.limit = limit;
        this.filter = filter;
        this.subType = subType;
    }

    protected CollectionField(String name, ConfigGroup group, Set<String> comments, boolean stringify, boolean singleline, boolean allowEmpty, boolean unique, int limit, Class<? extends Predicate<S>> filter, T defaultValue, Class<S> subType, Control control, String suffix) {
        super(name, group, comments, defaultValue, coherent(name, control), suffix);
        this.stringify = stringify;
        this.singleline = singleline;
        this.allowEmpty = allowEmpty;
        this.unique = unique;
        this.limit = limit;
        this.filter = filter;
        this.subType = subType;
    }

    private static Control coherent(String name, Control control) {
        return switch (control) {
            case DEFAULT, LIST_EDITOR -> Control.LIST_EDITOR;
            case TAG_INPUT -> Control.TAG_INPUT;
            case CHECKBOX_LIST -> Control.CHECKBOX_LIST;
            default -> throw incoherentControl(name, control);
        };
    }

    // LAZILY INSTANTIATES THE CONFIGURED FILTER PREDICATE, CACHED AFTER THE FIRST USE
    private Predicate<S> filterInstance;
    protected Predicate<S> filterInstance() {
        if (this.filter == null) {
            return null;
        }
        if (this.filterInstance == null) {
            try {
                var constructor = this.filter.getDeclaredConstructor();
                constructor.setAccessible(true);
                this.filterInstance = constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate filter '" + this.filter.getName() + "' for field '" + this.name() + "'", e);
            }
        }
        return this.filterInstance;
    }

    @Override
    public Class<S> subType() {
        return this.subType;
    }

    public abstract CollectionField<T, S> setArray(Object[] array);

    public abstract CollectionField<T, S> add(S element);

    public abstract CollectionField<T, S> remove(S element);

    public abstract S remove(int index);

    public abstract boolean contains(S element);

    public abstract int size();

    public abstract CollectionField<T, S> clear();
}
