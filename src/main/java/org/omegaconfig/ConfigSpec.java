package org.omegaconfig;

import org.omegaconfig.api.IConfigField;
import org.omegaconfig.api.formats.IFormatCodec;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.impl.fields.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static org.omegaconfig.OmegaConfigRegistry.FORMATS;

public final class ConfigSpec extends ConfigGroup {
    // NUMBER COMMENTS
    private static final String COMMENT_ALLOWS_MATH = "Allows math expressions";
    private static final String COMMENT_ALLOWS_MATH_STRICT = ", hard fails on invalid math expressions";
    private static final String COMMENT_MUST_BE_IN_RANGE = "Value must be less than '%s' and greater than '%s'";
    private static final String COMMENT_MUST_BE_LESS_THAN = "Value must be less than %s";
    private static final String COMMENT_MUST_BE_GREATER_THAN = "Value must be greater than %s";

    // STRING COMMENTS
    private static final String COMMENT_ALLOW_EMPTY = "Allows (non-null) empty values";
    private static final String COMMENT_DENY_EMPTY = "Must no be empty";
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
    private final String suffix;
    private final Path filePath;
    private final HashSet<IConfigField<?, ?>> dirtyFields = new LinkedHashSet<>();
    private final int backups;
    boolean dirty;
    boolean loaded;
    boolean reload;

    private ConfigSpec(String name, IFormatCodec format, String suffix, Path path, int backups) {
        super(name, null);
        this.format = format;
        this.suffix = suffix;
        this.filePath = path;
        this.backups = backups;
        this.dirty = true;
    }

    @Override
    public String id() {
        return this.name() + ":";
    }

    public void markDirty(IConfigField<?, ?> field) {
        if (field.spec() != this)
            throw new IllegalArgumentException("ConfigField requires to be updated by the intended spec");
        this.dirtyFields.add(field);
        this.dirty = true;
    }

    Set<IConfigField<?, ?>> dirtyFields() {
        return this.dirtyFields;
    }

    public Path path() {
        return this.filePath;
    }

    IFormatCodec format() {
        return this.format;
    }

    String suffix() {
        return this.suffix;
    }

    int backups() {
        return this.backups;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isReload() {
        return this.reload;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    void load() throws IOException {
        if (!this.filePath.toFile().exists()) {
            this.save();
        }
        IFormatReader reader = this.format.createReader(this.filePath);
        this.load(this, reader);
        reader.close();
    }

    private void load(ConfigGroup group, IFormatReader reader) {
        for (IConfigField<?, ?> field: group.getFields()) {
            if (field instanceof ConfigGroup g) {
                reader.push(g.name());
                this.load(g, reader);
                reader.pop();
                continue;
            }

            if (field instanceof CollectionField<?,?> collectionField) {
                String[] values = reader.readArray(field.name());
                if (values != null) {
                    Object[] parsedValues = OmegaConfig.tryParse(values, field.type(), field.subType());
                    if (parsedValues != null && Tools.requireNotNull(parsedValues)) {
                        collectionField.setArray(parsedValues);
                    }
                }
            } else {
                String value = reader.read(field.name());
                if (value != null) {
                    field.set0(OmegaConfig.tryParse(value, field.type(), field.subType()));
                }
            }
        }
        this.loaded = true;
        this.reload = false;
    }

    void save() throws IOException {
        IFormatWriter writer = this.format.createWritter(this.filePath);
        this.save(this, writer);
        writer.close();
    }

    private void save(ConfigGroup group, IFormatWriter writer) {
        for (IConfigField<?, ?> field: group.getFields()) {
            for (String c: field.comments()) {
                writer.write(c);
            }

            if (field instanceof ConfigGroup g) {
                writer.push(g.name());
                this.save(g, writer);
                writer.pop();
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

                if (min == null && max != null) {
                    writer.write(String.format(COMMENT_MUST_BE_GREATER_THAN, max));
                } else if (min != null && max == null) {
                    writer.write(String.format(COMMENT_MUST_BE_LESS_THAN, min));
                } else if (min != null) {
                    writer.write(String.format(COMMENT_MUST_BE_IN_RANGE, min, max));
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
                }
            }

            if (field instanceof ListField<?> listField) {
                writer.write(field.name(), OmegaConfig.tryEncode(listField.get().toArray(), field.type(), field.subType()), field.type(), field.subType());
            } else if (field instanceof ArrayField<?> arrayField) {
                writer.write(field.name(), OmegaConfig.tryEncode(arrayField.get(), field.type(), field.subType()), field.type(), field.subType());
            } else {
                writer.write(field.name(), OmegaConfig.tryEncode(field.get(), field.subType()), field.type(), field.subType());
            }
        }
        this.dirty = false;
    }


    public static final class SpecBuilder {
        private final ConfigSpec spec;
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

            Path path = OmegaConfig.getPath().toAbsolutePath().resolve(name + (!suffix.isEmpty() ? ("-" + suffix) : "") + format.extension());
            this.spec = new ConfigSpec(name, format, suffix, path, backups);
            this.active = this.spec;
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

        public SpecBuilder comments(String... comment) {
            this.active.comments.addAll(Arrays.asList(comment));
            return this;
        }

        public SpecBuilder push(String name) {
            for (String n : name.split("\\.")) {
                this.active = new ConfigGroup(n, this.active);
            }
            return this;
        }

        public SpecBuilder pop() {
            this.active.fields = Collections.unmodifiableSet(this.active.fields);
            this.active.comments = Collections.unmodifiableSet(this.active.comments);
            this.active = this.active.group;
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
            this.active = this.spec;
            return this;
        }

        public ConfigSpec build() {
            this.spec.fields = Collections.unmodifiableSet(this.spec.fields);
            this.spec.comments = Collections.unmodifiableSet(this.spec.comments);
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
        public BaseConfigField<T, S> end() {
            if (field == null) {
                return new BaseConfigField<>(this.name, this.group, this.comments, this.defaultValue) {

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
                return new BaseConfigField<>(this.name, this.group, this.comments, this.field, this.context) {
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

    public static final class PathFieldBuilder extends BaseFieldBuilder<PathFieldBuilder, PathField> {
        private final Path defaultValue;
        private boolean runtimePath = true;
        private boolean fileExists = false;

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

        @Override
        public PathField end() {
            if (this.field == null) {
                return new PathField(this.name, this.group, this.comments, this.runtimePath, this.fileExists, this.defaultValue);
            } else {
                return new PathField(this.name, this.group, this.comments, this.runtimePath, this.fileExists, this.field, this.context);
            }
        }
    }

    static final class ArrayFieldBuilder<S> extends BaseFieldBuilder<ArrayFieldBuilder<S>, ArrayField<S>> {
        private final Class<S> subType;
        private final Field field;
        private final Object context;
        private final S[] defaultValue;
        private boolean stringify = false;
        private boolean singleline = true;
        private boolean allowEmpty = true;
        private boolean unique = false;
        private int limit = Integer.MAX_VALUE;
        private Class<? extends Predicate<S>> filter;

        private ArrayFieldBuilder(String name, ConfigGroup group, S[] defaultValue, Class<S> subType) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.subType = subType;
            this.field = null;
            this.context = null;
        }

        private ArrayFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<S> subType) {
            super(name, group);
            this.defaultValue = null;
            this.subType = subType;
            this.field = field;
            this.context = context;
        }

        public ArrayFieldBuilder<S> stringify(boolean stringify) {
            this.stringify = stringify;
            return this;
        }

        public ArrayFieldBuilder<S> singleline(boolean singleline) {
            this.singleline = singleline;
            return this;
        }

        public ArrayFieldBuilder<S> allowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return this;
        }

        public ArrayFieldBuilder<S> unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public ArrayFieldBuilder<S> limit(int limit) {
            this.limit = limit;
            return this;
        }

        public ArrayFieldBuilder<S> filter(Class<? extends Predicate<S>> filter) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter cannot be null");
            }
            this.filter = filter;
            return this;
        }

        @Override
        public ArrayField<S> end() {
            if (this.field == null) {
                return new ArrayField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.defaultValue, this.subType);
            } else {
                return new ArrayField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.field, this.context, this.subType);
            }
        }
    }

    public static final class ListFieldBuilder<S> extends BaseFieldBuilder<ListFieldBuilder<S>, ListField<S>> {
        private final Class<S> subType;
        private final Field field;
        private final Object context;
        private final List<S> defaultValue;
        private boolean stringify = false;
        private boolean singleline = true;
        private boolean allowEmpty = true;
        private boolean unique = false;
        private int limit = Integer.MAX_VALUE;
        private Class<? extends Predicate<S>> filter;

        private ListFieldBuilder(String name, ConfigGroup group, List<S> defaultValue, Class<S> subType) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.subType = subType;
            this.field = null;
            this.context = null;
        }

        private ListFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<S> subType) {
            super(name, group);
            this.defaultValue = null;
            this.subType = subType;
            this.field = field;
            this.context = context;
        }

        public ListFieldBuilder<S> add(S e) {
            this.defaultValue.add(e);
            return this;
        }

        public ListFieldBuilder<S> stringify(boolean stringify) {
            this.stringify = stringify;
            return this;
        }

        public ListFieldBuilder<S> singleline(boolean singleline) {
            this.singleline = singleline;
            return this;
        }

        public ListFieldBuilder<S> allowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return this;
        }

        public ListFieldBuilder<S> unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public ListFieldBuilder<S> limit(int limit) {
            this.limit = limit;
            return this;
        }

        public ListFieldBuilder<S> filter(Class<? extends Predicate<S>> filter) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter cannot be null");
            }
            this.filter = filter;
            return this;
        }

        @Override
        public ListField<S> end() {
            if (this.field == null) {
                return new ListField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.defaultValue, this.subType);
            } else {
                return new ListField<>(this.name, this.group, this.comments, this.stringify, this.singleline, this.allowEmpty, this.unique, this.limit, this.filter, this.field, this.context, this.subType);
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
        public EnumField<T> end() {
            if (this.field == null) { // WEAK CHECK
                return new EnumField<>(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new EnumField<>(this.name, this.group, this.comments, this.field, this.context);
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
        public StringField end() {
            if (this.field == null) {
                return new StringField(this.name, this.group, this.comments, this.startsWith, this.endsWith, this.allowEmpty, this.condition, this.regexFlags, this.mode, this.defaultValue);
            } else {
                return new StringField(this.name, this.group, this.comments, this.startsWith, this.endsWith, this.allowEmpty, this.condition, this.regexFlags, this.mode, this.field, this.context);
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
        public BooleanField end() {
            if (this.field == null) {
                return new BooleanField(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new BooleanField(this.name, this.group, this.comments, this.field, this.context);
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
        public ByteField end() {
            if (this.field == null) {
                return new ByteField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new ByteField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
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
        public ShortField end() {
            if (this.field == null) {
                return new ShortField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new ShortField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
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
        public CharField end() {
            if (this.field == null) {
                return new CharField(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new CharField(this.name, this.group, this.comments, this.field, this.context);
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
        public IntField end() {
            if (this.field == null) {
                return new IntField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new IntField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
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
        public LongField end() {
            if (this.field == null) {
                return new LongField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new LongField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
            }
        }
    }

    public static final class FloatFieldBuilder extends NumberFieldBuilder<Float, FloatField, FloatFieldBuilder> {
        private final float defaultValue;

        private FloatFieldBuilder(String name, ConfigGroup group, float defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Float.MIN_VALUE;
            this.max = Float.MAX_VALUE;
        }

        private FloatFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0F);
            this.field = field;
            this.context = context;
        }

        @Override
        public FloatField end() {
            if (this.field == null) {
                return new FloatField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new FloatField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
            }
        }
    }

    public static final class DoubleFieldBuilder extends NumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder> {
        private final double defaultValue;

        private DoubleFieldBuilder(String name, ConfigGroup group, double defaultValue) {
            super(name, group);
            this.defaultValue = defaultValue;
            this.min = Double.MIN_VALUE;
            this.max = Double.MAX_VALUE;
        }

        private DoubleFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, 0D);
            this.field = field;
            this.context = context;
        }

        @Override
        public DoubleField end() {
            if (this.field == null) {
                return new DoubleField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
            } else {
                return new DoubleField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.field, this.context);
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

        @Override
        public abstract F end();
    }

    public static abstract sealed class BaseFieldBuilder<B extends BaseFieldBuilder<B, ?>, F extends IConfigField<?, ?>> {
        protected final String name;
        protected final ConfigGroup group;
        protected final Set<String> comments = new LinkedHashSet<>();
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

        public abstract F end();
    }
}
