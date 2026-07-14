package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.ICodec;
import me.srrapero720.waterconfig.api.IComplexCodec;
import me.srrapero720.waterconfig.api.annotations.ColorConditions;
import me.srrapero720.waterconfig.api.annotations.Comment;
import me.srrapero720.waterconfig.api.annotations.ListConditions;
import me.srrapero720.waterconfig.api.annotations.NumberConditions;
import me.srrapero720.waterconfig.api.annotations.PathConditions;
import me.srrapero720.waterconfig.api.annotations.Spec;
import me.srrapero720.waterconfig.api.annotations.StringConditions;
import me.srrapero720.waterconfig.api.annotations.TimeConditions;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;

import java.util.function.Predicate;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
    private static volatile Thread RT_WORKER;
    private static volatile boolean SHUTDOWN = false;

    // FILE LOCKDOWN (FROM WaterConfigConfig), APPLIED ONCE AT BOOTSTRAP
    private static volatile WaterConfigConfig.Lockdown LOCKDOWN = WaterConfigConfig.Lockdown.OFF;
    private static volatile boolean selfConfigDone = false;

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

    /**
     * The registered spec by name, {@link ConfigSpec#refresh() refreshed} before returning so a
     * config manager fetch always sees validated, real values. Null when no spec has that name.
     */
    public static ConfigSpec spec(String name) {
        return spec(name, true);
    }

    public static ConfigSpec spec(String name, boolean refresh) {
        ConfigSpec spec;
        synchronized (SPECS) {
            spec = SPECS.get(name);
        }
        if (spec != null && refresh) {
            spec.refresh();
        }
        return spec;
    }

    /**
     * Every registered spec, {@link ConfigSpec#refresh() refreshed} before returning.
     */
    public static Collection<ConfigSpec> specs() {
        return specs(true);
    }

    public static Collection<ConfigSpec> specs(boolean refresh) {
        List<ConfigSpec> all;
        synchronized (SPECS) {
            all = List.copyOf(SPECS.values());
        }
        if (refresh) {
            all.forEach(ConfigSpec::refresh);
        }
        return all;
    }

    /**
     * The registered spec managing the given file, or null. A pure lookup — no refresh.
     */
    public static ConfigSpec specOf(Path path) {
        synchronized (SPECS) {
            for (ConfigSpec spec : SPECS.values()) {
                if (samePath(spec.path(), path)) {
                    return spec;
                }
            }
        }
        return null;
    }

    private static boolean samePath(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    public static ConfigSpec register(ConfigSpec spec) {
        ensureSelfConfig();
        if (PANIC || SHUTDOWN) {
            failSpec(spec, new IllegalStateException("WaterConfig is shut down, spec '" + spec.name() + "' cannot be registered"));
            return spec;
        }

        synchronized (SPECS) {
            // INTRA-PROCESS EXCLUSIVITY: TWO DIFFERENT SPECS MAY NOT MANAGE THE SAME FILE (THEY WOULD
            // CLOBBER EACH OTHER). RE-REGISTERING THE SAME NAME OVERWRITES, AS BEFORE.
            for (ConfigSpec other : SPECS.values()) {
                if (other != spec && !other.name().equals(spec.name()) && samePath(other.path(), spec.path())) {
                    failSpec(spec, new IllegalStateException("File '" + spec.path() + "' is already managed by spec '" + other.name() + "'"));
                    return spec;
                }
            }
            SPECS.put(spec.name(), spec);
        }

        try {
            IO_POOL.submit(() -> {
                spec.status = ConfigSpec.Status.LOADING;
                try {
                    if (!spec.load()) spec.save();
                    spec.dirty = false;
                    spec.status = ConfigSpec.Status.LOADED;
                } catch (Throwable e) {
                    spec.loadError = e;
                    spec.status = ConfigSpec.Status.FAILED;
                    System.err.println("[WaterConfig] Failed to load spec '" + spec.name() + "': " + e.getMessage());
                } finally {
                    // EVEN A FAILED SPEC ENTERS THE LOOP SO A LATER save()/reload CAN RECOVER IT
                    LOOP_SPECS.put(spec.name(), spec);
                    synchronized (spec) {
                        spec.notifyAll();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            // SHUTDOWN RACED THE CHECK ABOVE
            failSpec(spec, new IllegalStateException("WaterConfig is shut down, spec '" + spec.name() + "' cannot be registered", e));
        }

        return spec;
    }

    // PUBLISHES A TERMINAL FAILED STATE SO registerBlocking NEVER HANGS ON A DEAD REGISTRATION
    private static void failSpec(ConfigSpec spec, Throwable error) {
        System.err.println("[WaterConfig] " + error.getMessage());
        spec.loadError = error;
        spec.status = ConfigSpec.Status.FAILED;
        synchronized (spec) {
            spec.notifyAll();
        }
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

    public static ConfigSpec registerBlocking(Object instance) {
        return awaitTerminal(register(instance));
    }

    public static ConfigSpec registerBlocking(Class<?> clazz) {
        return awaitTerminal(register(clazz));
    }

    // WAITS ON THE SPEC MONITOR FOR A TERMINAL STATE, RETHROWS THE LOAD ERROR ON FAILED
    private static ConfigSpec awaitTerminal(ConfigSpec spec) {
        synchronized (spec) {
            while (spec.status() != ConfigSpec.Status.LOADED && spec.status() != ConfigSpec.Status.FAILED) {
                try {
                    spec.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted while waiting for config to load", e);
                }
            }
        }
        if (spec.status() == ConfigSpec.Status.FAILED) {
            throw new RuntimeException("Failed to load spec '" + spec.name() + "'", spec.loadError());
        }
        return spec;
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
                ConfigSpec.PathFieldBuilder pathFieldBuilder = builder.definePath(name, field, instance);
                PathConditions conditions = field.getAnnotation(PathConditions.class);
                if (conditions != null) {
                    pathFieldBuilder.runtimePath(conditions.runtimePath());
                    pathFieldBuilder.fileExists(conditions.existsFile());
                    pathFieldBuilder.hardfail(conditions.hardfail());
                }

                fieldBuilder = pathFieldBuilder;
            }
            else if (specFieldType == java.awt.Color.class) {
                ConfigSpec.ColorFieldBuilder colorFieldBuilder = builder.defineColor(name, field, instance);
                ColorConditions conditions = field.getAnnotation(ColorConditions.class);
                if (conditions != null) {
                    colorFieldBuilder.radix(conditions.value());
                }
                fieldBuilder = colorFieldBuilder;
            }
            else if (specFieldType == Character.class) fieldBuilder = builder.defineChar(name, field, instance);
            else if (specFieldType == List.class) {
                ConfigSpec.ListFieldBuilder<?> listFieldBuilder = builder.defineList(name, field, instance, Tools.subTypeOf(field));
                applyListConditions(listFieldBuilder, field);
                fieldBuilder = listFieldBuilder;
            }
            else if (specFieldType.isArray()) {
                ConfigSpec.ArrayFieldBuilder<?> arrayFieldBuilder = builder.defineArray(name, field, instance, Tools.subTypeOf(field));
                applyListConditions(arrayFieldBuilder, field);
                fieldBuilder = arrayFieldBuilder;
            }
            else if (Enum.class.isAssignableFrom(specFieldType)) fieldBuilder = builder.defineEnum(name, field, instance);
            else if (specFieldType.isAnnotationPresent(Spec.class)) {
                if (!Modifier.isFinal(field.getModifiers()))
                    throw new IllegalArgumentException("Field '" + name + "' of type '" + specFieldType.getName() + "' must be final to be used as nested Spec");

                Object nestedInstance = Tools.valueFrom(field, instance);
                Spec nestedSpec = Tools.specOf(specFieldType);

                builder.push(nestedSpec.value());
                register$iterateClass(nestedInstance, specFieldType, builder, false /* Reading an object instance field, we asume you need those */);
                builder.pop();

                continue; // SKIP END CALL BELOW
            }
            else {
                TimeConditions time = field.getAnnotation(TimeConditions.class);
                if (time != null && COMPARABLE_TEMPORALS.contains(specFieldType)) {
                    fieldBuilder = defineTemporal(builder, name, field, instance, specFieldType, time);
                } else {
                    fieldBuilder = builder.define(name, field, instance);
                }
            }

            // CONTROL — FALLS BACK TO THE FIELD TYPE DEFAULT WHEN NOT CHOSEN
            fieldBuilder.control(specField.control());

            // SUFFIX — DISPLAY-ONLY HINT FOR EXTERNAL CONFIG UIS
            fieldBuilder.suffix(specField.suffix());

            // ALIASES — FORMER NAMES PICKED UP FROM OLD FILES ON LOAD
            fieldBuilder.aliases(specField.aliases());

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

    // TEMPORAL TYPES THAT ARE TOTALLY ORDERED, SO @TimeConditions from/to BOUNDS APPLY (Period IS NOT)
    private static final Set<Class<?>> COMPARABLE_TEMPORALS = Set.of(
            Duration.class, Instant.class, LocalDate.class, LocalTime.class,
            LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class);

    // BUILDS A TEMPORAL FIELD WITH @TimeConditions from/to BOUNDS PARSED VIA THE TYPE'S CODEC
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ConfigSpec.BaseFieldBuilder<?, ?> defineTemporal(ConfigSpec.SpecBuilder builder, String name, Field field, Object instance, Class<?> type, TimeConditions time) {
        ConfigSpec.TemporalFieldBuilder tb = builder.defineTemporal(name, field, instance, (Class) type);
        if (!time.from().isEmpty()) {
            tb.from((Comparable) parseElement(time.from(), type));
        }
        if (!time.to().isEmpty()) {
            tb.to((Comparable) parseElement(time.to(), type));
        }
        return tb;
    }

    // COPIES @ListConditions OPTIONS INTO A COLLECTION FIELD BUILDER
    @SuppressWarnings({"unchecked", "rawtypes", "removal"})
    private static void applyListConditions(ConfigSpec.CollectionFieldBuilder<?, ?, ?> builder, Field field) {
        ListConditions conditions = field.getAnnotation(ListConditions.class);
        if (conditions == null) return;

        builder.stringify(conditions.stringify());
        builder.singleline(conditions.singleline());
        builder.allowEmpty(conditions.allowEmpty());
        builder.unique(conditions.unique());
        builder.limit(conditions.limit());
        if (conditions.filter() != Predicate.class) {
            ((ConfigSpec.CollectionFieldBuilder) builder).filter(conditions.filter());
        }
    }

    /**
     * Builds a runtime spec from a config file you have no class or spec for: values become
     * the field defaults, dotted keys become groups and comments are preserved for save-back.
     *
     * <p>Parsing is always strict — without an original spec there is nothing to contrast a
     * repair against, so a file that does not parse cleanly returns null instead of guessing.</p>
     *
     * <p>The spec is named after the file stem and is NOT auto-persisted nor worker-managed;
     * the caller decides when to save.</p>
     *
     * @return the reconstructed spec, or null when the extension is unknown or the file cannot be parsed
     */
    public static ConfigSpec reverseSpec(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");

        // ALREADY MANAGED: HAND BACK THE REAL SPEC INSTEAD OF FABRICATING A DUPLICATE MANAGER
        // TODO: warn (no logger yet) that reverseSpec is not how to fetch a registered spec — use WaterConfig.specOf(path)
        ConfigSpec managed = specOf(path);
        if (managed != null) {
            return managed;
        }

        // FORMAT IS PICKED FROM THE FILE EXTENSION
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        IFormatCodec format = FORMATS.get(fileName.substring(dot + 1).toLowerCase());
        if (format == null) {
            return null;
        }

        // ALWAYS STRICT: A BROKEN FILE RETURNS NULL, NEVER A GUESSED REPAIR
        Map<String, Object> entries;
        Map<String, List<String>> comments;
        try (IFormatReader reader = format.createReader(path)) {
            entries = new LinkedHashMap<>(reader.entries());
            comments = new LinkedHashMap<>(reader.comments());
        } catch (Exception e) {
            return null;
        }

        // SPLIT DOTTED KEYS INTO A GROUP TREE; A KEY THAT CONFLICTS WITH A GROUP IS DROPPED
        LinkedHashMap<String, Object> tree = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry: entries.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            LinkedHashMap<String, Object> node = tree;
            for (int p = 0; p < parts.length - 1 && node != null; p++) {
                Object child = node.computeIfAbsent(parts[p], k -> new LinkedHashMap<String, Object>());
                node = (child instanceof LinkedHashMap<?, ?> map) ? (LinkedHashMap<String, Object>) map : null;
            }
            if (node == null || node.get(parts[parts.length - 1]) instanceof LinkedHashMap<?, ?>) {
                System.err.println("[WaterConfig] reverseSpec: key '" + entry.getKey() + "' conflicts with a group, dropped");
                continue;
            }
            node.put(parts[parts.length - 1], entry.getValue());
        }

        ConfigSpec.SpecBuilder builder = new ConfigSpec.SpecBuilder(fileName.substring(0, dot), format, path, 0);
        List<String> fileComments = comments.get("");
        if (fileComments != null) {
            builder.comments(fileComments.toArray(new String[0]));
        }
        reverseSpec$define(builder, tree, comments, "");

        ConfigSpec spec = builder.build();
        // PARSED VALUES ARE THE LOADED STATE: NOTHING PENDING, NOTHING WORKER-MANAGED
        spec.dirty = false;
        spec.status = ConfigSpec.Status.LOADED;
        return spec;
    }

    /**
     * @see #reverseSpec(Path)
     */
    public static ConfigSpec reverseSpec(java.io.File file) {
        Objects.requireNonNull(file, "File cannot be null");
        return reverseSpec(file.toPath());
    }

    // DEFINES EVERY TREE NODE: GROUPS RECURSE, LEAVES INFER THEIR TYPE FROM THE VALUE SHAPE
    private static void reverseSpec$define(ConfigSpec.SpecBuilder builder, LinkedHashMap<String, Object> node, Map<String, List<String>> comments, String prefix) {
        for (Map.Entry<String, Object> entry: node.entrySet()) {
            String key = entry.getKey();
            String fullKey = prefix + key;
            List<String> entryComments = comments.get(fullKey);

            if (entry.getValue() instanceof LinkedHashMap<?, ?> child) {
                builder.push(key);
                if (entryComments != null) {
                    builder.comments(entryComments.toArray(new String[0]));
                }
                reverseSpec$define(builder, (LinkedHashMap<String, Object>) child, comments, fullKey + ".");
                builder.pop();
                continue;
            }

            ConfigSpec.BaseFieldBuilder<?, ?> fieldBuilder;
            if (entry.getValue() instanceof String[] array) {
                fieldBuilder = reverseSpec$defineList(builder, key, array);
            } else {
                String value = (String) entry.getValue();
                fieldBuilder = switch (inferKind(value)) {
                    case BOOLEAN -> builder.defineBoolean(key, Boolean.parseBoolean(value));
                    case INT -> builder.defineInt(key, Integer.parseInt(value));
                    case LONG -> builder.defineLong(key, Long.parseLong(value));
                    case DOUBLE -> builder.defineDouble(key, Double.parseDouble(value));
                    case STRING -> builder.defineString(key, value);
                };
            }
            if (entryComments != null) {
                fieldBuilder.comments(entryComments.toArray(new String[0]));
            }
            fieldBuilder.end();
        }
    }

    // LISTS INFER THE NARROWEST COMMON ELEMENT TYPE ACROSS ALL VALUES
    private static ConfigSpec.BaseFieldBuilder<?, ?> reverseSpec$defineList(ConfigSpec.SpecBuilder builder, String key, String[] array) {
        Kind common = null;
        for (String element: array) {
            Kind kind = inferKind(element);
            common = (common == null) ? kind : switch (common) {
                case BOOLEAN -> kind == Kind.BOOLEAN ? Kind.BOOLEAN : Kind.STRING;
                case INT -> kind == Kind.INT ? Kind.INT : (kind == Kind.LONG ? Kind.LONG : (kind == Kind.DOUBLE ? Kind.DOUBLE : Kind.STRING));
                case LONG -> (kind == Kind.INT || kind == Kind.LONG) ? Kind.LONG : (kind == Kind.DOUBLE ? Kind.DOUBLE : Kind.STRING);
                case DOUBLE -> (kind == Kind.INT || kind == Kind.LONG || kind == Kind.DOUBLE) ? Kind.DOUBLE : Kind.STRING;
                case STRING -> Kind.STRING;
            };
        }

        if (common == null) {
            return builder.defineList(key, new ArrayList<String>(), String.class);
        }
        return switch (common) {
            case BOOLEAN -> builder.defineList(key, collect(array, Boolean::parseBoolean), Boolean.class);
            case INT -> builder.defineList(key, collect(array, Integer::parseInt), Integer.class);
            case LONG -> builder.defineList(key, collect(array, Long::parseLong), Long.class);
            case DOUBLE -> builder.defineList(key, collect(array, Double::parseDouble), Double.class);
            case STRING -> builder.defineList(key, collect(array, s -> s), String.class);
        };
    }

    private static <T> List<T> collect(String[] array, java.util.function.Function<String, T> parser) {
        List<T> list = new ArrayList<>(array.length);
        for (String element: array) {
            list.add(parser.apply(element));
        }
        return list;
    }

    private enum Kind { BOOLEAN, INT, LONG, DOUBLE, STRING }

    // TYPE INFERENCE BY VALUE SHAPE: true/false, INTEGRAL, DECIMAL, ELSE STRING
    private static Kind inferKind(String value) {
        if (value.equals("true") || value.equals("false")) {
            return Kind.BOOLEAN;
        }
        if (value.matches("[+-]?\\d+")) {
            try {
                long parsed = Long.parseLong(value);
                return (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) ? Kind.INT : Kind.LONG;
            } catch (NumberFormatException e) {
                return Kind.STRING; // BEYOND LONG RANGE
            }
        }
        if (value.matches("[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?") ) {
            return Kind.DOUBLE;
        }
        return Kind.STRING;
    }

    /**
     * Resolves the registered codec for the given type, boxing primitives.
     *
     * @return the codec, or null when none matches
     */
    public static ICodec<?> codecOf(Class<?> type) {
        Class<?> boxed = toBoxed(type);
        ICodec<?> codec = CODECS.get(boxed);
        if (codec != null) {
            return codec;
        }

        // SUBTYPE LOOKUPS (ENUMS, RECORDS, Path IMPLS) SCAN THE REGISTRY ONCE AND ARE CACHED
        codec = CODEC_CACHE.get(boxed);
        if (codec == null) {
            for (ICodec<?> c: CODECS.values()) {
                if (c.type().isAssignableFrom(boxed)) {
                    CODEC_CACHE.put(boxed, c);
                    return c;
                }
            }
        }
        return codec;
    }

    private static final Map<Class<?>, ICodec<?>> CODEC_CACHE = new ConcurrentHashMap<>();

    static String[] tryEncode(Object[] value, Class<?> type, Class<?> subType) {
        if (value instanceof String[] s) {
            return s;
        }

        // MIRRORS tryParse: List<String>.toArray() YIELDS Object[], COPY INTO String[]
        if (subType == String.class) {
            String[] result = new String[value.length];
            System.arraycopy(value, 0, result, 0, value.length);
            return result;
        }

        ICodec<Object> codec = (ICodec<Object>) codecOf(subType);
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

        ICodec<Object> codec = (ICodec<Object>) codecOf(value.getClass());
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

        ICodec<T> codec = (ICodec<T>) codecOf(subType);
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

        ICodec<T> codec = (ICodec<T>) codecOf(type);
        if (codec == null)
            throw new IllegalArgumentException("Codec for type '" + type.getName() + "' was not founded");

        if (codec instanceof IComplexCodec<T, ?> complexCodec) {
            return ((IComplexCodec<T, T2>) complexCodec).decode(value, type2);
        }
        return codec.decode(value);
    }

    // DECODES ONE VALUE TO type VIA ITS CODEC; ON FAILURE ATTEMPTS A REPAIR COERCION TO THAT TYPE.
    // RETURNS null WHEN NEITHER THE CODEC NOR THE REPAIR CAN PRODUCE A VALUE.
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object parseElement(String raw, Class<?> type) {
        if (raw == null) return null;
        if (type == String.class) return raw;

        ICodec<?> codec = codecOf(type);
        if (codec != null) {
            try {
                Object v = (codec instanceof IComplexCodec complex) ? complex.decode(raw, type) : codec.decode(raw);
                if (v != null) return v;
            } catch (Exception ignored) {
                // CODEC REJECTED THE RAW VALUE — FALL THROUGH TO REPAIR
            }
        }
        return coerce(raw, type);
    }

    // REPAIR COERCION: RECOVERS A CODEC-REJECTED VALUE BY CONVERTING IT TO THE TARGET TYPE.
    // NUMERIC TARGETS ACCEPT DECIMAL/EXPONENT STRINGS (TRUNCATED TO FIT INTEGRALS), BOOLEAN
    // TARGETS ACCEPT COMMON TRUTHY/FALSY SPELLINGS, STRING TARGETS TAKE ANYTHING VERBATIM.
    private static Object coerce(String raw, Class<?> type) {
        String s = raw.trim();
        if (s.isEmpty()) return null;

        if (type == String.class) return raw;

        if (type == Boolean.class) {
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "true", "1", "yes", "on", "y" -> Boolean.TRUE;
                case "false", "0", "no", "off", "n" -> Boolean.FALSE;
                default -> null;
            };
        }

        if (type == Integer.class || type == Long.class || type == Short.class
                || type == Byte.class || type == Double.class || type == Float.class) {
            double d;
            try {
                d = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
            if (type == Double.class) return d;
            if (type == Float.class) return (float) d;
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            long l = (long) d; // TRUNCATE TOWARD ZERO
            if (type == Long.class) return l;
            if (type == Integer.class) return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : null;
            if (type == Short.class) return (l >= Short.MIN_VALUE && l <= Short.MAX_VALUE) ? (short) l : null;
            return (l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE) ? (byte) l : null;
        }
        return null;
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

                // CHECK PANIC EVERY TICK, INDEPENDENT OF WHETHER ANY SLOW SPEC NEEDED WORK
                if (!OVERFLOW_ACTIVE.isEmpty()) {
                    checkPanic();
                } else {
                    overflowFullSince = 0; // OVERFLOW EMPTY — RESET THE CLOCK
                }

                if (!PANIC) Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void doProcess(ConfigSpec spec) {
        synchronized (spec.ioLock) {
            if (spec.isDirty()) {
                // SNAPSHOT-AND-CLEAR BEFORE SERIALIZING: A CONCURRENT set() RE-MARKS AND IS PICKED NEXT TICK
                spec.dirty = false;
                try {
                    spec.save();
                } catch (Exception e) {
                    spec.dirty = true; // RETRY NEXT TICK
                    System.err.println("[WaterConfig] Save failed for spec '" + spec.name() + "': " + e.getMessage());
                }
            }

            if (spec.isReload()) {
                spec.reload = false;
                try {
                    spec.load();
                } catch (Exception e) {
                    spec.reload = true; // RETRY NEXT TICK
                    System.err.println("[WaterConfig] Reload failed for spec '" + spec.name() + "': " + e.getMessage());
                }
            }
        }
    }

    private static void overflowProcess(ConfigSpec spec) {
        if (OVERFLOW_ACTIVE.size() >= OVERFLOW_LIMIT) {
            return; // FULL — THE MAIN LOOP HANDLES THE PANIC CHECK
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

        // ONLY LOG WHAT IS LOST — DO NOT ATTEMPT A SAVE, THE I/O IS DEAD
        for (ConfigSpec spec : LOOP_SPECS.values()) {
            if (spec.isDirty()) {
                System.err.println("[WaterConfig] PANIC: spec '" + spec.name() + "' had unsaved changes (data lost)");
            }
        }

        OVERFLOW_POOL.shutdownNow();
        IO_POOL.shutdownNow();
        LOOP_SPECS.clear();
    }

    /**
     * Unregisters every spec, discarding pending changes and waiting out in-flight saves.
     * Intended for controlled teardown scenarios where nothing must touch the files afterwards.
     */
    public static void unloadAll() {
        LOOP_SPECS.clear();
        List<ConfigSpec> specs;
        synchronized (SPECS) {
            specs = List.copyOf(SPECS.values());
            SPECS.clear();
        }
        // HOLDING EACH ioLock WAITS FOR ANY IN-FLIGHT SAVE BEFORE DISCARDING ITS DIRTY STATE
        for (ConfigSpec spec: specs) {
            synchronized (spec.ioLock) {
                spec.dirty = false;
                spec.reload = false;
            }
        }
    }

    public static void unload(String name) {
        ConfigSpec spec;
        synchronized (SPECS) {
            spec = SPECS.remove(name);
        }
        if (spec == null) return;

        // REMOVE FROM THE LOOP — THE WORKER NO LONGER TOUCHES IT
        LOOP_SPECS.remove(name);

        if (PANIC || SHUTDOWN) {
            System.err.println("[WaterConfig] Skipping final save for '" + name + "': WaterConfig is shut down");
            return;
        }

        try {
            // FINAL SAVE; THE PER-SPEC ioLock WAITS FOR ANY IN-FLIGHT OVERFLOW SAVE WITHOUT BUSY-WAITING
            IO_POOL.submit(() -> {
                try {
                    synchronized (spec.ioLock) {
                        if (spec.isDirty()) {
                            spec.dirty = false;
                            spec.save();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[WaterConfig] Final save failed for '" + name + "': " + e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            System.err.println("[WaterConfig] Skipping final save for '" + name + "': WaterConfig is shut down");
        }
    }

    // TRUE WHEN CONFIG FILES ARE LOCKED DOWN (SOFT OR HARD): RUNTIME RELOAD IS DISABLED
    public static boolean isLockdown() {
        return LOCKDOWN != WaterConfigConfig.Lockdown.OFF;
    }

    // APPLIES THE LOCKDOWN MODE FROM WaterConfigConfig. HARD IS NOT IMPLEMENTED YET (SEE HARD_LOCKDOWN.md)
    static void applyLockdown(WaterConfigConfig.Lockdown mode) {
        LOCKDOWN = (mode == null) ? WaterConfigConfig.Lockdown.OFF : mode;
        if (LOCKDOWN == WaterConfigConfig.Lockdown.I_READ_THE_COMMENT_HARD) {
            System.err.println("[WaterConfig] HARD lockdown is not implemented yet — applying SOFT (reload disabled). See HARD_LOCKDOWN.md");
        }
    }

    // DOGFOOD BOOTSTRAP: THE FIRST registration BRINGS UP WaterConfig's OWN CONFIG AND APPLIES LOCKDOWN.
    // LAZY (NOT IN init()) SO THE CONFIG PATH IS ALREADY SET BY THE TIME THE FIRST SPEC IS REGISTERED.
    private static synchronized void ensureSelfConfig() {
        if (selfConfigDone) {
            return;
        }
        selfConfigDone = true; // SET FIRST SO THE WaterConfigConfig REGISTRATION BELOW DOES NOT RE-ENTER
        registerBlocking(WaterConfigConfig.class);
        applyLockdown(WaterConfigConfig.lockdownConfigFiles);
    }

    public static synchronized void init() {
        if (RT_WORKER != null) return; // ALREADY INITIALIZED
        WaterConfigRegistry.init();

        RT_WORKER = new Thread(WaterConfig::run, "WaterConfig-Worker");
        RT_WORKER.setDaemon(true);
        RT_WORKER.start();

        Runtime.getRuntime().addShutdownHook(new Thread(WaterConfig::shutdown, "WaterConfig-Shutdown"));
    }

    static void shutdown() {
        SHUTDOWN = true;

        Thread worker = RT_WORKER;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // QUIESCE THE POOLS BEFORE THE FINAL SAVES SO DAEMON THREADS NEVER DIE MID-WRITE
        IO_POOL.shutdown();
        OVERFLOW_POOL.shutdown();
        try {
            if (!IO_POOL.awaitTermination(5, TimeUnit.SECONDS)) IO_POOL.shutdownNow();
            if (!OVERFLOW_POOL.awaitTermination(5, TimeUnit.SECONDS)) OVERFLOW_POOL.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (ConfigSpec spec : LOOP_SPECS.values()) {
            synchronized (spec.ioLock) {
                if (spec.isDirty()) {
                    spec.dirty = false;
                    try {
                        spec.save();
                    } catch (Exception e) {
                        System.err.println("[WaterConfig] Shutdown save failed for '" + spec.name() + "': " + e.getMessage());
                    }
                }
            }
        }

        LOOP_SPECS.clear();
        synchronized (SPECS) {
            SPECS.clear();
        }
    }

}
