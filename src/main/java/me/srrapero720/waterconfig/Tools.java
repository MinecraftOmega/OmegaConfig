package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.Spec;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class Tools {
    public static Class<?> toPrimitive(Class<?> clazz) {
        if (clazz == Integer.class) return int.class;
        if (clazz == Double.class) return double.class;
        if (clazz == Float.class) return float.class;
        if (clazz == Long.class) return long.class;
        if (clazz == Boolean.class) return boolean.class;
        if (clazz == Short.class) return short.class;
        if (clazz == Byte.class) return byte.class;
        if (clazz == Character.class) return char.class;
        return clazz;
    }

    public static Class<?> toBoxed(Class<?> clazz) {
        if (clazz == int.class) return Integer.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == long.class) return Long.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == short.class) return Short.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == char.class) return Character.class;
        return clazz;
    }

    public static Spec specOfWeak(Class<?> c) {
        return c.getAnnotation(Spec.class);
    }

    public static Spec specOf(Class<?> c) {
        Spec spec = specOfWeak(c);
        if (spec == null)
            throw new IllegalArgumentException("Class '" + c.getName() + "' has no Spec annotation");

        return spec;
    }

    public static Spec.Field specFieldOf(Field field) {
        return field.getAnnotation(Spec.Field.class);
    }

    public static Class<?> typeOf(Field field) {
        return toBoxed(field.getType());
    }

    public static Class<?> subTypeOf(Field field) {
        Class<?> type = typeOf(field);

        if (type.isArray()) {
            return type.getComponentType();
        }

        TypeVariable<?>[] types = type.getTypeParameters();

        if (types.length == 0) return null;
        if (types.length == 1) return types[0].getClass(); // toBoxed in generics is not needed

        throw new IllegalArgumentException("Class has more than 2 type arguments");
    }

    public static String concat(String prefix, String suffix, char key, Collection<String> strings) {
        StringBuilder result = new StringBuilder((strings.size() * 8) + 16);
        result.append(prefix);
        Iterator<String> it = strings.iterator();
        while (it.hasNext()) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(key);
            }
        }
        result.append(suffix);
        return result.toString();
    }

    public static String concat(String prefix, String suffix, char key, String... strings) {
        return concat(prefix, suffix, key, Arrays.asList(strings));
    }

    public static void closeQuietly(Closeable in) {
        try { in.close(); } catch (IOException e) { /* ignored */ }
    }

    public static <T> T valueFrom(Field field, Object context) {
        try {
            T result = (T) field.get(context);
            if (result == null) throw new NullPointerException("Field its empty");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate field" + (field != null ? " " + field.getName() : " because it's null"), e);
        }
    }

    public static <T> void setFieldValue(Field field, Object context, T value) {
        try {
            field.set(context, value);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, byte byteValue) {
        try {
            field.setByte(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, short shortValue) {
        try {
            field.setShort(context, shortValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, int intValue) {
        try {
            field.setInt(context, intValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, long longValue) {
        try {
            field.setLong(context, longValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, float floatValue) {
        try {
            field.setFloat(context, floatValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, double byteValue) {
        try {
            field.setDouble(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throwUpdateValueException(field, e);
        }
    }

    public static void setFieldValue(Field field, Object context, char charValue) {
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
//        try {
//            Field f = Unsafe.class.getDeclaredField("theUnsafe");
//            f.setAccessible(true);
//            UNSAFE = (Unsafe) f.get(null);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new RuntimeException("Failed to get Unsafe", e);
//        }
    }


    public static boolean contains(char c, char[] expected) {
        for (char e: expected) {
            if (c == e) return true;
        }
        return false;
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        try (var in = new FileInputStream(path.toFile());) {
            return in.readAllBytes();
        }
    }

    public static boolean requireNotNull(Object[] parsedValues) {
        for (Object value: parsedValues) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }
}
