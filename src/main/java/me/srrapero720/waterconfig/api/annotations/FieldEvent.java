package me.srrapero720.waterconfig.api.annotations;

import java.lang.annotation.*;

/**
 * Annotated method should have 2 argument types
 * {@link }
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldEvent {



    Priority value() default Priority.NORMAL;

    enum Priority {
        HIGH,
        NORMAL,
        LOW
    }
}
