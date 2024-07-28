package net.omegaloader.config.annotations.conditions;

import java.lang.annotation.*;
import java.util.function.Predicate;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ArrayConditions {
    /**
     * Makes serializer to sugar the output in a single-line string separated by commas for all entries <code>field="Entry1, Entry2"</code>
     * default behavior is serializing all entries as a javascript array <code>field=["Entry1", "Entry2"]</code>
     * @return stringify array mode
     */
    boolean stringify() default false;

    /**
     * Makes serializer to output a single-line array instead of split all entries with a line break
     * @return array align
     */
    boolean singleline() default false;

    /**
     * Specify a limit of valid entries, limit is always the int {@link Integer#MAX_VALUE MAX_VALUE}
     * For enums, limit is always the max amount of enums
     * @return array limit
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

    /**
     * Sets sorting mode for the array
     * @see Sorting
     * @return the predefined sort mode, for custom sorting use builder instead
     */
    Sorting sorting() default Sorting.NONE;

    enum Sorting {
        NONE,
        ALPHABETICAL,
        BYTE_WEIGHT,
        ALPHABETICAL_INVERSE,
        BYTE_WEIGHT_INVERSE;
    }
}
