package me.srrapero720.waterconfig.api.annotations;

import java.lang.annotation.*;

/**
 * Conditions for temporal fields ({@link java.time.Duration Duration}, {@link java.time.Instant Instant},
 * {@code Local*}, {@code Offset/ZonedDateTime}). Bounds are inclusive and parsed with the same radix.
 *
 * <p>{@link java.time.Period Period} is not totally ordered, so {@code from}/{@code to} do not apply to it.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TimeConditions {
    /**
     * Inclusive lower bound in the configured radix. Empty means no lower bound.
     */
    String from() default "";

    /**
     * Inclusive upper bound in the configured radix. Empty means no upper bound.
     */
    String to() default "";

    /**
     * Serialization radix. Only {@link Radix#ISO_8601} is currently emitted; the enum is the
     * extension axis for future formats (epoch, friendly durations).
     */
    Radix radix() default Radix.ISO_8601;

    enum Radix {
        ISO_8601
    }
}
