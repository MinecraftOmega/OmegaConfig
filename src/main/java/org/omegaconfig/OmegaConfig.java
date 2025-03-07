package org.omegaconfig;

import org.omegaconfig.api.ICodec;
import org.omegaconfig.api.IComplexCodec;
import org.omegaconfig.api.IFormat;
import org.omegaconfig.api.annotations.Spec;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

import static org.omegaconfig.Tools.toBoxed;

public class OmegaConfig {
    public static final String FORMAT_PROPERTIES = "properties";
    public static final String FORMAT_CFG = "cfg";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_JSON5 = "json5";
    public static final String FORMAT_TOML = "toml";

    private static final Map<Class<?>, ICodec<?>> CODECS = new HashMap<>();
    private static final Map<String, IFormat> FORMATS = new HashMap<>();
    private static final Map<String, ConfigSpec> SPECS = new HashMap<>();
    public static final Thread WORKER = new Thread(OmegaConfig::run);

    private static Path CONFIG_PATH = new File("config").toPath();

    static {
        for (ICodec<?> c: ServiceLoader.load(ICodec.class))
            CODECS.put(c.type(), c);

        for (IFormat f: ServiceLoader.load(IFormat.class))
            FORMATS.put(f.id(), f);

        WORKER.setName("OmegaConfig-Worker-0");
        WORKER.setDaemon(false);
        WORKER.setPriority(3);
        WORKER.start();
    }

    public static void registerConfigPath(Path path) {
        File file = path.toFile();
        boolean created = file.mkdirs();
        boolean modifiable = file.canRead() && file.canWrite();

        if (!created && modifiable)
            throw new IllegalArgumentException("Cannot register config path '" + path + "'");

        CONFIG_PATH = path;
    }

    public static void register(ConfigSpec spec) {
        synchronized (SPECS) {
            SPECS.put(spec.name(), spec);
        }
    }

    public static void register(Object instance) {
        // RETRIEVE ANNOTATION
        final Class<?> specClass = instance instanceof Class<?> clazz ? clazz : instance.getClass();
        final Spec spec = Tools.getClassSpec(specClass);
        final boolean isStatic = instance == specClass; // same shit

        // CALCULATE FILE PATH (/specidentifier-suffix.format)
        final Path path = CONFIG_PATH.toAbsolutePath().resolve(spec.value() + (!spec.suffix().isEmpty() ? ("-" + spec.suffix()) : "") + "." + spec.format());

        // BUILDER
        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder(spec.value(), spec.format(), spec.suffix(), path, spec.backups());

        // ITERATE ALL CLASES
        register$iterateClass(instance, specClass, builder, isStatic);

        // PUT ON OUR REGISTER
        register(builder.build());

        System.out.println("Registered config " + path);
    }

    private static void register$iterateClass(Object instance, Class<?> specClass, ConfigSpec.SpecBuilder builder, boolean isStatic) {
        // FIRST, REGISTER ALL FIELDS
        register$builder(instance, specClass, builder, isStatic);

        // THEN, ITERATE CHILD CLASSES
        for (Class<?> clazz: specClass.getDeclaredClasses()) {
            System.out.println("Iterator - clazz: " + clazz.getName());
            final Spec spec = Tools.getClassSpecWeak(clazz);
            System.out.println("Iterator - spec is: " + spec == null ? "null" : "not null");
            if (spec == null) continue; // IGNORE NOT ANNOTATED CLASSES

            builder.push(spec.value());
            register$iterateClass(clazz, clazz, builder, isStatic);
            builder.pop();
        }
    }

    private static void register$builder(Object instance, Class<?> specClass, ConfigSpec.SpecBuilder builder, boolean isStatic) {
        // FIELDS
        for (Field f: specClass.getDeclaredFields()) {
            System.out.println("Iterator - field: " + f.getName());
            if (isStatic != Modifier.isStatic(f.getModifiers())) continue; // IGNORE NOT MATCHING CONTEXT
            System.out.println("Iterator - field: " + f.getName() + "not skipped");
            final Spec.Field specField = Tools.getFieldSpecWeak(f);
            final Class<?> specFieldClass = f.getType();

            System.out.println("Iterator - spec field: " + specField);

            if (specField == null)
                continue;

            // DEFINE BY TYPE, CAN BE REPLICATED
            Tools.defineByType(builder, specFieldClass, specField, f, instance);
        }
    }

    private static void run() {
        while (!WORKER.isInterrupted()) {
            synchronized (SPECS) {
                for (ConfigSpec spec: SPECS.values()) {
                    if (!spec.path().toFile().exists()) {
                        if (FORMATS.get(spec.format()).serialize(spec)) {
                            System.out.println("Serializing file...");
                        } else {
                            System.out.println("Failed to serialize file");
                        }
                    }

                    if (!spec.dirtyFields().isEmpty() || spec.dirty) {
                        FORMATS.get(spec.format()).deserialize(spec, spec.path());
                        spec.dirty = false;
                    }
                }
            }
        }
    }

    public static <T, T2> T tryParse(String value, Class<T> type, Class<T2> type2) {
        ICodec<T> codec = (ICodec<T>) CODECS.get(toBoxed(type));
        if (codec == null)
            throw new IllegalArgumentException("Codec for type '" + type.getName() + "' was not founded");

        if (codec instanceof IComplexCodec<T, ?> complexCodec) {

            return ((IComplexCodec<T, T2>) complexCodec).decode(value, type2);
        }
        return codec.decode(value);
    }
}
