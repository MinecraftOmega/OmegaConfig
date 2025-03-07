package org.omegaconfig;

import org.omegaconfig.api.IConfigField;
import org.omegaconfig.impl.fields.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public final class ConfigSpec extends ConfigGroup {
    private final String format;
    private final String suffix;
    private final Path filePath;
    private final HashSet<IConfigField<?, ?>> dirtyFields = new LinkedHashSet<>();
    private final int backups;
    boolean dirty;
    private ConfigSpec(String name, String format, String suffix, Path path, int backups) {
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
        if (field.spec() != this) throw new IllegalArgumentException("ConfigField requires to be updated by the intended spec");
        this.dirtyFields.add(field);
    }

    Set<IConfigField<?, ?>> dirtyFields() {
        return this.dirtyFields;
    }

    public Path path() {
        return this.filePath;
    }

    String format() {
        return this.format;
    }

    String suffix() {
        return this.suffix;
    }

    int backups() {
        return this.backups;
    }

    public static final class SpecBuilder {
        private final ConfigSpec spec;
        private ConfigGroup active;
        private final Set<String> comments = new LinkedHashSet<>();
        private final Set<IConfigField<?, ?>> fields = new LinkedHashSet<>();

        public SpecBuilder(String name, String format, String suffix, Path path, int backups) {
            this.spec = new ConfigSpec(name, format, suffix, path, backups);
            this.active = this.spec;
        }

        <T> ListFieldBuilder<T> defineList(String name, Field field, Object context, Class<T> subType) {
            return new ListFieldBuilder<>(name, this.active, field, context, subType);
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

        public SpecBuilder comment(String ...comment) {
            this.comments.addAll(Arrays.asList(comment));
            return this;
        }

        public SpecBuilder push(String name) {
            for (String n: name.split("\\.")) {
                this.active = new ConfigGroup(n, this.active);
            }
            return this;
        }

        public SpecBuilder pop() {
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
            this.spec.fields = Collections.unmodifiableSet(this.fields);
            this.spec.comments = Collections.unmodifiableSet(this.comments);
            return spec;
        }
    }

    public static non-sealed class CustomFieldBuilder<T, S> extends BaseFieldBuilder<CustomFieldBuilder<T, S>, BaseConfigField<T, S>> {
        private final String name;
        private final ConfigGroup group;
        private final T defaultValue;
        private final Class<T> type;
        private final Class<S> subType;

        public CustomFieldBuilder(String name, ConfigGroup group, T defaultValue, Class<T> type, Class<S> subType) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
            this.type = type;
            this.subType = subType;
        }

        public CustomFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this.name = name;
            this.group = group;
            this.defaultValue = null;
            this.field = field;
            this.context = context;
            this.type = (Class<T>) Tools.getType(field);
            this.subType = (Class<S>) Tools.getOneArgType(field);
        }

        @Override
        public BaseConfigField<T, S> end() {
            return new BaseConfigField<>(this.name, this.group, this.comments, this.defaultValue) {

                @Override
                public Class<T> type() {
                    return type;
                }

                @Override
                public Class<S> subType() {
                    return subType;
                }
            };
        }
    }

    public static final class ListFieldBuilder<S> extends BaseFieldBuilder<ListFieldBuilder<S>, ListField<S>> {
        private final String name;
        private final ConfigGroup group;
        private final List<S> defaultValue;
        private final Class<S> subType;
        private final Field field;
        private final Object context;

        private ListFieldBuilder(String name, ConfigGroup group, List<S> defaultValue, Class<S> subType) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
            this.subType = subType;
            this.field = null;
            this.context = null;
        }

        private ListFieldBuilder(String name, ConfigGroup group, Field field, Object context, Class<S> subType) {
            this.name = name;
            this.group = group;
            this.defaultValue = null;
            this.subType = subType;
            this.field = field;
            this.context = context;
        }

        public ListFieldBuilder<S> add(S e) {
            this.defaultValue.add(e);
            return this;
        }

        @Override
        public ListField<S> end() {
            if (this.field == null) {
                return new ListField<>(this.name, this.group, this.comments, this.defaultValue, this.subType);
            } else {
                return new ListField<>(this.name, this.group, this.comments, this.subType, this.field, this.context);
            }
        }
    }

    public static final class EnumFieldBuilder<T extends Enum<T>> extends BaseFieldBuilder<EnumFieldBuilder<T>, EnumField<T>> {
        private final String name;
        private final ConfigGroup group;
        private final T defaultValue;

        private EnumFieldBuilder(String name, ConfigGroup group, T defaultValue) {
            this.name = name;
            this.group = group;
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

    public static final class StringFieldBuilder extends BaseFieldBuilder<ShortFieldBuilder, StringField> {
        private final String name;
        private final ConfigGroup group;
        private final String defaultValue;

        private StringFieldBuilder(String name, ConfigGroup group, String defaultValue) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
        }

        private StringFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, null);
            this.field = field;
            this.context = context;
        }

        public StringField end() {
            if (this.field == null) {
                return new StringField(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new StringField(this.name, this.group, this.comments, this.field, this.context);
            }
        }
    }

    public static final class BooleanFieldBuilder extends BaseFieldBuilder<BooleanFieldBuilder, BooleanField> {
        private final String name;
        private final ConfigGroup group;
        private final boolean defaultValue;

        private BooleanFieldBuilder(String name, ConfigGroup group, boolean defaultValue) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
        }

        private BooleanFieldBuilder(String name, ConfigGroup group, Field field, Object context) {
            this(name, group, false);
            this.field = field;
            this.context = context;
        }

        public BooleanField end() {
            if (this.field == null) {
                return new BooleanField(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new BooleanField(this.name, this.group, this.comments, this.field, this.context);
            }
        }
    }

    public static final class ByteFieldBuilder extends NumberFieldBuilder<Byte, ByteField, ByteFieldBuilder> {
        private final String name;
        private final ConfigGroup group;
        private final byte defaultValue;

        private ByteFieldBuilder(String name, ConfigGroup group, byte defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final short defaultValue;

        private ShortFieldBuilder(String name, ConfigGroup group, short defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final char defaultValue;

        private CharFieldBuilder(String name, ConfigGroup group, char defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final int defaultValue;

        private IntFieldBuilder(String name, ConfigGroup group, int defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final long defaultValue;

        private LongFieldBuilder(String name, ConfigGroup group, long defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final float defaultValue;

        private FloatFieldBuilder(String name, ConfigGroup group, float defaultValue) {
            this.name = name;
            this.group = group;
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
        private final String name;
        private final ConfigGroup group;
        private final double defaultValue;

        private DoubleFieldBuilder(String name, ConfigGroup group, double defaultValue) {
            this.name = name;
            this.group = group;
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
     *
     * @param <T>
     * @param <B> builder which extends
     */
    public static abstract sealed class NumberFieldBuilder<T extends Number, F extends NumberField<T>, B extends NumberFieldBuilder<T, F, B>> extends BaseFieldBuilder<B, F> {
        protected boolean strictMath = false;
        protected boolean math = false;
        protected T min;
        protected T max;

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
        protected final Set<String> comments = new LinkedHashSet<>();
        protected Field field;
        protected Object context;

        public B comments(String ...comments) {
            this.comments.addAll(Arrays.asList(comments));
            return (B) this;
        }

        public abstract F end();
    }
}
