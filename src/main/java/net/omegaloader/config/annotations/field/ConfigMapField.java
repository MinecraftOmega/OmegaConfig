package net.omegaloader.config.annotations.field;

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

    /**
     * By default (empty value), it will pick the field name on the class as the config field name
     * This gets affected by obfuscation, so be careful with what you hide.
     *
     * <p>Used to assi</p>
     *
     * @return config container id
     */
    String name() default "";

    /**
     * Field name as i18n
     *
     * <p>Optional, but useful for a proper name display on a i18n language</p>
     *
     * @return i18n key
     */
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