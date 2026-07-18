package me.srrapero720.waterconfig;

import me.srrapero720.waterconfig.api.Control;
import me.srrapero720.waterconfig.api.IConfigField;
import me.srrapero720.waterconfig.api.IStructuredField;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;
import me.srrapero720.waterconfig.api.annotations.ColorConditions;
import me.srrapero720.waterconfig.impl.fields.*;
import me.srrapero720.waterconfig.impl.formats.special.MathEvaluator;

import java.awt.Color;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;

import static me.srrapero720.waterconfig.WaterConfigRegistry.FORMATS;

public final class ConfigSpec extends ConfigGroup {
    // NUMBER COMMENTS
    private static final String COMMENT_ALLOWS_MATH = "Allows math expressions";
    private static final String COMMENT_ALLOWS_MATH_STRICT = ", hard fails on invalid math expressions";
    private static final String COMMENT_MUST_BE_IN_RANGE = "Value must be less than '%s' and greater than '%s'";
    private static final String COMMENT_MUST_BE_LESS_THAN = "Value must be less than %s";
    private static final String COMMENT_MUST_BE_GREATER_THAN = "Value must be greater than %s";

    // STRING COMMENTS
    private static final String COMMENT_ALLOW_EMPTY = "Allows (non-null) empty values";
    private static final String COMMENT_DENY_EMPTY = "Must not be empty";
    private static final String COMMENT_MUST_START_WITH = "Value must start with %s";
    private static final String COMMENT_MUST_END_WITH = "Value must end with %s";
    private static final String COMMENT_MUST_START_AND_END_WITH = "Value must start with %s and end with %s";
    private static final String COMMENT_MUST_CONTAIN = "Value must contain %s";
    private static final String COMMENT_MUST_NOT_CONTAIN = "Value must not contain %s";
    private static final String COMMENT_MUST_EQUALS = "Value must match %s";
    private static final String COMMENT_MUST_NOT_EQUALS = "Value must not match %s";
    private static final String COMMENT_MUST_MATCH = "Value must match with %s";
    private static final String COMMENT_MUST_NOT_MATCH = "Value must not match with %s";

    // LIST COMMENTS
    private static final String COMMENT_ARRAY_ALLOW_EMPTY = "Allows empty arrays";
    private static final String COMMENT_ARRAY_DENY_EMPTY = "Must not be empty";
    private static final String COMMENT_ARRAY_VALUES_MUST_BE_UNIQUE = "and values must be unique";
    private static final String COMMENT_ARRAY_SIZE_MUST_BE_GREATER_THAN = "Array size must be greater %s";

    // ENUM COMMENTS
    private static final String COMMENT_ENUM_VALID_VALUES = "Accepted values are: %s";

    // PATH COMMENTS
    private static final String COMMENT_PATH_RUNTIME = "path must be a runtime path";
    private static final String COMMENT_PATH_STATIC = "path must be a static path";
    private static final String COMMENT_PATH_FILE_EXISTS = "File should exists";
    private static final String COMMENT_PATH_HARD_FAIL = "Hard fail";
    private static final String COMMENT_PATH_SOFT_FAIL = "Soft fail";

    private final IFormatCodec format;
    private final String fileSuffix;
    private final Path filePath;
    private final int backups;
    // PREVIOUS-FORMAT RECOVERY SOURCE, NULL WHEN THE SPEC HAS NOTHING OLD TO RECOVER FROM
    private IFormatCodec oldFormat;
    private Path oldPath;
    // TRUE WHILE THE IN-MEMORY STATE DESCENDS FROM THE OLD FILE: THE NEXT SUCCESSFUL SAVE
    // COMPLETES THE MIGRATION AND DELETES THE OLD FILE (GUARDED BY ioLock)
    private boolean oldCleanup;
    // GUARDS FILE IO AND THE {dirty, reload, dirtyFields} TRANSITIONS AGAINST CONCURRENT SAVES/LOADS
    final Object ioLock = new Object();
    volatile boolean dirty;
    volatile boolean reload;
    volatile boolean slow;
    volatile Status status = Status.UNLOADED;
    volatile Throwable loadError;

    private ConfigSpec(String name, IFormatCodec format, String fileSuffix, Path path, int backups) {
        super(name, null);
        this.format = format;
        this.fileSuffix = fileSuffix;
        this.filePath = path;
        this.backups = backups;
        this.dirty = true;
    }

    /**
     * Lifecycle of a spec registration. Terminal states are {@link #LOADED} and {@link #FAILED}.
     */
    public enum Status {
        UNLOADED, LOADING, LOADED, FAILED
    }

    @Override
    public String id() {
        return this.name() + ":";
    }

    @Override
    public ConfigSpec spec() {
        return this;
    }

    public void markDirty(IConfigField<?, ?> field) {
        if (field.spec() != this)
            throw new IllegalArgumentException("ConfigField requires to be updated by the intended spec");
        this.dirty = true;
    }

    public Path path() {
        return this.filePath;
    }

    IFormatCodec format() {
        return this.format;
    }

    IFormatCodec oldFormat() {
        return this.oldFormat;
    }

    Path oldPath() {
        return this.oldPath;
    }

    String fileSuffix() {
        return this.fileSuffix;
    }

    int backups() {
        return this.backups;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isLoaded() {
        return this.status == Status.LOADED;
    }

    public Status status() {
        return this.status;
    }

    /**
     * The failure that moved this spec into {@link Status#FAILED}, null otherwise.
     */
    public Throwable loadError() {
        return this.loadError;
    }

    public boolean isReload() {
        return this.reload;
    }

    public void setReload(boolean reload) {
        // SOFT LOCKDOWN: EXTERNAL RELOADS ARE DISABLED, SO HOT-EDITS NEVER TAKE EFFECT
        if (reload && WaterConfig.isLockdown()) {
            return;
        }
        this.reload = reload;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Re-validates every field from the base, resetting any that no longer meet their conditions.
     * Intended for a config manager to refresh a spec after external (reflect-mode) field mutations,
     * which the spec is otherwise unaware of.
     */
    public void refresh() {
        this.refresh(this);
    }

    private void refresh(ConfigGroup group) {
        for (IConfigField<?, ?> field: group.getFields()) {
            if (field instanceof ConfigGroup g) {
                this.refresh(g);
            } else {
                field.validate();
            }
        }
    }

    boolean isSlow() {
        return this.slow;
    }

    void setSlow(boolean slow) {
        this.slow = slow;
    }

    // TODO: must return a boolean or just throw instead?
    boolean load() throws IOException {
        synchronized (this.ioLock) {
            // OLD-FORMAT RECOVERY: WHEN THE CURRENT-FORMAT FILE IS MISSING, THE SAME SPEC FILE IS
            // SEARCHED WITH THE OLD FORMAT EXTENSION AND ITS SETTINGS RECOVERED (OLD FILE UNTOUCHED)
            IFormatCodec source = this.format;
            Path sourcePath = this.filePath;
            boolean recovered = false;
            if (!Files.exists(sourcePath)) {
                if (this.oldFormat == null || !Files.exists(this.oldPath)) {
                    return false;
                }
                source = this.oldFormat;
                sourcePath = this.oldPath;
                recovered = true;
            }
            // REPAIR MODE: THE SPEC VALIDATES EVERY RECOVERED VALUE, SO FORMAT ERRORS
            // BECOME LOGGED RESYNC POINTS INSTEAD OF ABORTING THE WHOLE LOAD
            try (IFormatReader reader = source.createReader(sourcePath, IFormatCodec.ParseMode.REPAIR)) {
                this.load(this, reader);

                // UNKNOWN FILE KEYS (ALIASES INCLUDED) JOIN THE READER'S RECOVERY REPORT
                Set<String> known = new HashSet<>();
                this.collectKeys(this, "", known);
                for (String key : reader.entries().keySet()) {
                    if (!known.contains(key)) {
                        reader.report().add(new IFormatCodec.RepairEntry(0, "unknown key '" + key + "'", "ignored"));
                    }
                }
                for (IFormatCodec.RepairEntry entry : reader.report()) {
                    System.err.println("[WaterConfig] Recovered while loading '" + this.name() + "': " + entry);
                }
            }
            this.reload = false;
            // RECOVERED SETTINGS ONLY EXIST IN MEMORY: STAY DIRTY, KEEP LOADED UNPUBLISHED AND
            // REPORT THE CURRENT-FORMAT FILE AS MISSING SO THE CALLER PERSISTS THE MIGRATION
            if (recovered) {
                this.dirty = true;
                this.oldCleanup = true;
                return false;
            }
            this.oldCleanup = false; // A CURRENT-FORMAT LOAD SUPERSEDES ANY PENDING MIGRATION CLEANUP
            this.status = Status.LOADED;
            return true;
        }
    }

    // COLLECTS EVERY DOTTED KEY THE SPEC UNDERSTANDS, ALIASES INCLUDED
    private void collectKeys(ConfigGroup group, String prefix, Set<String> known) {
        for (IConfigField<?, ?> field: group.getFields()) {
            if (field instanceof ConfigGroup g) {
                this.collectKeys(g, prefix + g.name() + ".", known);
                continue;
            }
            known.add(prefix + field.name());
            for (String alias : field.aliases()) {
                known.add(prefix + alias);
                known.add(alias); // ALIASES ALSO RESOLVE FROM THE FILE ROOT
            }
        }
    }

    // ALIAS LOOKUP: TRIES THE FIELD'S GROUP FIRST, THEN THE ALIAS AS AN ABSOLUTE KEY FROM THE
    // FILE ROOT, SO RESTRUCTURED SPECS STILL PICK VALUES OUT OF FLAT (PRE-GROUPING) OLD FILES
    private static String readAlias(IFormatReader reader, String alias) {
        String value = reader.read(alias);
        return value != null ? value : (reader.entries().get(alias) instanceof String s ? s : null);
    }

    private static String[] readAliasArray(IFormatReader reader, String alias) {
        String[] values = reader.readArray(alias);
        return values != null ? values : (reader.entries().get(alias) instanceof String[] s ? s : null);
    }

    private void load(ConfigGroup group, IFormatReader reader) {
        for (IConfigField<?, ?> field: group.getFields()) {
            if (field instanceof ConfigGroup g) {
                reader.push(g.name());
                this.load(g, reader);
                reader.pop();
                continue;
            }

            if (field instanceof IStructuredField sf) {
                // SELF-SERIALIZING FIELD (E.G. COLOR AS A GROUP): DELEGATE WITH PER-FIELD ISOLATION
                try {
                    sf.readSelf(reader);
                } catch (Exception e) {
                    System.err.println("[WaterConfig] Invalid value for field '" + field.id() + "', reset to default: " + e.getMessage());
                    field.reset();
                }
                continue;
            }

            if (field instanceof CollectionField<?,?> collectionField) {
                // PER-FIELD ISOLATION: ONE BAD VALUE RESETS THAT FIELD, NEVER ABORTS THE WHOLE LOAD
                try {
                    String[] values = reader.readArray(field.name());
                    // RENAMED FIELDS PICK UP VALUES FROM THEIR FORMER NAMES
                    for (int a = 0; values == null && a < field.aliases().length; a++) {
                        values = readAliasArray(reader, field.aliases()[a]);
                    }
                    if (values == null && collectionField.stringify) {
                        // STRINGIFY SUGAR: THE COLLECTION WAS SAVED AS ONE COMMA-SEPARATED STRING
                        String raw = reader.read(field.name());
                        for (int a = 0; raw == null && a < field.aliases().length; a++) {
                            raw = readAlias(reader, field.aliases()[a]);
                        }
                        if (raw != null) {
                            if (raw.isBlank()) {
                                values = new String[0];
                            } else {
                                values = raw.split(",");
                                for (int i = 0; i < values.length; i++) {
                                    values[i] = values[i].trim();
                                }
                            }
                        }
                    }
                    if (values != null) {
                        // PER-ELEMENT TOLERANCE: KEEP EVERY DECODABLE ELEMENT (REPAIR-COERCED WHEN THE
                        // CODEC FAILS), DROP THE REST, SO ONE BAD ENTRY NEVER DISCARDS THE WHOLE ARRAY
                        List<Object> kept = new ArrayList<>(values.length);
                        for (String raw : values) {
                            Object element = WaterConfig.parseElement(raw, collectionField.subType());
                            if (element != null) {
                                kept.add(element);
                            } else {
                                reader.report().add(new IFormatCodec.RepairEntry(0, "list element '" + raw + "' in '" + field.id() + "' is not a valid " + collectionField.subType().getSimpleName(), "dropped"));
                            }
                        }
                        collectionField.setArray(kept.toArray());
                        collectionField.validate();
                    }
                } catch (Exception e) {
                    System.err.println("[WaterConfig] Invalid value for field '" + field.id() + "', reset to default: " + e.getMessage());
                    field.reset();
                }
            } else {
                String value = reader.read(field.name());
                // RENAMED FIELDS PICK UP VALUES FROM THEIR FORMER NAMES
                for (int a = 0; value == null && a < field.aliases().length; a++) {
                    value = readAlias(reader, field.aliases()[a]);
                }
                if (value == null) {
                    continue;
                }

                // STRICT MATH IS A HARD-FAIL CONTRACT: ITS EXCEPTION MUST ABORT THE LOAD
                if (field instanceof BaseNumberField<?> numberField && numberField.math()) {
                    String evaluated = MathEvaluator.tryEvaluate(value, numberField.strictMath());
                    if (evaluated == null) {
                        field.reset();
                        continue;
                    }
                    // INTEGRAL FIELDS TRUNCATE FRACTIONAL MATH RESULTS (DOCUMENTED CONTRACT)
                    Class<?> t = field.type();
                    int dot = evaluated.indexOf('.');
                    if (dot >= 0 && (t == Integer.class || t == Long.class || t == Short.class || t == Byte.class)) {
                        evaluated = evaluated.substring(0, dot);
                    }
                    value = evaluated;
                }

                // PER-FIELD ISOLATION: ONE BAD VALUE RESETS THAT FIELD, NEVER ABORTS THE WHOLE LOAD.
                // REPAIR: parseElement DECODES VIA THE CODEC, THEN COERCES TO THE FIELD TYPE ON FAILURE
                try {
                    field.set0(WaterConfig.parseElement(value, field.type()));
                } catch (Exception e) {
                    System.err.println("[WaterConfig] Invalid value for field '" + field.id() + "', reset to default: " + e.getMessage());
                    field.reset();
                }
            }
        }
    }

    void save() throws IOException {
        synchronized (this.ioLock) {
            byte[] previous = (this.backups > 0 && this.filePath.toFile().exists()) ? Tools.readAllBytes(this.filePath) : null;

            try (IFormatWriter writer = this.format.createWriter(this.filePath)) {
                for (String c : this.comments()) {
                    writer.write(c);
                }
                writer.push(this.name());
                this.save(this, writer);
                writer.pop();
            }

            // ROTATE BACKUPS ONLY WHEN THE SAVED CONTENT ACTUALLY CHANGED
            if (previous != null && !Arrays.equals(previous, Tools.readAllBytes(this.filePath))) {
                for (int i = this.backups - 1; i >= 1; i--) {
                    Path from = backupPath(i);
                    if (Files.exists(from)) {
                        Files.move(from, backupPath(i + 1), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                Files.write(backupPath(1), previous);
            }

            // MIGRATION CLEANUP: THE OLD-FORMAT FILE IS DELETED ONLY ONCE ITS SETTINGS ARE
            // SAFELY PERSISTED IN THE CURRENT FORMAT; A FAILED DELETE IS RESIDUE, NOT A SAVE ERROR
            if (this.oldCleanup) {
                this.oldCleanup = false;
                try {
                    Files.deleteIfExists(this.oldPath);
                } catch (IOException e) {
                    System.err.println("[WaterConfig] Failed to delete old config file '" + this.oldPath + "': " + e.getMessage());
                }
            }
        }
    }

    private Path backupPath(int index) {
        return this.filePath.resolveSibling(this.filePath.getFileName() + "_backup" + index);
    }

    private void save(ConfigGroup group, IFormatWriter writer) {
        for (IConfigField<?, ?> field: group.getFields()) {
            try {
                for (String c: field.comments()) {
                    writer.write(c);
                }

                if (field instanceof ConfigGroup g) {
                    writer.push(g.name());
                    this.save(g, writer);
                    writer.pop();
                    continue;
                }

                if (field instanceof IStructuredField sf) {
                    sf.writeSelf(writer);
                    continue;
                }

                if (field instanceof BaseNumberField<?> numberField) {
                    // SET MATH ALLOW COMMENTS
                    if (numberField.math()) {
                        writer.write(COMMENT_ALLOWS_MATH + (numberField.strictMath() ? COMMENT_ALLOWS_MATH_STRICT : ""));
                    }

                    // SET MIN/MAX VALUE COMMENTS
                    String min = numberField.minValueString();
                    String max = numberField.maxValueString();

                    if (min != null && max == null) {
                        writer.write(String.format(COMMENT_MUST_BE_GREATER_THAN, min));
                    } else if (min == null && max != null) {
                        writer.write(String.format(COMMENT_MUST_BE_LESS_THAN, max));
                    } else if (min != null) {
                        writer.write(String.format(COMMENT_MUST_BE_IN_RANGE, max, min));
                    }
                }

                if (field instanceof StringField stringField) {
                    // SET STRING STARTWITH COMMENT
                    if (stringField.startsWith.isEmpty() && !stringField.endsWith.isEmpty()) {
                        writer.write(String.format(COMMENT_MUST_END_WITH, stringField.endsWith));
                    } else if (!stringField.startsWith.isEmpty() && stringField.endsWith.isEmpty()) {
                        writer.write(String.format(COMMENT_MUST_START_WITH, stringField.startsWith));
                    } else if (!stringField.startsWith.isEmpty()) {
                        writer.write(String.format(COMMENT_MUST_START_AND_END_WITH, stringField.startsWith, stringField.endsWith));
                    }

                    // SET STRING CONDITION AND MODE COMMENT
                    if (stringField.condition != null) {
                        String comment = switch (stringField.mode) {
                            case CONTAINS -> COMMENT_MUST_CONTAIN;
                            case EQUALS -> COMMENT_MUST_EQUALS;
                            case REGEX -> COMMENT_MUST_MATCH;
                            case NOT_CONTAINS -> COMMENT_MUST_NOT_CONTAIN;
                            case NOT_EQUALS -> COMMENT_MUST_NOT_EQUALS;
                            case NOT_REGEX -> COMMENT_MUST_NOT_MATCH;
                        };
                        writer.write(String.format(comment, stringField.condition));
                    }

                    // SET STRING ALLOW EMPTY COMMENT
                    writer.write(stringField.allowEmpty ? COMMENT_ALLOW_EMPTY : COMMENT_DENY_EMPTY);
                }

                if (field instanceof ListField<?> listField) {
                    // SET LIST ALLOW EMPTY AND UNIQUE COMMENT
                    writer.write((listField.allowEmpty
                            ? COMMENT_ARRAY_ALLOW_EMPTY
                            : COMMENT_ARRAY_DENY_EMPTY)
                            + (listField.unique ? " " + COMMENT_ARRAY_VALUES_MUST_BE_UNIQUE : "")
                    );

                    // SET LIST LIMIT COMMENT
                    writer.write(String.format(COMMENT_ARRAY_SIZE_MUST_BE_GREATER_THAN, listField.limit));
                }

                if (field instanceof EnumField<?> enumField) {
                    writer.write(String.format(COMMENT_ENUM_VALID_VALUES, Arrays.toString(enumField.type().getEnumConstants())));
                }

                if (field instanceof PathField pathField) {
                    writer.write(pathField.runtimePath ? COMMENT_PATH_RUNTIME : COMMENT_PATH_STATIC);
                    if (pathField.fileExists) {
                        writer.write(COMMENT_PATH_FILE_EXISTS);
                        writer.write(pathField.hardfail ? COMMENT_PATH_HARD_FAIL : COMMENT_PATH_SOFT_FAIL);
                    }
                }


                if (field instanceof CollectionField<?, ?> collectionField) {
                    Object[] elements = (field instanceof ListField<?> listField) ? listField.get().toArray() : ((ArrayField<?>) field).get();
                    String[] encoded = WaterConfig.tryEncode(elements, field.type(), field.subType());
                    if (collectionField.stringify) {
                        // STRINGIFY SUGAR: ONE COMMA-SEPARATED STRING INSTEAD OF A NATIVE ARRAY
                        writer.write(field.name(), String.join(", ", encoded), String.class, null);
                    } else {
                        writer.write(field.name(), encoded, field.type(), field.subType());
                    }
                } else {
                    writer.write(field.name(), WaterConfig.tryEncode(field.get(), field.subType()), field.type(), field.subType());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to save field '" + field.id() + "' in config spec '" + this.name() + "'", e);
            }
        }
    }


    public static final class SpecBuilder {
        private final ConfigSpec spec;
        private final Deque<Integer> pushedLevels = new ArrayDeque<>();
        private ConfigGroup active;

        public SpecBuilder(String name, String format, String suffix, int backups) {
            this(name, FORMATS.get(format), suffix, backups);
        }

        public SpecBuilder(String name, IFormatCodec format, String suffix, int backups) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            if (format == null) {
                throw new IllegalArgumentException("Format cannot be null");
            }
            if (backups < 0) {
                throw new IllegalArgumentException("Backups cannot be negative");
            }

            Path path = WaterConfig.getPath().toAbsolutePath().resolve(name + (!suffix.isEmpty() ? ("-" + suffix) : "") + format.extension());
            this.spec = new ConfigSpec(name, format, suffix, path, backups);
            this.active = this.spec;
        }

        // REVERSESPEC INTERNAL: POINTS THE SPEC AT AN ARBITRARY EXISTING FILE
        SpecBuilder(String name, IFormatCodec format, Path path, int backups) {
            this.spec = new ConfigSpec(name, format, "", path, backups);
            this.active = this.spec;
        }

        /**
         * Sets the previous format to recover settings from: when the current-format file is
         * missing on load, the same spec file with the old format extension is searched and its
         * settings recovered, persisted in the current format and the old file deleted. When the
         * current-format file already exists the old file is only ignored.
         *
         * @param format the previous format id; empty or null means nothing old to recover
         * @throws IllegalArgumentException when the format is unknown or equals the spec format
         */
        public SpecBuilder old(String format) {
            if (format == null || format.isEmpty()) {
                return this;
            }
            IFormatCodec old = FORMATS.get(format);
            if (old == null) {
                throw new IllegalArgumentException("Unknown old format '" + format + "'");
            }
            if (old.id().equals(this.spec.format().id())) {
                throw new IllegalArgumentException("Old format cannot be the spec format itself");
            }
            // SAME FILE NAME NEXT TO THE CURRENT ONE, WITH THE OLD FORMAT EXTENSION
            String file = this.spec.path().getFileName().toString();
            String extension = this.spec.format().extension();
            String stem = file.endsWith(extension) ? file.substring(0, file.length() - extension.length()) : file;
            this.spec.oldFormat = old;
            this.spec.oldPath = this.spec.path().resolveSibling(stem + old.extension());
            return this;
        }

        PathFieldBuilder definePath(String name, Field field, Object context) {
            return new PathFieldBuilder(name, this.active, field, context);
        }

        <T> ListFieldBuilder<T> defineList(String name, Field field, Object context, Class<T> subType) {
            return new ListFieldBuilder<>(name, this.active, field, context, subType);
        }

        <T> ArrayFieldBuilder<T> defineArray(String name, Field field, Object context, Class<T> subType) {
            return new ArrayFieldBuilder<>(name, this.active, field, context, subType);
        }

        <T extends Enum<T>> EnumFieldBuilder<T> defineEnum(String name, Field field, Object context) {
            return new EnumFieldBuilder<>(name, this.active, field, context);
        }

        StringFieldBuilder defineString(String name, Field field, Object context) {
            return new StringFieldBuilder(name, this.active, field, context);
        }

        BooleanFieldBuilder defineBoolean(String name, Field field, Object context) {
            return new BooleanFieldBuilder(name, this.active, field, context);
        }

        ByteFieldBuilder defineByte(String name, Field field, Object context) {
            return new ByteFieldBuilder(name, this.active, field, context);
        }

        ShortFieldBuilder defineShort(String name, Field field, Object context) {
            return new ShortFieldBuilder(name, this.active, field, context);
        }

        CharFieldBuilder defineChar(String name, Field field, Object context) {
            return new CharFieldBuilder(name, this.active, field, context);
        }

        IntFieldBuilder defineInt(String name, Field field, Object context) {
            return new IntFieldBuilder(name, this.active, field, context);
        }

        LongFieldBuilder defineLong(String name, Field field, Object context) {
            return new LongFieldBuilder(name, this.active, field, context);
        }

        FloatFieldBuilder defineFloat(String name, Field field, Object context) {
            return new FloatFieldBuilder(name, this.active, field, context);
        }

        DoubleFieldBuilder defineDouble(String name, Field field, Object context) {
            return new DoubleFieldBuilder(name, this.active, field, context);
        }

        <T, S> BaseFieldBuilder<CustomFieldBuilder<T, S>, BaseConfigField<T, S>> define(String name, Field field, Object context) {
            return new CustomFieldBuilder<>(name, this.active, field, context);
        }

        <T extends Comparable<T>> TemporalFieldBuilder<T> defineTemporal(String name, Field field, Object context, Class<T> type) {
            return new TemporalFieldBuilder<>(name, this.active, field, context, type);
        }

        public <T extends Comparable<T>> TemporalFieldBuilder<T> defineTemporal(String name, T defaultValue, Class<T> type) {
            return new TemporalFieldBuilder<>(name, this.active, defaultValue, type);
        }

        ColorFieldBuilder defineColor(String name, Field field, Object context) {
            return new ColorFieldBuilder(name, this.active, field, context);
        }

        public ColorFieldBuilder defineColor(String name, Color defaultValue) {
            return new ColorFieldBuilder(name, this.active, defaultValue);
        }

        public PathFieldBuilder definePath(String name, Path defaultValue) {
            return new PathFieldBuilder(name, this.active, defaultValue);
        }

        public <T> ListFieldBuilder<T> defineList(String name, List<T> defaultValue, Class<T> subType) {
            return new ListFieldBuilder<>(name, this.active, defaultValue, subType);
        }

        public <T extends Enum<T>> EnumFieldBuilder<T> defineEnum(String name, T defaultValue) {
            return new EnumFieldBuilder<>(name, this.active, defaultValue);
        }

        public StringFieldBuilder defineString(String name, String defaultValue) {
            return new StringFieldBuilder(name, this.active, defaultValue);
        }

        public BooleanFieldBuilder defineBoolean(String name, boolean defaultValue) {
            return new BooleanFieldBuilder(name, this.active, defaultValue);
        }

        public ByteFieldBuilder defineByte(String name, byte defaultValue) {
            return new ByteFieldBuilder(name, this.active, defaultValue);
        }

        public ShortFieldBuilder defineShort(String name, short defaultValue) {
            return new ShortFieldBuilder(name, this.active, defaultValue);
        }

        public CharFieldBuilder defineChar(String name, char defaultValue) {
            return new CharFieldBuilder(name, this.active, defaultValue);
        }

        public IntFieldBuilder defineInt(String name, int defaultValue) {
            return new IntFieldBuilder(name, this.active, defaultValue);
        }

        public LongFieldBuilder defineLong(String name, long defaultValue) {
            return new LongFieldBuilder(name, this.active, defaultValue);
        }

        public FloatFieldBuilder defineFloat(String name, float defaultValue) {
            return new FloatFieldBuilder(name, this.active, defaultValue);
        }

        public DoubleFieldBuilder defineDouble(String name, double defaultValue) {
            return new DoubleFieldBuilder(name, this.active, defaultValue);
        }

        public <T, S> BaseFieldBuilder<CustomFieldBuilder<T, S>, BaseConfigField<T, S>> define(String name, T defaultValue, Class<T> type, Class<S> subType) {
            return new CustomFieldBuilder<>(name, this.active, defaultValue, type, subType);
        }

        public SpecBuilder comments(String... comments) {
            this.active.comments.addAll(Arrays.asList(comments));
            return this;
        }

        public SpecBuilder comment(String comment) {
            this.active.comments.add(comment);
            return this;
        }

        public SpecBuilder push(String name) {
            String[] parts = name.split("\\.");
            for (String n: parts) {
                this.active = new ConfigGroup(n, this.active);
            }
            // A DOTTED PUSH CREATES SEVERAL LEVELS: pop() MUST UNDO ALL OF THEM
            this.pushedLevels.addLast(parts.length);
            return this;
        }

        public SpecBuilder pop() {
            Integer levels = this.pushedLevels.pollLast();
            if (levels == null) {
                throw new IllegalStateException("pop() without a matching push()");
            }
            while (levels-- > 0) {
                if (!this.active.fields.isEmpty()) {
                    this.active.fields = Collections.unmodifiableSet(this.active.fields);
                    this.active.comments = Collections.unmodifiableSet(this.active.comments);
                } else {
                    this.active.group.dispose(this.active); // REMOVE EMPTY GROUP
                }
                this.active = this.active.group;
            }
            return this;
        }

        public SpecBuilder pop(int l) {
            if (l < 1)
                throw new IllegalArgumentException("length must be positive");

            while ((l--) > 0) {
                this.pop();
            }
            return this;
        }

        public SpecBuilder popAll() {
            this.pushedLevels.clear();
            this.active = this.spec;
            return this;
        }

        public ConfigSpec build() {
            this.spec.fields = Collections.unmodifiableSet(this.spec.fields);
            this.spec.comments = Collections.unmodifiableSet(this.spec.comments);

            // PARENT DIRECTORIES ARE CREATED ONCE HERE INSTEAD OF ON EVERY SAVE
            Path parent = this.spec.path().toAbsolutePath().getParent();
            if (parent != null) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to create config directory " + parent, e);
                }
            }
            return spec;
        }
    }

    public static non-sealed class CustomFieldBuilder<T, S> extends BaseFieldBuilder<CustomFieldBuilder<T, S>, BaseConfigField<T, S>> {
        private final T defaultValue;
        private final Class<T> type;
        private final Class<S> subType;

        public CustomFieldBuilder(String name, ConfigGroup group, T defaultValue, Class<T> type, Class<S> subType) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.type = type;
            this.subType = subType;
        }

        public CustomFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            super(name, group);
            this.defaultValue = null;
            this.field = field;
            this.context = context;
            this.type = (Class<T>) Tools.typeOf(field);
            this.subType = (Class<S>) Tools.subTypeOf(field);
        }

        @Override
        protected BaseConfigField<T, S> build() {
            if (field == null) {
                return new BaseConfigField<>(this.name, this.group, this.comments, this.defaultValue, this.control, this.suffix) {

                    @Override
                    public void validate() {}

                    @Override
                    public Class<T> type() {
                        return type;
                    }

                    @Override
                    public Class<S> subType() {
                        return subType;
                    }
                };
            } else {
                return new BaseConfigField<>(this.name, this.group, this.comments, this.field, this.context, this.control, this.suffix) {
                    @Override
                    public Class<T> type() {
                        return type;
                    }

                    @Override
                    public Class<S> subType() {
                        return subType;
                    }

                    @Override
                    public void validate() {

                    }
                };
            }

        }
    }

    public static final class TemporalFieldBuilder<T extends Comparable<T>> extends BaseFieldBuilder<TemporalFieldBuilder<T>, TemporalField<T>> {
        private final Class<T> type;
        private final Field field;
        private final Object context;
        private final T defaultValue;
        private T from;
        private T to;

        private TemporalFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<T> type) {
            super(name, group);
            this.field = field;
            this.context = context;
            this.type = type;
            this.defaultValue = null;
        }

        private TemporalFieldBuilder(String name, ConfigGroup group, T defaultValue, Class<T> type) {
            super(name, group);
            this.field = null;
            this.context = null;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public TemporalFieldBuilder<T> from(T from) {
            this.from = from;
            return this;
        }

        public TemporalFieldBuilder<T> to(T to) {
            this.to = to;
            return this;
        }

        @Override
        protected TemporalField<T> build() {
            if (this.field == null) {
                return new TemporalField<>(this.name, this.group, this.comments, this.type, this.from, this.to, this.defaultValue, this.control, this.suffix);
            } else {
                return new TemporalField<>(this.name, this.group, this.comments, this.type, this.from, this.to, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class ColorFieldBuilder extends BaseFieldBuilder<ColorFieldBuilder, ColorField> {
        private final Color defaultValue;
        private final Field field;
        private final Object context;
        private ColorConditions.Radix radix = ColorConditions.Radix.AUTO;

        private ColorFieldBuilder(String name, ConfigGroup group, Color defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.field = null;
            this.context = null;
        }

        private ColorFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            super(name, group);
            this.defaultValue = null;
            this.field = field;
            this.context = context;
        }

        public ColorFieldBuilder radix(ColorConditions.Radix radix) {
            this.radix = radix;
            return this;
        }

        @Override
        protected ColorField build() {
            if (this.field == null) {
                return new ColorField(this.name, this.group, this.comments, this.radix, this.defaultValue, this.control, this.suffix);
            } else {
                return new ColorField(this.name, this.group, this.comments, this.radix, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class PathFieldBuilder extends BaseFieldBuilder<PathFieldBuilder, PathField> {
        private final Path defaultValue;
        private boolean runtimePath = true;
        private boolean fileExists = false;
        private boolean hardfail = false;

        private PathFieldBuilder(String name, ConfigGroup group, Path defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
        }

        private PathFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, null);
            this.field = field;
            this.context = context;
        }

        public PathFieldBuilder runtimePath(boolean runtimePath) {
            this.runtimePath = runtimePath;
            return this;
        }

        public PathFieldBuilder fileExists(boolean fileExists) {
            this.fileExists = fileExists;
            return this;
        }

        public PathFieldBuilder hardfail(boolean hardfail) {
            this.hardfail = hardfail;
            return this;
        }

        @Override
        protected PathField build() {
            if (this.field == null) {
                return new PathField(this.name, this.group, this.comments, this.runtimePath, this.fileExists, this.hardfail, this.defaultValue, this.control, this.suffix);
            } else {
                return new PathField(this.name, this.group, this.comments, this.runtimePath, this.fileExists, this.hardfail, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    /**
     * Shared options for collection (list/array) field builders.
     */
    public static abstract sealed class CollectionFieldBuilder<S, F extends CollectionField<?, S>, B extends CollectionFieldBuilder<S, F, B>> extends BaseFieldBuilder<B, F> permits ListFieldBuilder, ArrayFieldBuilder {
        protected final Class<S> subType;
        protected boolean stringify = false;
        @Deprecated(forRemoval = true)
        protected boolean singleline = true;
        protected boolean allowEmpty = true;
        protected boolean unique = false;
        protected int limit = Integer.MAX_VALUE;
        protected Class<? extends Predicate<S>> filter;

        protected CollectionFieldBuilder(String name, ConfigGroup group, Class<S> subType) {
            super(name, group);
            this.subType = subType;
        }

        public B stringify(boolean stringify) {
            this.stringify = stringify;
            return (B) this;
        }

        @Deprecated(forRemoval = true)
        public B singleline(boolean singleline) {
            this.singleline = singleline;
            return (B) this;
        }

        public B allowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return (B) this;
        }

        public B unique(boolean unique) {
            this.unique = unique;
            return (B) this;
        }

        public B limit(int limit) {
            this.limit = limit;
            return (B) this;
        }

        public B filter(Class<? extends Predicate<S>> filter) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter cannot be null");
            }
            this.filter = filter;
            return (B) this;
        }
    }

    static final class ArrayFieldBuilder<S> extends CollectionFieldBuilder<S, ArrayField<S>, ArrayFieldBuilder<S>> {
        private final S[] defaultValue;

        private ArrayFieldBuilder(String name, ConfigGroup group, S[] defaultValue, Class<S> subType) {
            super(name, group, requireBoxed(name, subType));
            this.defaultValue = defaultValue;
        }

        private ArrayFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<S> subType) {
            this(name, group, null, subType);
            this.field = field;
            this.context = context;
        }

        // PRIMITIVE COMPONENT TYPES (int[]) CANNOT BACK AN ArrayField<S>: FAIL WITH A CLEAR MESSAGE
        private static <S> Class<S> requireBoxed(String name, Class<S> subType) {
            if (subType != null && subType.isPrimitive()) {
                throw new IllegalArgumentException("Array field '" + name + "' has a primitive component type '"
                        + subType.getName() + "[]'; use the boxed type instead (e.g. Integer[])");
            }
            return subType;
        }

        @Override
        protected ArrayField<S> build() {
            if (this.field == null) {
                return new ArrayField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.defaultValue, this.subType, this.control, this.suffix);
            } else {
                return new ArrayField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.field, this.context, this.subType, this.control, this.suffix);
            }
        }
    }

    public static final class ListFieldBuilder<S> extends CollectionFieldBuilder<S, ListField<S>, ListFieldBuilder<S>> {
        private final List<S> defaultValue;

        private ListFieldBuilder(String name, ConfigGroup group, List<S> defaultValue, Class<S> subType) {
            super(name, group, subType);
            this.defaultValue = defaultValue;
        }

        private ListFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<S> subType) {
            this(name, group, null, subType);
            this.field = field;
            this.context = context;
        }

        public ListFieldBuilder<S> add(S e) {
            this.defaultValue.add(e);
            return this;
        }

        @Override
        protected ListField<S> build() {
            if (this.field == null) {
                return new ListField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.defaultValue, this.subType, this.control, this.suffix);
            } else {
                return new ListField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.field, this.context, this.subType, this.control, this.suffix);
            }
        }
    }

    public static final class EnumFieldBuilder<T extends Enum<T>> extends BaseFieldBuilder<EnumFieldBuilder<T>, EnumField<T>> {
        private final T defaultValue;

        private EnumFieldBuilder(String name, ConfigGroup group, T defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
        }

        private EnumFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, null);
            this.field = field;
            this.context = context;
        }

        @Override
        protected EnumField<T> build() {
            if (this.field == null) { // NATIVE MODE WHEN NO REFLECTION FIELD IS BOUND
                return new EnumField<>(this.name, this.group, this.comments, this.defaultValue, this.control, this.suffix);
            } else {
                return new EnumField<>(this.name, this.group, this.comments, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class StringFieldBuilder extends BaseFieldBuilder<StringFieldBuilder, StringField> {
        private final String defaultValue;
        private String startsWith = "";
        private String endsWith = "";
        private boolean allowEmpty = true;
        private String condition = "";
        private int regexFlags = 0;
        private StringField.Mode mode = StringField.Mode.CONTAINS;

        private StringFieldBuilder(String name, ConfigGroup group, String defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
        }

        private StringFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, null);
            this.field = field;
            this.context = context;
        }

        public StringFieldBuilder startsWith(String start) {
            this.startsWith = start;
            return this;
        }

        public StringFieldBuilder endsWith(String end) {
            this.endsWith = end;
            return this;
        }

        public StringFieldBuilder allowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return this;
        }

        public StringFieldBuilder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public StringFieldBuilder regexFlags(int regexFlags) {
            this.regexFlags = regexFlags;
            return this;
        }

        public StringFieldBuilder mode(StringField.Mode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        protected StringField build() {
            if (this.field == null) {
                return new StringField(this.name, this.group, this.comments, this.startsWith, this.endsWith, this.allowEmpty, this.condition, this.regexFlags, this.mode, this.defaultValue, this.control, this.suffix);
            } else {
                return new StringField(this.name, this.group, this.comments, this.startsWith, this.endsWith, this.allowEmpty, this.condition, this.regexFlags, this.mode, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class BooleanFieldBuilder extends BaseFieldBuilder<BooleanFieldBuilder, BooleanField> {
        private final boolean defaultValue;

        private BooleanFieldBuilder(String name, ConfigGroup group, boolean defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
        }

        private BooleanFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, false);
            this.field = field;
            this.context = context;
        }

        @Override
        protected BooleanField build() {
            if (this.field == null) {
                return new BooleanField(this.name, this.group, this.comments, this.defaultValue, this.control, this.suffix);
            } else {
                return new BooleanField(this.name, this.group, this.comments, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class ByteFieldBuilder extends NumberFieldBuilder<Byte, ByteField, ByteFieldBuilder> {
        private final byte defaultValue;

        private ByteFieldBuilder(String name, ConfigGroup group, byte defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Byte.MIN_VALUE;
            this.max = Byte.MAX_VALUE;
        }

        private ByteFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, (byte) 0);
            this.field = field;
            this.context = context;
        }

        @Override
        protected ByteField build() {
            if (this.field == null) {
                return new ByteField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new ByteField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class ShortFieldBuilder extends NumberFieldBuilder<Short, ShortField, ShortFieldBuilder> {
        private final short defaultValue;

        private ShortFieldBuilder(String name, ConfigGroup group, short defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Short.MIN_VALUE;
            this.max = Short.MAX_VALUE;
        }

        private ShortFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, (short) 0);
            this.field = field;
            this.context = context;
        }

        @Override
        protected ShortField build() {
            if (this.field == null) {
                return new ShortField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new ShortField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class CharFieldBuilder extends BaseFieldBuilder<CharFieldBuilder, CharField> {
        private final char defaultValue;

        private CharFieldBuilder(String name, ConfigGroup group, char defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
        }

        private CharFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, (char) 0);
            this.field = field;
            this.context = context;
        }

        @Override
        protected CharField build() {
            if (this.field == null) {
                return new CharField(this.name, this.group, this.comments, this.defaultValue, this.control, this.suffix);
            } else {
                return new CharField(this.name, this.group, this.comments, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class IntFieldBuilder extends NumberFieldBuilder<Integer, IntField, IntFieldBuilder> {
        private final int defaultValue;

        private IntFieldBuilder(String name, ConfigGroup group, int defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Integer.MIN_VALUE;
            this.max = Integer.MAX_VALUE;
        }

        private IntFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0);
            this.field = field;
            this.context = context;
        }

        @Override
        protected IntField build() {
            if (this.field == null) {
                return new IntField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new IntField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class LongFieldBuilder extends NumberFieldBuilder<Long, LongField, LongFieldBuilder> {
        private final long defaultValue;

        private LongFieldBuilder(String name, ConfigGroup group, long defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Long.MIN_VALUE;
            this.max = Long.MAX_VALUE;
        }

        private LongFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0);
            this.field = field;
            this.context = context;
        }

        @Override
        protected LongField build() {
            if (this.field == null) {
                return new LongField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new LongField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class FloatFieldBuilder extends NumberFieldBuilder<Float, FloatField, FloatFieldBuilder> {
        private final float defaultValue;

        private FloatFieldBuilder(String name, ConfigGroup group, float defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = -Float.MAX_VALUE;
            this.max = Float.MAX_VALUE;
        }

        private FloatFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0F);
            this.field = field;
            this.context = context;
        }

        @Override
        protected FloatField build() {
            if (this.field == null) {
                return new FloatField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new FloatField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    public static final class DoubleFieldBuilder extends NumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder> {
        private final double defaultValue;

        private DoubleFieldBuilder(String name, ConfigGroup group, double defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = -Double.MAX_VALUE;
            this.max = Double.MAX_VALUE;
        }

        private DoubleFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0D);
            this.field = field;
            this.context = context;
        }

        @Override
        protected DoubleField build() {
            if (this.field == null) {
                return new DoubleField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue, this.control, this.suffix);
            } else {
                return new DoubleField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context, this.control, this.suffix);
            }
        }
    }

    /**
     * @param <T>
     * @param <B> builder which extends
     */
    public static abstract sealed class NumberFieldBuilder<T extends Number, F extends BaseNumberField<T>, B extends NumberFieldBuilder<T, F, B>> extends BaseFieldBuilder<B, F> {
        protected boolean strictMath = false;
        protected boolean math = false;
        protected T min;
        protected T max;

        protected NumberFieldBuilder(String name, ConfigGroup group) {
            super(name, group);
        }

        public B setMin(T min) {
            this.min = min;
            return (B) this;
        }

        public B setMax(T max) {
            this.max = max;
            return (B) this;
        }

        public B strictMath(boolean strictMath) {
            this.strictMath = strictMath;
            return (B) this;
        }

        public B math(boolean math) {
            this.math = math;
            return (B) this;
        }
    }

    public static abstract sealed class BaseFieldBuilder<B extends BaseFieldBuilder<B, ?>, F extends IConfigField<?, ?>> {
        protected final String name;
        protected final ConfigGroup group;
        protected final Set<String> comments = new LinkedHashSet<>();
        protected Control control = Control.DEFAULT;
        protected String suffix = "";
        protected String[] aliases = IConfigField.NO_ALIASES;
        protected Field field;
        protected Object context;

        protected BaseFieldBuilder(String name, ConfigGroup group) {
            this.name = name;
            this.group = group;
        }

        public B comments(String... comments) {
            this.comments.addAll(Arrays.asList(comments));
            return (B) this;
        }

        /**
         * Former names of this field. On load, when the current name is missing from the
         * file, aliases are tried in order so renamed fields pick up values from old files.
         */
        public B aliases(String... aliases) {
            this.aliases = (aliases == null) ? IConfigField.NO_ALIASES : aliases;
            return (B) this;
        }

        /**
         * Sets the preferred UI control for this field. When left as {@link Control#DEFAULT},
         * the field keeps the control its type prefers.
         */
        public B control(Control control) {
            this.control = (control == null) ? Control.DEFAULT : control;
            return (B) this;
        }

        /**
         * Sets a display suffix (e.g. {@code "MB"}, {@code "SECS"}) forwarded to the field as
         * informational metadata for external config UIs. Empty string when unset.
         */
        public B suffix(String suffix) {
            this.suffix = (suffix == null) ? "" : suffix;
            return (B) this;
        }

        public final F end() {
            F field = this.build();
            if (field instanceof BaseConfigField<?, ?> base) {
                base.aliases(this.aliases);
            }
            return field;
        }

        protected abstract F build();
    }
}
