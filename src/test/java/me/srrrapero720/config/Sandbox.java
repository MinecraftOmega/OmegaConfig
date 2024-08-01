package me.srrrapero720.config;

import me.srrrapero720.config.util.CustomFilter;
import net.omegaloader.config.annotations.field.Config;
import net.omegaloader.config.annotations.field.ConfigField;
import net.omegaloader.config.annotations.conditions.ArrayConditions;
import net.omegaloader.config.annotations.metadata.Comment;
import net.omegaloader.config.annotations.conditions.NumberConditions;
import net.omegaloader.config.annotations.conditions.StringConditions;
import net.omegaloader.config.builder.field.IntField;

@Config(value = "", i18n = "examplemod.config.parent.name")
public class Sandbox {
    @Comment("This is a first-line comment")
    @Comment("Here i can add more details")
    @Comment("Conditions are attached on comments automatically")
    @StringConditions(startsWith = "{", endsWith = "}", empty = false)
    @ConfigField
    public static String stringField = "{test_of_what_should_contain}";

    @StringConditions(empty = false, value = "^[1-9a-z_]+:[1-9a-z_]+$", mode = StringConditions.Mode.REGEX)
    @ConfigField
    public static String resourceLocation = "examplemod:valid_resource_location";

    @Comment(value = {"Multiple comments", "but using just 1 annotation"})
    @NumberConditions(minLong = -48, maxLong = 105, math = true, strictMath = true)
    @ConfigField
    public static long longField = 0L;

    @Comment(value = "Single line comment", i18n = "examplemod.config.simple_enum.with_i18n")
    @ConfigField
    public static Yegoslovia simpleEnum = Yegoslovia.TINY;

    @Comment("Comment")
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
    @ConfigField(name = "custonFieldName")
    @Comment("advanced int field")
    public IntField instanceIntField = null;

    static {
    }
}
