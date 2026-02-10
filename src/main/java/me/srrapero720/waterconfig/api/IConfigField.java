package me.srrapero720.waterconfig.api;

import me.srrapero720.waterconfig.ConfigGroup;
import me.srrapero720.waterconfig.ConfigSpec;

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
    default String id() {
        final ConfigGroup group = this.group();
        final String groupId = group != null ? group.id() : "";
        final String name = this.name();
        final String parentName = group != null ? (groupId + (groupId.endsWith(":") ? "" : ".")) : ""; // spec:parent.config or spec:config
        return parentName + name + (group != null ? "" : ":");
    }

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
     * Provides the groups count above this field
     */
    default int groupCount() {
        int count = 0;
        ConfigGroup g = this.group();
        while (g != null) {
            count++;
            g = g.group();
        }
        return count;
    }

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
     * Validates the field value, resets to default value if it doesn't match the validation
     */
    void validate();


    /**
     * Internal Usage
     * @param object
     */
    default void set0(Object object) {
        if (object == null) {
            this.reset();
        } else {
            this.accept((T) object);
            this.validate();
        }
    }

    default void set(T value) {
        this.accept(value);
    }
}
