package net.omegaloader.config.annotations.metadata;

import java.lang.annotation.*;

/**
 * Set comments on a field
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Comment.Comments.class)
public @interface Comment {
    /**
     * Can be placed in one annotation as an array or using multiple ones.
     * Ordering is up-to-down
     * @return Comments for a field.
     */
    String[] value();

    /**
     * Can be placed in one annotation as an array or using multiple ones.
     * Ordering is up-to-down
     *
     * @return Comments i18n for a field, empty by default.
     */
    String[] i18n() default "";

    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Comments {
        Comment[] value();
    }
}