package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.annotations.Spec;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;

public class Tools {
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

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length == 0) return null;
            if (typeArgs.length == 1) {
                if (typeArgs[0] instanceof Class<?> c) return toBoxed(c);
                if (typeArgs[0] instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
                return null;
            }
            throw new IllegalArgumentException("Class has more than 2 type arguments");
        }

        return null;
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
            throw new IllegalStateException("Failed to set new value to field '" + field.getName() + "'", e);
        }
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        try (var in = new FileInputStream(path.toFile())) {
            return in.readAllBytes();
        }
    }
}
