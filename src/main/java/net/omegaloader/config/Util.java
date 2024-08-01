package net.omegaloader.config;

import net.omegaloader.config.core.InvalidFieldException;
import net.omegaloader.config.core.RefreshValueException;

import java.lang.reflect.Field;

public class Util {
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
            return null;
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

    public static <T> void setField(Field field, Object context, T enumValue) {
        try {
            field.set(context, enumValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }

    public static <T extends Enum<T>> void setField(Field field, Object context, T enumValue) {
        try {
            field.set(context, enumValue);
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

    public static void setField(Field field, Object context, double byteValue) {
        try {
            field.setDouble(context, byteValue);
        } catch (ReflectiveOperationException e) {
            throw new RefreshValueException(field, e);
        }
    }
}
