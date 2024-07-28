package net.omegaloader.config.annotations.field;

public @interface ConfigField {
    String name() default "";

    String i18n() default "";

}
