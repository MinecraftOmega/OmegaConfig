package org.omegaconfig.api.annotations;

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
     *
     * @return required CharSequence at the start, empty by default
     */
    String startsWith() default "";
    /**
     * Required CharSequence at the end of the parsed string
     * Mode is ignored for this condition, only checks if fulfills it
     *
     * @return required CharSequence at the end, empty by default
     */
    String endsWith() default "";

    /**
     * Allows if a string can be empty
     *
     * @return non-empty conditional, <code>true</code> by default if the default value is NOT empty,
     * otherwise <code>false</code> by default.
     */
    boolean empty() default true;

    /**
     * Required CharSequence contained on the string
     * Accepts regex strings
     *
     * @see Mode
     * @return required CharSequence, empty by default
     */
    String value() default "";

    /**
     * Regex flags
     *
     * @see <a href="https://simpleregex.dev/flags/">Flags used in Regular Expressions</a>
     * @return string of all regex flags
     */
    String regexFlags() default "";

    /**
     * Check mode for the required char sequence
     *
     * @see Mode
     * @return check mode, rev variants are intended to act like <code>if (!true) {}</code>
     */
    Mode mode() default Mode.STRING_CONTAINS;

    enum Mode {
        STRING_CONTAINS,
        STRING_MATCHS,
        REGEX,
        STRING_CONTAINS_REV,
        STRING_MATCHS_REV,
        REGEX_REV
    }
}
