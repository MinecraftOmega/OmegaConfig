package net.omegaloader.config.api.annotations;

import java.lang.annotation.*;

/**
 * Attached commands on the config field
 *
 * <p>Value or i18n cannot be both empty, requires one definition of them</p>
 */
@Documented
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FieldComment.Comments.class)
public @interface FieldComment {
    /**
     * Can be placed in one annotation as an array or using multiple ones.
     * Ordering is up-to-down
     *
     * @return Comments for a field.
     */
    String[] value() default "";

    /**
     * Helper annotation to use multiple Comment annotations
     * If you want to use it... what is wrong with you?
     */
    @Documented
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Comments {
        FieldComment[] value();
    }
}