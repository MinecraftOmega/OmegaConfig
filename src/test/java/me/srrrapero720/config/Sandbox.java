package me.srrrapero720.config;

import me.srrrapero720.config.util.CustomFilter;
import org.omegaconfig.api.annotations.Spec;
import org.omegaconfig.api.annotations.ArrayConditions;
import org.omegaconfig.api.annotations.FieldComment;
import org.omegaconfig.api.annotations.NumberConditions;
import org.omegaconfig.api.annotations.StringConditions;
import org.omegaconfig.impl.fields.IntField;

@Spec(value = "examplemod")
public class Sandbox {
    @FieldComment("This is a first-line comment")
    @FieldComment("Here i can add more details")
    @FieldComment("Conditions are attached on comments automatically")
    @StringConditions(startsWith = "{", endsWith = "}", empty = false)
    @Spec.Field
    public static String stringField = "{test_of_what_should_contain}";

    @StringConditions(empty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringConditions.Mode.REGEX)
    @Spec.Field
    public static String resourceLocation = "examplemod:valid_resource_location";

    @FieldComment(value = {"Multiple comments", "but using just 1 annotation"})
    @NumberConditions(minLong = -48, maxLong = 105, math = true, strictMath = true)
    @Spec.Field
    public static long longField = 0L;

    @FieldComment(value = "Single line comment")
    @Spec.Field
    public static Yegoslovia simpleEnum = Yegoslovia.TINY;

    @FieldComment("Comment")
    @ArrayConditions(stringify = true, filter = CustomFilter.class)
    @Spec.Field
    public static Yegoslovia[] multiEnum = new Yegoslovia[] { Yegoslovia.GIANT, Yegoslovia.SMALL };

    @Spec(value = "sub_parent")
    public static class SubParent {

        @StringConditions(empty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringConditions.Mode.REGEX)
        @Spec.Field
        public static String resourceLocation = "examplemod:valid_resource_location";

    }

    // CAN WORKS WITH ANNOTATIONS OR VIA BUILDER
    @NumberConditions(minInt = -50, maxInt = 50)
    @Spec.Field(value = "custonFieldName")
    @FieldComment("advanced int field")
    public IntField instanceIntField = null;

}