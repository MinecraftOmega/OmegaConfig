package net.minecraftomega.config.annotations.metadata;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Comments {
    Comment[] value();
}
