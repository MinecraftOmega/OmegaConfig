package net.minecraftomega.config;

public class Util {
    public static Class<?> swapNumberClass(Class<?> clazz) {
        Class<?> number = clazz.getClass();
        if (number.equals(Long.class)) {
            return long.class;
        } else if (number.equals(Integer.class)) {
            return int.class;
        } else if (number.equals(Short.class)) {
            return short.class;
        } else if (number.equals(Byte.class)) {
            return byte.class;
        } else if (number.equals(Float.class)) {
            return float.class;
        } else if (number.equals(Double.class)) {
            return double.class;
        } else {
            return clazz;
        }
    }
}
