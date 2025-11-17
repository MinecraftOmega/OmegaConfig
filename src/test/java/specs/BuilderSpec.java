package specs;

import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;
import org.omegaconfig.impl.fields.IntField;
import org.omegaconfig.impl.fields.StringField;

public class BuilderSpec {
    private static final ConfigSpec.SpecBuilder BUILDER = new ConfigSpec.SpecBuilder("buildercfg", OmegaConfig.FORMAT_PROPERTIES, "test", 0);

    public static final IntField A_INT_FIELD = BUILDER.defineInt("aIntField", 0)
            .comments("This is an int field", "It has a default value of 0")
            .math(true)
            .strictMath(true)
            .setMin(0)
            .setMax(100)
            .end();

    public static final StringField A_STRING_FIELD = BUILDER.defineString("aStringField", "default")
            .comments("This is a string field", "It has a default value of 'default'")
            .end();

    public static void init() {
        OmegaConfig.register(BUILDER.build());
    }
}
