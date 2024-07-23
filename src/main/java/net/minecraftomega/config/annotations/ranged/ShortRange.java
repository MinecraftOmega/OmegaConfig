package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of a short type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ShortRange {

    /**
     * Minimal allowed value
     * @return short value
     */
    short min() default Short.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return short value
     */
    short max() default Short.MAX_VALUE;
}
