package org.omegaconfig;

import org.omegaconfig.api.ICodec;
import org.omegaconfig.api.IComplexCodec;
import org.omegaconfig.api.annotations.Comment;
import org.omegaconfig.api.annotations.NumberConditions;
import org.omegaconfig.api.annotations.Spec;
import org.omegaconfig.api.annotations.StringConditions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

import static org.omegaconfig.OmegaConfigRegistry.*;
import static org.omegaconfig.Tools.toBoxed;

public class OmegaConfig {
    private static final Thread RT_WORKER = new Thread(OmegaConfig::run);
    public static final String ID = "omegaconfig";

    // FORMATS
    public static final String FORMAT_PROPERTIES = "properties";
    public static final String FORMAT_CFG = "cfg";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_JSON5 = "json5";
    public static final String FORMAT_TOML = "toml";

    public static Path getPath() { return CONFIG_PATH; }
    public static void setPath(Path configPath) { CONFIG_PATH = configPath; }

    public static ConfigSpec register(ConfigSpec spec) {
        synchronized (SPECS) {
            SPECS.put(spec.name(), spec);
        }

        return spec;
    }

    public static ConfigSpec register(Class<?> clazz) {
        Objects.requireNonNull(clazz, "Spec class cannot be null");

        // RETRIEVE ANNOTATION
        final Spec spec = Tools.specOf(clazz);
        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder(spec.value(), FORMATS.get(spec.format()), spec.suffix(), spec.backups());

        // ITERATE ALL CLASES
        register$iterateClass(clazz, clazz, builder, true);

        // PUT ON OUR REGISTER
        return register(builder.build());
    }

    public static ConfigSpec register(Object instance) {
        Objects.requireNonNull(instance, "Spec instance cannot be null");

        // RETRIEVE ANNOTATION
        if (instance instanceof Class<?> clazz) {
            return register(clazz);
        }
        final Class<?> specClass = instance.getClass();
        final Spec spec = Tools.specOf(specClass);

        // BUILDER
        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder(spec.value(), FORMATS.get(spec.format()), spec.suffix(), spec.backups());

        // ITERATE ALL CLASES
        register$iterateClass(instance, specClass, builder, false);

        // PUT ON OUR REGISTER
        return register(builder.build());
    }

    private static void register$iterateClass(Object instance, Class<?> specClass, ConfigSpec.SpecBuilder builder, boolean isStatic) {
        // FIRST, REGISTER ALL FIELDS
        for (Field field: specClass.getDeclaredFields()) {
            if (isStatic != Modifier.isStatic(field.getModifiers())) continue; // IGNORE NOT MATCHING CONTEXT
            final Spec.Field specField = Tools.specFieldOf(field);
            final Class<?> specFieldClass = Tools.toBoxed(field.getType());

            if (specField == null)
                continue;

            // DEFINE BY TYPE, CAN BE REPLICATED
            ConfigSpec.BaseFieldBuilder<?, ?> fieldBuilder;
            final String name = specField.value().isEmpty() ? field.getName() : specField.value();
            if (specFieldClass == Boolean.class) fieldBuilder = builder.defineBoolean(name, field, instance);
            else if (specFieldClass == Byte.class) {
                ConfigSpec.ByteFieldBuilder numberFieldBuilder = builder.defineByte(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minByte());
                    numberFieldBuilder.setMax(conditions.maxByte());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == Short.class) {
                ConfigSpec.ShortFieldBuilder numberFieldBuilder = builder.defineShort(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minShort());
                    numberFieldBuilder.setMax(conditions.maxShort());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == Integer.class) {
                ConfigSpec.IntFieldBuilder numberFieldBuilder = builder.defineInt(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minInt());
                    numberFieldBuilder.setMax(conditions.maxInt());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == Long.class) {
                ConfigSpec.LongFieldBuilder numberFieldBuilder = builder.defineLong(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minLong());
                    numberFieldBuilder.setMax(conditions.maxLong());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == Float.class) {
                ConfigSpec.FloatFieldBuilder numberFieldBuilder = builder.defineFloat(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minFloat());
                    numberFieldBuilder.setMax(conditions.maxFloat());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == Double.class) {
                ConfigSpec.DoubleFieldBuilder numberFieldBuilder = builder.defineDouble(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minDouble());
                    numberFieldBuilder.setMax(conditions.maxDouble());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldClass == String.class) {
                ConfigSpec.StringFieldBuilder stringFieldBuilder = builder.defineString(name, field, instance);

                // STRING CONDITIONS
                final StringConditions conditions = field.getAnnotation(StringConditions.class);
                if (conditions != null) {
                    stringFieldBuilder.startsWith(conditions.startsWith());
                    stringFieldBuilder.endsWith(conditions.endsWith());
                    stringFieldBuilder.allowEmpty(conditions.allowEmpty());
                    stringFieldBuilder.condition(conditions.value());
                    stringFieldBuilder.regexFlags(conditions.regexFlags());
                    stringFieldBuilder.mode(conditions.mode());
                }

                fieldBuilder = stringFieldBuilder;
            } else if (specFieldClass == Path.class) {
                fieldBuilder = builder.definePath(name, field, instance);
            }
            else if (specFieldClass == Character.class) fieldBuilder = builder.defineChar(name, field, instance);
            else if (specFieldClass == List.class) fieldBuilder = builder.defineList(name, field, instance, Tools.subTypeOf(field));
            else if (specFieldClass.isArray()) fieldBuilder = builder.defineArray(name, field, instance, Tools.subTypeOf(field));
            else if (Enum.class.isAssignableFrom(specFieldClass)) fieldBuilder = builder.defineEnum(name, field, instance);
            else fieldBuilder = builder.define(name, field, instance);

            // COMMENTS
            Comment.Comments comments = field.getAnnotation(Comment.Comments.class);
            if (comments != null) {
                for (Comment comment: comments.value()) {
                    fieldBuilder.comments(comment.value());
                }
            }

            // END
            fieldBuilder.end();
        }

        // THEN, ITERATE CHILD CLASSES
        for (Class<?> clazz: specClass.getDeclaredClasses()) {
            final Spec spec = Tools.specOfWeak(clazz);
            if (spec == null) continue; // IGNORE NOT ANNOTATED CLASSES

            builder.push(spec.value());
            register$iterateClass(clazz, clazz, builder, isStatic);
            builder.pop();
        }
    }

    static String[] tryEncode(Object[] value, Class<?> type, Class<?> subType) {
        if (value instanceof String[] s) {
            return s;
        }

        ICodec<Object> codec = (ICodec<Object>) CODECS.get(toBoxed(subType));

        if (codec == null) {
            for (ICodec<?> c: CODECS.values()) {
                // SECOND ATTEMPT
                if (c.type().isAssignableFrom(subType)) {
                    codec = (ICodec<Object>) c;
                    break;
                }
            }
        }

        if (codec == null) {
            throw new IllegalArgumentException("Codec for type '" + value.getClass().getName() + "' was not founded");
        }

        String[] result = new String[value.length];

        if (codec instanceof IComplexCodec<Object, ?> complexCodec) {
            for (int i = 0; i < value.length; i++) {
                result[i] = complexCodec.encode(value[i], subType);
            }
            return result;
        }

        for (int i = 0; i < value.length; i++) {
            result[i] = codec.encode(value[i]);
        }

        return result;
    }

    static String tryEncode(Object value) {
        return tryEncode(value, null);
    }

    static String tryEncode(Object value, Class<?> subType) {
        if (value instanceof String s) {
            return s;
        }

        ICodec<Object> codec = (ICodec<Object>) CODECS.get(toBoxed(value.getClass()));

        if (codec == null) {
            for (ICodec<?> c: CODECS.values()) {
                // FIRST ATTEMPT
                if (c.type().isInstance(value)) {
                    codec = (ICodec<Object>) c;
                    break;
                }

                // SECOND ATTEMPT
                if (c.type().isAssignableFrom(value.getClass())) {
                    codec = (ICodec<Object>) c;
                    break;
                }
            }
        }

        if (codec == null) {
            throw new IllegalArgumentException("Codec for type '" + value.getClass().getName() + "' was not founded");
        }

        if (codec instanceof IComplexCodec<Object, ?> complexCodec) {
            return complexCodec.encode(value, subType);
        }

        return codec.encode(value);
    }

    static <T, T2> T[] tryParse(String[] value, Class<T> type, Class<T2> subType) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }
        if (subType == String.class) {
            return (T[]) value;
        }

        ICodec<T> codec = (ICodec<T>) CODECS.get(toBoxed(subType));

        if (codec == null) {
            for (ICodec<?> c: CODECS.values()) {
                if (c.type().isAssignableFrom(subType)) {
                    codec = (ICodec<T>) c;
                    break;
                }
            }
        }

        if (codec == null)
            throw new IllegalArgumentException("Codec for type '" + type.getName() + "' with subType '" + subType.getName() + "' was not founded");

        if (codec instanceof IComplexCodec<T, ?> complexCodec) {
            T[] result = (T[]) Array.newInstance(subType, value.length);
            for (int i = 0; i < value.length; i++) {
                result[i] = ((IComplexCodec<T, T2>) complexCodec).decode(value[i], subType);
            }
            return result;
        }

        T[] result = (T[]) Array.newInstance(subType, value.length);
        for (int i = 0; i < value.length; i++) {
            result[i] = codec.decode(value[i]);
        }

        return result;
    }

    static <T, T2> T tryParse(String value, Class<T> type, Class<T2> type2) {
        if (type == String.class) {
            return (T) value;
        }


        ICodec<T> codec = (ICodec<T>) CODECS.get(toBoxed(type));


        if (codec == null) {
            for (ICodec<?> c: CODECS.values()) {
                if (c.type().isAssignableFrom(type)) {
                    codec = (ICodec<T>) c;
                    break;
                }
            }
        }

        if (codec == null)
            throw new IllegalArgumentException("Codec for type '" + type.getName() + "' was not founded");

        if (codec instanceof IComplexCodec<T, ?> complexCodec) {
            return ((IComplexCodec<T, T2>) complexCodec).decode(value, type2);
        }
        return codec.decode(value);
    }

    static void run() {
        while (!Thread.interrupted()) {
            synchronized (SPECS) {
                for (ConfigSpec spec: SPECS.values()) {
                    if (!spec.isLoaded()) {
                        try {
                            spec.load();
                        } catch (Exception e) {
                            e.printStackTrace();
                            spec.setDirty(true); // mark as dirty to re-save
                        }
                    }
                    if (spec.isDirty()) {
                        try {
                            spec.save();

                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to save spec '" + spec.name() + "'", e);
                        }
                    }
                    if (spec.isReload()) {
                        try {
                            spec.load();
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to reload spec '" + spec.name() + "'", e);
                        }
                    }
                }
            }
        }
    }

    public static void init() {
        OmegaConfigRegistry.init();
        // This method is intentionally left empty.
        // It can be used for any initialization logic if needed in the future.
    }

    static {
        init();
        RT_WORKER.setDaemon(true);
        RT_WORKER.setName("OmegaConfig-Worker");
        RT_WORKER.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                RT_WORKER.interrupt();
                RT_WORKER.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }
}
