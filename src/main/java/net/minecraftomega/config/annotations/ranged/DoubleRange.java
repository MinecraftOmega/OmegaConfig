package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of a double type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DoubleRange {

    /**
     * Minimal allowed value
     * @return double value
     */
    double min() default Double.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return double value
     */
    double max() default Double.MAX_VALUE;
}
