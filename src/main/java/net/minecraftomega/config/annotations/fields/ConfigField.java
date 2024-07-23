package net.minecraftomega.config.annotations.fields;

import net.minecraftomega.config.builder.fields.BaseConfigField;

public @interface ConfigField {
    Class<?> value() default BaseConfigField.class;
    String name() default "";
}
