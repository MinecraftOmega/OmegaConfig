package net.omegaloader.config.annotations.conditions;

import java.lang.annotation.*;

/**
 * Specify {@link java.nio.file.Path Path} conditions for the serializer and deserializer
 * Valid only on number field types.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PathConditions {

    /**
     * Path must be evaluated as a current working directory aka the runtime path.
     * <p>
     * Serializers adds an initial <code>/</code> character with no internal
     * effects when this option is enabled
     * </p>
     *
     * <p>For example: if cwd is "C:/my/path/to/working/dir/" and the config is "/data/myfancyfile.txt
     * the resulting fir is "C:/my/path/to/working/dir/data/myfancyfile.txt"</p>
     *
     * @return true by default
     */
    boolean runtimePath() default true;

    /**
     * Path must be targeting an existing file
     * Behavior depends on {@link #hardfail()}} value
     *
     * @return false by default
     */
    boolean existsFile() default false;

    /**
     * When is true conditions are not meet, throw a runtime exception about a wrong config file
     *
     * <p>When is false then just sends a log error</p>
     * @return false by default
     */
    boolean hardfail() default false;
}
