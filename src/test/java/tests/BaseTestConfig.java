package tests;

import org.omegaconfig.api.annotations.NumberConditions;
import org.omegaconfig.api.annotations.Spec;

import java.nio.file.Path;
import java.util.List;

@Spec("test")
public class BaseTestConfig {
    @Spec.Field
    public static byte aByte = 1;

    @Spec.Field
    @NumberConditions(minInt = 1, maxInt = 100, math = true, strictMath = true)
    public static int anInt = 500;

    @Spec.Field
    @NumberConditions(minLong = 3, maxLong = 31, math = true, strictMath = false)
    public static long aLong = 30L;

    @Spec.Field
    @NumberConditions(minFloat = 0, maxFloat = 95, math = false, strictMath = true)
    public static float aFloat = 15.638F;

    @Spec.Field
    @NumberConditions(minDouble = 0d, maxDouble = 95d, math = true, strictMath = true)
    public static volatile double aDouble = 30.856169456;

    @Spec.Field
    public static char aChar = 'e';

    @Spec.Field
    public static String aString = "Hello String!";

    @Spec("sub_config")
    public static class SubConfig {
        @Spec.Field
        public static byte aByte = 10;

        @Spec.Field
        public static int anInt = 1;

        @Spec.Field
        public static long aLong = 80000000000L;

        @Spec.Field
        public static float aFloat = 3.1416F;

        @Spec.Field
        public static double aDouble = 98.88416456;

        @Spec.Field
        public static char aChar = 'e';

        @Spec.Field
        public static String aString = "Hello String!";

        @Spec.Field
        public static List<String> aList = List.of("Hello", "World");

        @Spec.Field
        public static Path aPath = Path.of("config/test.txt");
    }
}
