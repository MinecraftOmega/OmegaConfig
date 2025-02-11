package org.omegaconfig.api;

import org.omegaconfig.ConfigGroup;
import org.omegaconfig.ConfigSpec;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IConfigField<T, S> extends Consumer<T>, Supplier<T> {

    /**
     * returns the full field qualifier of this field, containing the spec id,
     * group field and field id.
     *
     * <p>Example:</p>
     * <code>exampleid:group_one.group_two.field_id</code>
     */
    String id();

    /**
     * Returns the field name, commonly the local field qualifier
     */
    String name();

    /**
     * Provides the config spec.
     */
    ConfigSpec spec();

    /**
     * Provides the group of the field, by default the spec is the main group
     */
    ConfigGroup group();

    /**
     * Provides field type
     */
    Class<T> type();

    /**
     * Provides field type subtype. Used for List, Enums or any class with a single-generic parameter.
     * Can be null
     */
    Class<S> subType();

    /**
     * Provides the field comments
     */
    String[] comments();

    /**
     * Sets config to default value
     */
    void reset();

    /**
     * On reflective mode, checks if the field value matches with the config field value
     * @return true if matches or when is not in reflective mode, false otherwise
     */
    boolean reflected();

    /**
     * Internal Usage
     * @param object
     */
    default void set0(Object object) {
        this.accept((T) object);
    }

    default void set(T value) {
        this.accept(value);
    }
}
