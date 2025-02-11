package tests;

import org.omegaconfig.api.annotations.Spec;

@Spec("test")
public class BaseTestConfig {
    @Spec.Field
    public static byte aByte = 1;

    @Spec.Field
    public static int anInt = 500;

    @Spec.Field
    public static long aLong = 30L;

    @Spec.Field
    public static float aFloat = 15.638F;

    @Spec.Field
    public static double aDouble = 30.85616456;

    @Spec.Field
    public static char aChar = 'e';

    @Spec.Field
    public static String aString = "Hello String!";
}
