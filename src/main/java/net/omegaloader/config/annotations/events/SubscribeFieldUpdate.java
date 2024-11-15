package net.omegaloader.config.annotations.events;

import java.lang.annotation.*;

/**
 * Annotated method should have 2 argument types
 * {@link }
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeFieldUpdate {



    Priority value() default Priority.NORMAL;

    enum Priority {
        HIGH,
        NORMAL,
        LOW
    }
}
