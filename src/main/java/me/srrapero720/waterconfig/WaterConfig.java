package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.ICodec;
import me.srrapero720.waterconfig.api.IComplexCodec;
import me.srrapero720.waterconfig.api.annotations.Comment;
import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.annotations.StringConditions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static me.srrapero720.waterconfig.WaterConfigRegistry.*;
import static me.srrapero720.waterconfig.Tools.toBoxed;

public class WaterConfig {
    public static final String ID = "waterconfig";

    // ══════════════════════════════════════════════════════════
    //  THREADING
    // ══════════════════════════════════════════════════════════
    private static final ExecutorService IO_POOL = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "WaterConfig-IO");
        t.setDaemon(true);
        return t;
    });
    private static Thread RT_WORKER;

    // ══════════════════════════════════════════════════════════
    //  LOOP SPECS — exclusive to the worker
    // ══════════════════════════════════════════════════════════
    private static final Map<String, ConfigSpec> LOOP_SPECS = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════
    //  OVERFLOW
    // ══════════════════════════════════════════════════════════
    private static final long SLOW_THRESHOLD_NS  = TimeUnit.SECONDS.toNanos(5);
    private static final long PANIC_THRESHOLD_NS = TimeUnit.SECONDS.toNanos(10);
    private static final int  OVERFLOW_LIMIT     = 3;

    private static final ExecutorService OVERFLOW_POOL = Executors.newFixedThreadPool(OVERFLOW_LIMIT, r -> {
        var t = new Thread(r, "WaterConfig-Overflow");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> OVERFLOW_ACTIVE = ConcurrentHashMap.newKeySet();
    private static volatile boolean PANIC = false;
    private static volatile long overflowFullSince = 0;

    // FORMATS
    public static final String FORMAT_PROPERTIES = "properties";
    public static final String FORMAT_CFG = "cfg";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_JSON5 = "json5";
    public static final String FORMAT_TOML = "toml";

    public static Path getPath() { return CONFIG_PATH; }
    public static void setPath(Path configPath) { CONFIG_PATH = configPath; }

    public static boolean isRegistered(String name) {
        synchronized (SPECS) {
            return SPECS.containsKey(name);
        }
    }

    public static ConfigSpec register(ConfigSpec spec) {
        synchronized (SPECS) {
            SPECS.put(spec.name(), spec);
        }

        IO_POOL.submit(() -> {
            try {
                if (!spec.load()) spec.save();
                spec.loaded = true;
                spec.dirty = false;
                LOOP_SPECS.put(spec.name(), spec);
            } catch (Exception e) {
                System.err.println("[WaterConfig] Failed to load spec '" + spec.name() + "': " + e.getMessage());
            }
        });

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
            final Class<?> specFieldType = Tools.typeOf(field);

            if (specField == null)
                continue;

            // DEFINE BY TYPE, CAN BE REPLICATED
            ConfigSpec.BaseFieldBuilder<?, ?> fieldBuilder;
            final String name = specField.value().isEmpty() ? field.getName() : specField.value();
            if (specFieldType == Boolean.class) fieldBuilder = builder.defineBoolean(name, field, instance);
            else if (specFieldType == Byte.class) {
                ConfigSpec.ByteFieldBuilder numberFieldBuilder = builder.defineByte(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minByte());
                    numberFieldBuilder.setMax(conditions.maxByte());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == Short.class) {
                ConfigSpec.ShortFieldBuilder numberFieldBuilder = builder.defineShort(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minShort());
                    numberFieldBuilder.setMax(conditions.maxShort());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == Integer.class) {
                ConfigSpec.IntFieldBuilder numberFieldBuilder = builder.defineInt(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minInt());
                    numberFieldBuilder.setMax(conditions.maxInt());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == Long.class) {
                ConfigSpec.LongFieldBuilder numberFieldBuilder = builder.defineLong(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minLong());
                    numberFieldBuilder.setMax(conditions.maxLong());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == Float.class) {
                ConfigSpec.FloatFieldBuilder numberFieldBuilder = builder.defineFloat(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minFloat());
                    numberFieldBuilder.setMax(conditions.maxFloat());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == Double.class) {
                ConfigSpec.DoubleFieldBuilder numberFieldBuilder = builder.defineDouble(name, field, instance);
                NumberConditions conditions = field.getAnnotation(NumberConditions.class);
                if (conditions != null) {
                    numberFieldBuilder.math(conditions.math());
                    numberFieldBuilder.strictMath(conditions.strictMath());
                    numberFieldBuilder.setMin(conditions.minDouble());
                    numberFieldBuilder.setMax(conditions.maxDouble());
                }

                fieldBuilder = numberFieldBuilder;
            } else if (specFieldType == String.class) {
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
            } else if (specFieldType == Path.class) {
                fieldBuilder = builder.definePath(name, field, instance);
            }
            else if (specFieldType == Character.class) fieldBuilder = builder.defineChar(name, field, instance);
            else if (specFieldType == List.class) fieldBuilder = builder.defineList(name, field, instance, Tools.subTypeOf(field));
            else if (specFieldType.isArray()) fieldBuilder = builder.defineArray(name, field, instance, Tools.subTypeOf(field));
            else if (Enum.class.isAssignableFrom(specFieldType)) fieldBuilder = builder.defineEnum(name, field, instance);
            else if (specFieldType.isAnnotationPresent(Spec.class)) {
                if (!Modifier.isFinal(specFieldType.getModifiers()))
                    throw new IllegalArgumentException("Field '" + name + "' of type '" + specFieldType.getName() + "' must be final to be used as nested Spec");

                Object nestedInstance = Tools.valueFrom(field, instance);
                Spec nestedSpec = Tools.specOf(specFieldType);

                builder.push(nestedSpec.value());
                register$iterateClass(nestedInstance, specFieldType, builder, false /* Reading an object instance field, we asume you need those */);
                builder.pop();

                continue; // SKIP END CALL BELOW
            }
            else fieldBuilder = builder.define(name, field, instance);

            // COMMENTS
            for (Comment comment: field.getAnnotationsByType(Comment.class)) {
                fieldBuilder.comments(comment.value());
            }

            // END
            fieldBuilder.end();
        }

        // THEN, ITERATE CHILD CLASSES
        for (Class<?> clazz: specClass.getDeclaredClasses()) {
            final Spec spec = Tools.specOfWeak(clazz);
            if (spec == null || spec.disableStatic()) continue; // IGNORE NOT ANNOTATED CLASSES

            Object childInstance = clazz;
            boolean childStatic = true;
            try {
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                childInstance = ctor.newInstance();
                childStatic = false;
            } catch (Exception ignored) {}

            builder.push(spec.value());
            register$iterateClass(childInstance, clazz, builder, childStatic);
            builder.pop();
        }

        // COMMENTS INTO SPEC
        for (Comment comment: specClass.getAnnotationsByType(Comment.class)) {
            builder.comments(comment.value());
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
            try {
                if (PANIC) break;

                for (ConfigSpec spec : LOOP_SPECS.values()) {
                    if (PANIC) break;
                    if (OVERFLOW_ACTIVE.contains(spec.name())) continue;

                    boolean needsSave = spec.isDirty();
                    boolean needsReload = spec.isReload();
                    if (!needsSave && !needsReload) continue;

                    if (spec.isSlow()) {
                        overflowProcess(spec);
                        continue;
                    }

                    long start = System.nanoTime();
                    doProcess(spec);
                    long elapsed = System.nanoTime() - start;

                    if (elapsed >= SLOW_THRESHOLD_NS) {
                        spec.setSlow(true);
                    }
                }

                // Verificar pánico en cada tick, independiente de si algún spec slow necesitó trabajo
                if (!OVERFLOW_ACTIVE.isEmpty()) {
                    checkPanic();
                } else {
                    overflowFullSince = 0; // overflow vacío — resetear reloj
                }

                if (!PANIC) Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void doProcess(ConfigSpec spec) {
        try {
            if (spec.isDirty()) {
                spec.save();
                // Limpiar DESPUÉS de que save() haya tenido éxito
                spec.dirty = false;
                spec.dirtyFields().clear();
            }
        } catch (Exception e) {
            // dirty sigue en true, dirtyFields intactos — retry natural en el siguiente tick
            System.err.println("[WaterConfig] Save failed for spec '" + spec.name() + "': " + e.getMessage());
        }

        try {
            if (spec.isReload()) {
                spec.load();
                // Limpiar DESPUÉS de que load() haya tenido éxito
                spec.reload = false;
            }
        } catch (Exception e) {
            // reload sigue en true — retry natural en el siguiente tick
            System.err.println("[WaterConfig] Reload failed for spec '" + spec.name() + "': " + e.getMessage());
        }
    }

    private static void overflowProcess(ConfigSpec spec) {
        if (OVERFLOW_ACTIVE.size() >= OVERFLOW_LIMIT) {
            return; // lleno — el loop principal se encarga de verificar pánico
        }

        OVERFLOW_ACTIVE.add(spec.name());
        OVERFLOW_POOL.submit(() -> {
            long start = System.nanoTime();
            try {
                doProcess(spec);

                long elapsed = System.nanoTime() - start;
                if (elapsed < SLOW_THRESHOLD_NS) {
                    spec.setSlow(false);
                }
            } finally {
                OVERFLOW_ACTIVE.remove(spec.name());
            }
        });
    }

    private static void checkPanic() {
        if (OVERFLOW_ACTIVE.size() >= OVERFLOW_LIMIT) {
            long now = System.nanoTime();
            if (overflowFullSince == 0) {
                overflowFullSince = now;
            } else if (now - overflowFullSince >= PANIC_THRESHOLD_NS) {
                PANIC = true;
                triggerPanic();
            }
        } else {
            overflowFullSince = 0;
        }
    }

    private static void triggerPanic() {
        System.err.println("[WaterConfig] PANIC: I/O overflow saturated for 10s — emergency shutdown");

        // Solo loguear qué se pierde — NO intentar save, el I/O está muerto
        for (ConfigSpec spec : LOOP_SPECS.values()) {
            if (spec.isDirty()) {
                System.err.println("[WaterConfig] PANIC: spec '" + spec.name() + "' had unsaved changes (data lost)");
            }
        }

        OVERFLOW_POOL.shutdownNow();
        IO_POOL.shutdownNow();
        LOOP_SPECS.clear();
    }

    public static void unload(String name) {
        ConfigSpec spec;
        synchronized (SPECS) {
            spec = SPECS.remove(name);
        }
        if (spec == null) return;

        // Remover del loop — el worker ya no lo toca
        LOOP_SPECS.remove(name);

        // Save final en IO_POOL, pero esperar a que overflow termine primero si aplica
        IO_POOL.submit(() -> {
            try {
                // Esperar a que el overflow termine con este spec (si está procesándolo)
                // Polling simple con timeout — el overflow debería terminar eventualmente
                long waitStart = System.nanoTime();
                long maxWait = TimeUnit.SECONDS.toNanos(15); // 5s slow threshold + margen
                while (OVERFLOW_ACTIVE.contains(name)) {
                    if (System.nanoTime() - waitStart >= maxWait) {
                        System.err.println("[WaterConfig] Timeout waiting for overflow to release spec '" + name + "' during unload");
                        break;
                    }
                    Thread.sleep(100);
                }

                if (spec.isDirty()) spec.save();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[WaterConfig] Final save failed for '" + name + "': " + e.getMessage());
            }
        });
    }

    public static void init() {
        if (RT_WORKER != null) return; // ya inicializado
        WaterConfigRegistry.init();

        RT_WORKER = new Thread(WaterConfig::run, "WaterConfig-Worker");
        RT_WORKER.setDaemon(true);
        RT_WORKER.start();

        Runtime.getRuntime().addShutdownHook(new Thread(WaterConfig::shutdown, "WaterConfig-Shutdown"));
    }

    static void shutdown() {
        RT_WORKER.interrupt();
        try {
            RT_WORKER.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (ConfigSpec spec : LOOP_SPECS.values()) {
            if (spec.isDirty()) {
                try {
                    spec.save();
                } catch (Exception e) {
                    System.err.println("[WaterConfig] Shutdown save failed for '" + spec.name() + "': " + e.getMessage());
                }
            }
        }

        IO_POOL.shutdown();
        OVERFLOW_POOL.shutdown();
        LOOP_SPECS.clear();
        synchronized (SPECS) {
            SPECS.clear();
        }
    }

}
