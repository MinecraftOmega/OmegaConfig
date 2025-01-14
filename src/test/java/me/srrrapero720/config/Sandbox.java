package me.srrrapero720.config;

import me.srrrapero720.config.util.CustomFilter;
import net.omegaloader.config.api.annotations.Config;
import net.omegaloader.config.api.annotations.ConfigField;
import net.omegaloader.config.api.annotations.ArrayConditions;
import net.omegaloader.config.api.annotations.FieldComment;
import net.omegaloader.config.api.annotations.NumberConditions;
import net.omegaloader.config.api.annotations.StringConditions;
import net.omegaloader.config.api.builder.IntField;

@Config(value = "examplemod")
public class Sandbox {
    @FieldComment("This is a first-line comment")
    @FieldComment("Here i can add more details")
    @FieldComment("Conditions are attached on comments automatically")
    @StringConditions(startsWith = "{", endsWith = "}", empty = false)
    @ConfigField
    public static String stringField = "{test_of_what_should_contain}";

    @StringConditions(empty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringConditions.Mode.REGEX)
    @ConfigField
    public static String resourceLocation = "examplemod:valid_resource_location";

    @FieldComment(value = {"Multiple comments", "but using just 1 annotation"})
    @NumberConditions(minLong = -48, maxLong = 105, math = true, strictMath = true)
    @ConfigField
    public static long longField = 0L;

    @FieldComment(value = "Single line comment", i18n = "examplemod.config.simple_enum.with_i18n")
    @ConfigField
    public static Yegoslovia simpleEnum = Yegoslovia.TINY;

    @FieldComment("Comment")
    @ArrayConditions(stringify = true, filter = CustomFilter.class)
    @ConfigField
    public static Yegoslovia[] multiEnum = new Yegoslovia[] { Yegoslovia.GIANT, Yegoslovia.SMALL };

    @Config(value = "sub_parent", i18n = "examplemod.config.parent.subparent.name")
    public static class SubParent {

        @StringConditions(empty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringConditions.Mode.REGEX)
        @ConfigField
        public static String resourceLocation = "examplemod:valid_resource_location";

    }

    // CAN WORKS WITH ANNOTATIONS OR VIA BUILDER
    @NumberConditions(minInt = -50, maxInt = 50)
    @ConfigField(value = "custonFieldName")
    @FieldComment("advanced int field")
    public IntField instanceIntField = null;

    static {
    }
}
