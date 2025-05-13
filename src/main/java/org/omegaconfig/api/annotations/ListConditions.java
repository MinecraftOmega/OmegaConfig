package org.omegaconfig.api.annotations;

import java.lang.annotation.*;
import java.util.function.Predicate;

/**
 * Specify item array conditions for the serializer and deserializer
 * Valid only on array field types.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ListConditions {
    /**
     * Makes serializer to sugar the output in a single-line string separated by commas for all entries <code>field="Entry1, Entry2"</code>
     * default behavior is serializing all entries as a javascript array <code>field=["Entry1", "Entry2"]</code>
     * @return stringify array mode
     */
    boolean stringify() default false;

    /**
     * Makes serializer to output a single-line array instead of split all entries with a line break
     * @return singleline condition,
     * <code>false</code> by default, option is ignored when {@link ListConditions#stringify()} is <code>true</code>.
     */
    boolean singleline() default false;

    /**
     * Allows if the list can be empty
     *
     * @return non-empty conditional, <code>true</code> by default if the default value is NOT empty,
     * otherwise <code>false</code> by default.
     */
    boolean allowEmpty() default true;

    /**
     * Allows if the list can contain null values
     */
    boolean unique() default false;

    /**
     * Specify a limit of entries in an array,
     * limit is always the maximum theoretical size in java which is {@link Integer#MAX_VALUE}
     *
     * @return Max array size, {@link Integer#MAX_VALUE} by default.
     *
     * <p>Enum types have a length limit forced to the existing values length by default</p>
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
    Class<? extends Predicate> filter() default Predicate.class;
}
