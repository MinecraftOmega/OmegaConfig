package net.omegaloader.config.builder;

import net.omegaloader.config.annotations.field.Config;
import net.omegaloader.config.annotations.field.ConfigField;
import net.omegaloader.config.builder.field.*;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class ConfigSpec extends GroupField {
    private static final Unsafe UNSAFE;

    private String filename;
    private ConfigFileFormat format;
    private Object context;

    ConfigSpec(String filename, ConfigFileFormat format, Object context) {
        super(null, filename, "org.omegaloader.config.spec." + filename);
        this.format = format;
        this.context = context;

        Class<?> contextClass = context instanceof Class<?> clazz ? clazz : context.getClass();


    }

    // TODO: this doesn't follow the class ordering, it requires an special way to find the order
    private List<Object> preComputeFields(Class<?> context) {
        List<Field> configFields = new ArrayList<>();
        List<Class<?>> configClass = new ArrayList<>();


        for (Field f: context.getDeclaredFields()) {
            if (f.isAnnotationPresent(ConfigField.class) || BaseConfigField.class.isAssignableFrom(f.getType())) {
                configFields.add(f);
            }
        }

        for (Class<?> c: context.getDeclaredClasses()) {
            if (c.isAnnotationPresent(Config.class) || GroupField.class.isAssignableFrom(c)) {
                configClass.add(c);
            }
        }

        var result = new ArrayList<>();
        result.addAll(configFields);
        result.addAll(configClass);

        return result;
    }

    private <T extends BaseConfigField<?>> T[] computeFields(List<?> precomputedFields) throws Exception {
        BaseConfigField<?>[] result = new BaseConfigField[precomputedFields.size()];

        for (int i = 0; i < result.length; i++) {
            Object o = precomputedFields.get(i);
            if (o instanceof Field field) {
                if (field.isAnnotationPresent(ConfigField.class)) {
                    Class<?> converted = field.getType();

                    Modifier.isVolatile(field.getModifiers());


                    BaseConfigField<?> configField;
                    // enums, list, arrays, maps and records
                    if (converted.equals(int.class)) {
                        configField = new IntField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(long.class)) {
                        configField = new LongField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(float.class)) {
                        configField = new FloatField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(double.class)) {
                        configField = new DoubleField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(short.class)) {
                        configField = new ShortField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(byte.class)) {
                        configField = new ByteField.Context(context, field.getDeclaredAnnotations(), field);
                    } else if (converted.equals(String.class)) {
                        configField = new StringField.Context(context, field.getDeclaredAnnotations(), field);
                    } else {
                        if (field.getType().isAssignableFrom(Enum.class)) { // we can't do "type == enum"
                            configField = new EnumField.Context<>(context, field.getDeclaredAnnotations(), field);
                            continue;
                        }

                        if (field.getType().isAssignableFrom(List.class)) {
                            var type = (ParameterizedType) field.getGenericType();
                            var generic = type.getActualTypeArguments()[0];
                            if (generic != String.class) {
                                // FIXME: add more generic support
                                throw new IllegalArgumentException("Only string is accepted as a Map, Array or List Type");
                            }

                            configField = new ArrayField.Context<>(context, field.getDeclaredAnnotations(), field);
                            continue;
                        }
                        if (field.getType().isAssignableFrom(Map.class)) {
                            var type = (ParameterizedType) field.getGenericType();
                            var generics = type.getActualTypeArguments();
                            if (generics[0] != String.class || generics[1] != String.class) {
                                // FIXME: add more generic support
                                throw new IllegalArgumentException("Only string is accepted as a Map, Array or List Type");
                            }

                            configField = new MapField.Context<>(context, field.getDeclaredAnnotations(), field);
                            continue;
                        }

                        throw new UnsupportedOperationException("Field type is not supported");
                    }

                    result[i] = configField;
                    // contruct the field
                } else if (field.getType().isAssignableFrom(BaseConfigField.class)) {
                    result[i] = (BaseConfigField<?>) field.get(null);
                }
            } else if (o instanceof Class<?> clazz) {
                Method m = null;
            }
        }
    }

    public void save() {

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