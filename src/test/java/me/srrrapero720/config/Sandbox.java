package me.srrrapero720.config;

import net.minecraftomega.config.annotations.ConfigParent;
import net.minecraftomega.config.annotations.fields.ConfigField;
import net.minecraftomega.config.annotations.metadata.Comment;
import net.minecraftomega.config.annotations.metadata.MultiEnum;
import net.minecraftomega.config.annotations.metadata.StringConditions;
import net.minecraftomega.config.annotations.ranged.IntRange;
import net.minecraftomega.config.annotations.ranged.LongRange;
import net.minecraftomega.config.builder.fields.IntField;

import java.lang.reflect.Array;

@ConfigParent("")
public class Sandbox {
    @Comment("This is a first-line comment")
    @Comment("Here i can add more details")
    @Comment("Conditions are attached on comments automatically")
    @ConfigField(String.class)
    @StringConditions(startsWith = "{", endsWith = "}", empty = false)
    public static String stringField = "{test_of_what_should_contain}";

    @Comment("Single line comment")
    @LongRange(min = -48, max = 105)
    @ConfigField(long.class)
    public static long longField = 0L;

    @Comment("Comment")
    @MultiEnum
    @ConfigField(Enum.class)
    public static Yegoslovia[] multiEnum = new Yegoslovia[] { Yegoslovia.GIANT, Yegoslovia.SMALL };

    // CAN WORKS WITH ANNOTATIONS OR VIA BUILDER
    @IntRange(min = -50, max = 50)
    public IntField instanceIntField = null;

    static {
        Class<Yegoslovia[]> y = Yegoslovia[].class;
    }
}
