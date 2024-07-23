package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of a float type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FloatRange {
    /**
     * Minimal allowed value
     * @return float value
     */
    float min() default Float.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return float value
     */
    float max() default Float.MAX_VALUE;
}
