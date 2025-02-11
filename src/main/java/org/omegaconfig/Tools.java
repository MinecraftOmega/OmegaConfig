package org.omegaconfig;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

public class Tools {
    public static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            // ignored
        }
    }

    public static void closeQuietly(RandomAccessFile in) {
        try {
            in.close();
        } catch (IOException e) {
            // ignored
        }
    }

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

    public static <T> T getFieldValue(Field field, Object context) {
        try {
            T result = (T) field.get(context);
            if (result == null) throw new NullPointerException("Field doesn't define a default value");
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate field" + (field != null ? " " + field.getName() : " because it's null"), e);
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
//        try {
//            Field f = Unsafe.class.getDeclaredField("theUnsafe");
//            f.setAccessible(true);
//            UNSAFE = (Unsafe) f.get(null);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new RuntimeException("Failed to get Unsafe", e);
//        }
    }
}
