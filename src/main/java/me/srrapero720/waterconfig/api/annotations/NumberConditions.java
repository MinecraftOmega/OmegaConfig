package me.srrapero720.waterconfig.api.annotations;

import java.lang.annotation.*;

/**
 * Specify number conditions for the serializer and deserializer
 * Valid only on number field types.
 *
 * <p>Some configurations are type-specific just for floating precision</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NumberConditions {
    /**
     * Accept basic math operations such add, subtract, multiply, divide and square.
     * This is an outsmarting thing of serializer specs,
     * but helps users to automatically do math operation on advanced fields.
     * @return acceptance of math operations on number fields, <code>false</code> by default
     */
    boolean math() default false;

    /**
     * Normal behavior is resolve the math operation, when the result is a float and field is an int; then it got cast back to int.
     * When the result exceeds the field max or min limits, then it got capped to max value
     * In case math operation has a wrong syntax, value got corrected to default
     *
     * <p>When this option is enabled,
     * deserializer will throw an exception saying math operation is invalid without correcting the file</p>
     *
     * @return strict math mode, <code>false</code> by default
     */
    boolean strictMath() default false;

    /**
     * Minimal allowed value
     * @return byte value, {@link Byte#MIN_VALUE} by default.
     */
    byte minByte() default Byte.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return byte value, {@link Byte#MAX_VALUE} by default.
     */
    byte maxByte() default Byte.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return short value, {@link Short#MIN_VALUE} by default.
     */
    short minShort() default Short.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return short value, {@link Short#MAX_VALUE} by default.
     */
    short maxShort() default Short.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return integer value, {@link Integer#MIN_VALUE} by default.
     */
    int minInt() default Integer.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return integer value, {@link Integer#MAX_VALUE} by default.
     */
    int maxInt() default Integer.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return long value, {@link Long#MIN_VALUE} by default.
     */
    long minLong() default Long.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return long value, {@link Long#MAX_VALUE} by default.
     */
    long maxLong() default Long.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return double value, {@link Double#MIN_VALUE} by default.
     */
    double minDouble() default Double.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return double value, {@link Double#MAX_VALUE} by default.
     */
    double maxDouble() default Double.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return float value, {@link Float#MIN_VALUE} by default.
     */
    float minFloat() default Float.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return float value, {@link Float#MAX_VALUE} by default.
     */
    float maxFloat() default Float.MAX_VALUE;
}
