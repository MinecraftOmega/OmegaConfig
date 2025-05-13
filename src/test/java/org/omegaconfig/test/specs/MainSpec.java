package org.omegaconfig.test.specs;

import me.srrrapero720.config.Yegoslovia;
import me.srrrapero720.config.util.CustomFilter;
import org.omegaconfig.api.annotations.*;
import org.omegaconfig.impl.fields.IntField;
import org.omegaconfig.impl.fields.StringField;

@Spec("main")
public class MainSpec {
    @Comment("This is a first-line comment")
    @Comment("Here i can add more details")
    @Comment("Conditions are attached on comments automatically")
    @StringConditions(startsWith = "{", endsWith = "}", allowEmpty = false)
    @Spec.Field
    public static String stringField = "{test_of_what_should_contain}";

    @StringConditions(allowEmpty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringField.Mode.REGEX)
    @Spec.Field
    public static String resourceLocation = "examplemod:valid_resource_location";

    @Comment(value = {"Multiple comments", "but using just 1 annotation"})
    @NumberConditions(minLong = -48, maxLong = 105, math = true, strictMath = true)
    @Spec.Field
    public static long longField = 0L;

    @Comment(value = "Single line comment")
    @Spec.Field
    public static Yegoslovia simpleEnum = Yegoslovia.TINY;

    @Comment("Comment")
    @ListConditions(stringify = true, filter = CustomFilter.class)
    @Spec.Field
    public static Yegoslovia[] multiEnum = new Yegoslovia[] { Yegoslovia.GIANT, Yegoslovia.SMALL };

    @Spec(value = "sub_parent")
    public static class SubParent {
        @StringConditions(allowEmpty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringField.Mode.REGEX)
        @Spec.Field
        public static String resourceLocation = "examplemod:valid_resource_location";
    }

    // CAN WORKS WITH ANNOTATIONS OR VIA BUILDER
    @NumberConditions(minInt = -50, maxInt = 50)
    @Spec.Field(value = "custonFieldName")
    @Comment("advanced int field")
    public IntField instanceIntField = null;
}
