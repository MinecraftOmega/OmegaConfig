package net.omegaloader.config;

import net.omegaloader.config.core.InvalidFieldException;
import net.omegaloader.config.core.RefreshValueException;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class Util {
    private static final Unsafe UNSAFE;

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
            throw new InvalidFieldException(field, e);
        }
    }

    public static <T> void setField(Field field, Object context, T value) {
        try {
            field.set(context, value);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, byte byteValue) {
        try {
            field.setByte(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, short shortValue) {
        try {
            field.setShort(context, shortValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, int intValue) {
        try {
            field.setInt(context, intValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, long longValue) {
        try {
            field.setLong(context, longValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, float floatValue) {
        try {
            field.setInt(context, floatValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, double byteValue) {
        try {
            field.setDouble(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static void setField(Field field, Object context, char charValue) {
        try {
            field.setChar(context, charValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            f.trySetAccessible();
            UNSAFE = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get Unsafe", e);
        }
    }
}
