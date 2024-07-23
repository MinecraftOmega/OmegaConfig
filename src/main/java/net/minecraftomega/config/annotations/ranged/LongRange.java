package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of an integer type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LongRange {
    /**
     * Minimal allowed value
     * @return long value
     */
    long min() default Long.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return long value
     */
    long max() default Long.MAX_VALUE;
}
