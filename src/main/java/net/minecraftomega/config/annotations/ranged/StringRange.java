package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of a String type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StringRange {

    /**
     * Minimal length of a string, this can't be lower than 0
     * @return length as int value
     */
    int min() default 0;

    /**
     * Maximum allowed value, max lenght of a string is {@link Integer#MAX_VALUE}
     * @return length as int value
     */
    int max() default Integer.MAX_VALUE;
}
