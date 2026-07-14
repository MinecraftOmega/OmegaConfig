package me.srrapero720.waterconfig.api.annotations;

import java.lang.annotation.*;

/**
 * Serialization radix for a {@link java.awt.Color Color} field. On load any supported form is
 * accepted (so the radix can change without breaking old files); on save the configured radix wins.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ColorConditions {
    Radix value() default Radix.AUTO;

    enum Radix {
        /** Shortest lossless hex: {@code RGB}/{@code RRGGBB} when opaque, {@code RGBA}/{@code RRGGBBAA} otherwise. */
        AUTO,
        /** Hex without alpha: {@code RGB}/{@code RRGGBB} (alpha forced opaque). */
        OPAQUE,
        /** Hex with alpha: {@code RGBA}/{@code RRGGBBAA}. */
        ALPHA,
        /** A {@code { r, g, b }} group of byte channels (no alpha). */
        BYTE_SPLIT,
        /** A {@code { r, g, b, a }} group of byte channels. */
        BYTE_SPLIT_ALPHA
    }
}
