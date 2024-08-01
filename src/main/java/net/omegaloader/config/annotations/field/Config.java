package net.omegaloader.config.annotations.field;

import java.lang.annotation.*;

/**
 * Defines the base class of a config class
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
    /**
     * Config identifier
     *
     * <p>Nested parents are taken as objects</p>
     *
     * @return config container id
     */
    String value();

    /**
     * Config name as i18n
     *
     * <p>Optional, but useful for a proper name display on a i18n language</p>
     *
     * @return i18n key
     */
    String i18n() default "";

    /**
     * backup is always triggered after refresh the config spec and got spotted some missing fields and/or exists
     * unassigned fields by the config spec on the file
     *
     * <p>This value is ignored on nested config parents.</p>
     *
     * @return Max count of backups.
     * Files are saved named like <code>mymodid-configforwhat.json5_backup1</code>
     */
    int backups() default 2;
}