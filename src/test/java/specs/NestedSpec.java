package specs;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.annotations.Comment;
import org.omegaconfig.api.annotations.Spec;

@Spec(value = "nested_spec", format = OmegaConfig.FORMAT_CFG)
@Comment("This is a nested specification example")
@Comment("It contains various fields and a nested extender class")
public class NestedSpec {
    @Spec.Field()
    public static int value = 42;
    @Spec.Field()
    public static String name = "default";
    @Spec.Field()
    public static boolean active = true;
    @Spec.Field()
    @Comment("Extender")
    public static final Nested extender = new Nested();

    @Spec(value = "nested", disableStatic = true)
    @Comment("This is the nested class extender")
    public static final class Nested {
        @Spec.Field()
        public String description = "";
        @Spec.Field()
        public double ratio = 1.0;
    }

    @Spec(value = "staticly")
    @Comment("This is the nested class extender")
    public static final class Staticly {
        @Spec.Field()
        public String description = "";
        @Spec.Field()
        public double ratio = 1.0;
    }
}
