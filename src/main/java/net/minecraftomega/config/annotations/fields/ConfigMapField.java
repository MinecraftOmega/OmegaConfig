package net.minecraftomega.config.annotations.fields;

import java.lang.annotation.*;

/**
 * Declares a field as a config Map field
 * Accepts key class and value class to parse and validate
 * @see ConfigMapField#value()
 * @see ConfigField
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigMapField {

    String i18n() default "";

    /**
     * Key class type, only accepts strings
     * @return class type
     */
    Class<?> key() default String.class;

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
}