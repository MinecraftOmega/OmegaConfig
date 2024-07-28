package net.omegaloader.config.annotations.metadata;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Comment.Comments.class)
public @interface Comment {
    String[] value();

    String[] i18n() default "";

    @Documented
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Comments {
        Comment[] value();
    }
}