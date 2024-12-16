package net.omegaloader.config.api.annotations;

import java.lang.annotation.*;
import java.util.function.Predicate;

/**
 * Specify item map conditions for the serializer and deserializer.
 * <br>
 * Valid only on <code>Map&lt;String, ?&gt;</code> field types.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapConditions {

    /**
     * Specify a limit of entries in a map,
     * limit is always the maximum theoretical size in java which is {@link Integer#MAX_VALUE}
     *
     * @return Max array size, {@link Integer#MAX_VALUE} by default.
     */
    int limit() default Integer.MAX_VALUE;

    /**
     * Due to annotation limitations, we require class as an argument,
     * The given class must not be an interface or an abstract class and have to
     * implements {@link Predicate} to make possible to make a new instance via reflection.
     * Filter is cached at the first time it got required.
     *
     * <p>Provide a custom filter for invalid entries.
     * filtered entries get removed after finishing parsing</p>
     * @see Predicate
     * @return filter class, this must be a custom class implementing the filter
     */
    Class<? extends Predicate> keyFilter() default Predicate.class;

    /**
     * Due to annotation limitations, we require class as an argument,
     * The given class must not be an interface or an abstract class and have to
     * implements {@link Predicate} to make possible to make a new instance via reflection.
     * Filter is cached at the first time it got required.
     *
     * <p>Provide a custom filter for invalid entries.
     * filtered entries get removed after finishing parsing</p>
     * @see Predicate
     * @return filter class, this must be a custom class implementing the filter
     */
    Class<? extends Predicate> valueFilter() default Predicate.class;
}
