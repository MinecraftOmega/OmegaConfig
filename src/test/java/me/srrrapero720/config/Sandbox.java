package me.srrrapero720.config;

import me.srrrapero720.config.util.CustomFilter;
import net.omegaloader.config.annotations.fields.Config;
import net.omegaloader.config.annotations.fields.ConfigField;
import net.omegaloader.config.annotations.conditions.ArrayConditions;
import net.omegaloader.config.annotations.metadata.Comment;
import net.omegaloader.config.annotations.conditions.NumberConditions;
import net.omegaloader.config.annotations.conditions.StringConditions;
import net.omegaloader.config.builder.fields.IntField;

@Config(value = "", i18n = "examplemod.config.parent.name")
public class Sandbox {
    @Comment("This is a first-line comment")
    @Comment("Here i can add more details")
    @Comment("Conditions are attached on comments automatically")
    @ConfigField(String.class)
    @StringConditions(startsWith = "{", endsWith = "}", empty = false, value = "YEPA", mode = StringConditions.Mode.REGEX)
    public static String stringField = "{test_of_what_should_contain}";

    @Comment(value = {"Multiple comments", "but using just 1 annotation"})
    @NumberConditions(minLong = -48, maxLong = 105)
    @ConfigField(long.class)
    public static long longField = 0L;

    @Comment("Single line comment")
    @ConfigField(Yegoslovia.class)
    public static Yegoslovia simpleEnum = Yegoslovia.TINY;

    @Comment("Comment")
    @ArrayConditions(stringify = true, filter = CustomFilter.class)
    @ConfigField(Yegoslovia.class)
    public static Yegoslovia[] multiEnum = new Yegoslovia[] { Yegoslovia.GIANT, Yegoslovia.SMALL };

    // CAN WORKS WITH ANNOTATIONS OR VIA BUILDER
    @NumberConditions(minInt = -50, maxInt = 50)
    public IntField instanceIntField = null;

    static {
    }
}
