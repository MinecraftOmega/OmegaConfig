package specs;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.annotations.*;
import org.omegaconfig.impl.fields.IntField;
import org.omegaconfig.impl.fields.StringField;
import specs.util.EnumPredicateFilter;
import specs.util.EnumTest;

@Spec(value = "main", backups = 2, format = OmegaConfig.FORMAT_JSON5)
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
    public static EnumTest simpleEnum = EnumTest.TINY;

    @Comment("Comment")
    @ListConditions(stringify = true, filter = EnumPredicateFilter.class)
    @Spec.Field
    public static EnumTest[] multiEnum = new EnumTest[] { EnumTest.GIANT, EnumTest.SMALL };

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
