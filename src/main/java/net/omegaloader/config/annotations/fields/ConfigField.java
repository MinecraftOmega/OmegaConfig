package net.omegaloader.config.annotations.fields;

import net.omegaloader.config.builder.fields.BaseConfigField;

public @interface ConfigField {
    Class<?> value() default BaseConfigField.class;
    String name() default "";

    String i18n() default "";

}
