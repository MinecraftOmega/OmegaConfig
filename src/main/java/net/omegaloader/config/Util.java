package net.omegaloader.config;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Util {
    public static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private static final Unsafe UNSAFE;

    public static String[] split(String str) {
        return DOT_PATTERN.split(str);
    }

    public static float similarity(String expected, String input) {
        if (expected == null || input == null) {
            throw new IllegalArgumentException("Strings can't be null");
        }

        int maxLength = Math.max(expected.length(), input.length());
        int matchCount = 0;

        // Check each char
        for (int i = 0; i < Math.min(expected.length(), input.length()); i++) {
            if (expected.charAt(i) == input.charAt(i)) {
                matchCount++;
            }
        }

        // Coincidence?
        return (float) matchCount / maxLength * 100;
    }

    public static Class<?> getContextType(Object o) {
        return o instanceof Class<?> clazz ? clazz : o.getClass();
    }

    public static Class<? extends Number> swapNumberClass(Class<? extends Number> clazz) {
        if (clazz == Long.class) {
            return long.class;
        } else if (clazz == Integer.class) {
            return int.class;
        } else if (clazz == Short.class) {
            return short.class;
        } else if (clazz == Byte.class) {
            return byte.class;
        } else if (clazz == Float.class) {
            return float.class;
        } else if (clazz == Double.class) {
            return double.class;
        } else {
            return clazz;
        }
    }

    public static <T, R> R[] map(T[] map, Class<R> resultType, Function<T, R> mapper) {
        R[] result = (R[]) Array.newInstance(resultType, map.length);
        for (int i = 0; i < map.length; i++) {
            result[i] = mapper.apply(map[i]);
        }
        return result;
    }

    public static void openField(Field field) {
        if (field.trySetAccessible()) {
            throw new IllegalStateException("Cannot enable accesibility for field '" + field.getName() + "'");
        }
    }

    public static <T> T getFieldValue(Field field, Object context) {
        try {
            T result = (T) field.get(context);
            if (result == null) throw new NullPointerException("Field doesn't define a default value");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate field" + (field != null ? " " + field.getName() : " because it's null"), e);
        }
    }

    public static <T> void setFieldAsType(Field field, Object context, T value) {
        if (field != null) {
            Class<?> c = value.getClass();
            if (c.isAssignableFrom(Number.class)) {
                c = Util.swapNumberClass((Class<? extends Number>) c);
            }
            if (c == byte.class) { // reflect did this check internally, here we skip it and have even more control on data types
                setField(field, context, (byte) value);
            } else if (c == short.class) {
                setField(field, context, (short) value);
            } else if (c == int.class) {
                setField(field, context, (int) value);
            } else if (c == long.class) {
                setField(field, context, (long) value);
            } else if (c == float.class) {
                setField(field, context, (float) value);
            } else if (c == double.class) {
                setField(field, context, (double) value);
            } else if (c == char.class) {
                setField(field, context, (char) value);
            } else {
                setField(field, context, value);
            }
        }
    }

    public static <T> void setField(Field field, Object context, T value) {
        try {
            field.set(context, value);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, byte byteValue) {
        try {
            field.setByte(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, short shortValue) {
        try {
            field.setShort(context, shortValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, int intValue) {
        try {
            field.setInt(context, intValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, long longValue) {
        try {
            field.setLong(context, longValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, float floatValue) {
        try {
            field.setFloat(context, floatValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, double byteValue) {
        try {
            field.setDouble(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, char charValue) {
        try {
            field.setChar(context, charValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }
    
    public static void throwUpdateValueException(Field field, Exception e) {
        throw new IllegalStateException("Failed to set new value to field '" + field.getName() + "'", e);
    }

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get Unsafe", e);
        }
    }
}
