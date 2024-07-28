package net.minecraftomega.config.annotations.conditions;

import java.lang.annotation.*;

/**
 * Conditions required on strings to match
 * In case string don't match any of the required conditions it gets corrected to default
 * If default doesn't match the conditions it throws an exception.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StringConditions {
    /**
     * Required CharSequence at the start of the parsed string
     * Mode is ignored for this condition, only checks if fulfills it
     * @return required CharSequence
     */
    String startsWith() default "";
    /**
     * Required CharSequence at the end of the parsed string
     * Mode is ignored for this condition, only checks if fulfills it
     * @return required CharSequence
     */
    String endsWith() default "";

    /**
     * Allows if a string can ve empty
     * @return required condition
     */
    boolean empty() default true;

    /**
     * Required CharSequence contained on the string
     * Works with RegExp if was configured to work as one
     * @return required CharSequence
     */
    String value() default "";

    /**
     *
     * @return
     */
    Mode mode() default Mode.STRING;

    enum Mode {
        STRING,
        REGEX,
        NEGATE_STRING,
        NEGATE_REGEX
    }
}
