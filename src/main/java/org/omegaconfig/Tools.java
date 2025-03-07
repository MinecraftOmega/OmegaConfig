package org.omegaconfig;

import it.unimi.dsi.fastutil.Pair;
import org.omegaconfig.api.annotations.Spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.List;

public class Tools {
    public static Annotation[] getAnnotation(Class<?> c) {
        return c.getAnnotations();
    }

    public static Pair<Class<?>, Spec> getSpecPair(Class<?> clazz) {
        return Pair.of(clazz, getClassSpec(clazz));
    }

    public static Spec getClassSpec(Class<?> c) {
        Spec spec = c.getAnnotation(Spec.class);
        if (spec == null)
            throw new IllegalArgumentException("Class '" + c.getName() + "' has no Spec annotation");

        return spec;
    }

    public static Spec getClassSpecWeak(Class<?> c) {
        return c.getAnnotation(Spec.class);
    }

    public static Spec.Field getFieldSpecWeak(Field field) {
        return field.getAnnotation(Spec.Field.class);
    }

    public static Class<?> getType(Field field) {
        return toBoxed(field.getType());
    }

    public static Class<?> getOneArgType(Field field) {
        Class<?> type = getType(field);
        TypeVariable<?>[] types = type.getTypeParameters();

        if (types.length == 0) return null;
        if (types.length == 1) return toBoxed(types[0].getClass());

        throw new IllegalArgumentException("Class has more than 2 type arguments");
    }

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

    public static void defineByType(ConfigSpec.SpecBuilder builder, Class<?> fieldClass, Spec.Field specField, Field field, Object context) {
        final String name = specField.value().isEmpty() ? field.getName() : specField.value();
        if (fieldClass == Boolean.class) builder.defineBoolean(name, field, context);
        else if (fieldClass == Byte.class) builder.defineByte(name, field, context);
        else if (fieldClass == Short.class) builder.defineShort(name, field, context);
        else if (fieldClass == Character.class) builder.defineChar(name, field, context);
        else if (fieldClass == Integer.class) builder.defineInt(name, field, context);
        else if (fieldClass == Long.class) builder.defineLong(name, field, context);
        else if (fieldClass == Float.class) builder.defineFloat(name, field, context);
        else if (fieldClass == Double.class) builder.defineDouble(name, field, context);
        else if (fieldClass == String.class) builder.defineString(name, field, context);
        else if (fieldClass == List.class) builder.defineList(name, field, context, Tools.getOneArgType(field));
        else if (fieldClass.isAssignableFrom(Enum.class)) builder.defineEnum(name, field, context);
        else builder.define(name, field, context);
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

    public static String concat(String prefix, String key, String... strings) {
        StringBuilder result = new StringBuilder((strings.length * 8) + 16);
        result.append(prefix);
        for (String s: strings) {
            result.append(s);
            if (s != strings[strings.length - 1]) { // != was intentional
                result.append(key);
            }
        }
        return result.toString();
    }
}
