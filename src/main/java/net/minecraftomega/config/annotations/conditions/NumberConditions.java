package net.minecraftomega.config.annotations.conditions;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NumberConditions {
    /**
     * Accept basic math operations such add, subtract, multiply, divide and square.
     * This is an outsmarting of serializer specs, but helps users to automatically do math operation on advanced fields.
     * @return math acceptance on number fields
     */
    boolean math() default false;

    /**
     * Normal behavior is resolve the math operation, when the result is a float and field is an int; then it got cast back to int.
     * When the result exceeds the field max or min limits, then it got capped to max value
     * In case math operation has a wrong syntax, value got corrected to default
     *
     * <p>When is enabled, deserializer will throw an exception saying math operation is invalid without correcting the file</p>
     */
    boolean strictMath() default false;

    /**
     * Minimal allowed value
     * @return byte value
     */
    byte minByte() default Byte.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return byte value
     */
    byte maxByte() default Byte.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return short value
     */
    short minShort() default Short.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return short value
     */
    short maxShort() default Short.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return integer value
     */
    int minInt() default Integer.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return integer value
     */
    int maxInt() default Integer.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return long value
     */
    long minLong() default Long.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return long value
     */
    long maxLong() default Long.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return double value
     */
    double minDouble() default Double.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return double value
     */
    double maxDouble() default Double.MAX_VALUE;

    /**
     * Minimal allowed value
     * @return float value
     */
    float minFloat() default Float.MIN_VALUE;

    /**
     * Maximum allowed value
     * @return float value
     */
    float maxFloat() default Float.MAX_VALUE;
}
