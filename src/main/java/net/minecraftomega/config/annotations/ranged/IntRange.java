package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of an integer type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IntRange {
    /**
     * Minimal allowed value
     * @return integer value
     */
    int min() default Integer.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return integer value
     */
    int max() default Integer.MAX_VALUE;
}
