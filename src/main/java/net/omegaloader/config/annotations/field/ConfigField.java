package net.omegaloader.config.annotations.field;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigField {

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

}
