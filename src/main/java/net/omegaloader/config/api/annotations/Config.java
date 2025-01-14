package net.omegaloader.config.api.annotations;

import net.omegaloader.config.formats.IConfigFormat;

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
     * <p>Nested parents are taken as objects and identify is automatically concatenated with parent identifier</p>
     *
     * <p>Example: parent is id "myfancyconfig" and nested config is "toggles"
     * the "toggles" id will be "myfanacyconfig.toggles".
     * </p>
     *
     * Some serializers might not be able to support at all nested parents
     */
    String value();

    /**
     * Suffix is attached to the config identifier, is only used by the serializer with no special .
     *
     * <p>Value is ignored on nested config parents</p>
     *
     * @return Empty by default
     */
    String suffix() default "";

    /**
     * backup is always triggered after refresh the config spec and got spotted some missing fields and/or exists
     * unassigned fields by the config spec on the file
     *
     * <p>Value is ignored on nested config parents</p>
     *
     * @return Max count of backups.
     * <code>2</code> by default.
     *
     * <p>Backup names remains as the spec name with a suffix. For example: <code>mymodid-configforwhat.json5_backup1</code></p>
     */
    int backups() default 2;

    /**
     * Config file format, format determines the serializer and extension
     *
     * <p>Value is ignored on sub-config classes or nested config classes</p>
     *
     * @return {@link IConfigFormat#PROPERTIES} by default
     */
    String format() default "properties";
}