package net.omegaloader.config.annotations.fields;

public @interface ConfigField {
    String name() default "";

    String i18n() default "";

}
