package net.minecraftomega.config.annotations.ranged;

import java.lang.annotation.*;

/**
 * Range of a byte type field
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ByteRange {
    /**
     * Minimal allowed value
     * @return byte value
     */
    byte min() default Byte.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return byte value
     */
    byte max() default Byte.MAX_VALUE;
}
