package net.minecraftomega.config.annotations.fields;

import net.minecraftomega.config.annotations.SerializerMode;

import java.lang.annotation.*;

/**
 * Declares a field as a config Map field
 * Accepts key class and value class to parse and validate
 * <p>
 * String is valid only for key.
 * Valid types for Values are:
 * <code>String</code>,
 * <code>Int</code>,
 * <code>Float</code>,
 * <code>Double</code>,
 * <code>Short</code>,
 * <code>Byte</code>,
 * <code>Enums</code>,
 * <code>Array[String]</code>;</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigMapField {

    /**
     * Key class type, only accepts strings
     * @return class type
     */
    Class<?> key();

    /**
     * Value class type, accepts
     * <code>String</code>,
     * <code>Int</code>,
     * <code>Float</code>,
     * <code>Double</code>,
     * <code>Short</code>,
     * <code>Byte</code>,
     * <code>Enums</code>,
     * <code>Array[String]</code>;
     * @return class type
     */
    Class<?> value();

    /**
     * Sets how config will be serializable
     * {@link SerializerMode#FIELD}: Indicates Map will be serialized as a field inside a container
     * {@link SerializerMode#CONTAINER}: Indicates Map will be serialized as a container
     * <p>Avoid bloating the Map with useless config values filling it with registry keys and booleans, instead
     * use an array to store "disabled" or "enabled" values</p>
     * @return How Map should be serialized
     */
    SerializerMode mode() default SerializerMode.FIELD;
}