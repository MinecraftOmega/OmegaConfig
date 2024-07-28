package net.minecraftomega.config.annotations.fields;

/**
 * Defines the base class of a config class
 */
public @interface Config {
    /**
     * Config identifier
     * <p>Nested parents are taken as objects</p>
     * @return config container id
     */
    String value();

    String i18n() default "";
}