package net.omegaloader.config.annotations.field;

import java.lang.annotation.*;

/**
 * Defines a field as a configuration field.
 *
 * <p>Field must be public and non-final</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigField {

    /**
     * By default, it will pick the field name on the class as the config field name
     * This gets affected by obfuscation, so be careful with what you hide.
     *
     * <p>Used to assist obfuscation tools</p>
     *
     * @return config container id, empty by default
     */
    String value() default "";
}
