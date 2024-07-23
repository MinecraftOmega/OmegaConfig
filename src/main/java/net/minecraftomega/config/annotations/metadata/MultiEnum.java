package net.minecraftomega.config.annotations.metadata;

import java.lang.annotation.*;

/**
 * Makes the field to accept multiple Enum values at once.
 * <p>Sugar for serializer, has no impact in code. Requires to define
 * {@link net.minecraftomega.config.annotations.fields.ConfigField ConfigField} as Enum instead</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MultiEnum {
}
