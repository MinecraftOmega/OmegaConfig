package org.omegaconfig;

import org.omegaconfig.api.IConfigField;
import org.omegaconfig.impl.fields.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public final class ConfigSpec extends ConfigGroup {
    private final Path filePath;
    private final String format;
    private final String suffix;
    private final HashSet<IConfigField<?, ?>> dirtyFields = new LinkedHashSet<>();
    private ConfigSpec(String name, Path path, String format, String suffix) {
        super(name, null);
        this.filePath = path;
        this.format = format;
        this.suffix = suffix;
    }

    public void markDirty(IConfigField<?, ?> field) {
        if (field.spec() != this) throw new IllegalArgumentException("ConfigField requires to be updated by the intended spec");
        this.dirtyFields.add(field);
    }

    Set<IConfigField<?, ?>> dirtyFields() {
        return this.dirtyFields;
    }

    Path path() {
        return this.filePath;
    }

    String format() {
        return this.format;
    }

    String suffix() {
        return this.suffix;
    }

    public static final class SpecBuilder {
        private final ConfigSpec spec;
        private ConfigGroup active;
        private final Set<String> comments = new LinkedHashSet<>();
        private final Set<IConfigField<?, ?>> fields = new LinkedHashSet<>();

        public SpecBuilder(String name, Path path, String format, String suffix) {
            this.spec = new ConfigSpec(name, path, format, suffix);
            this.active = this.spec;
        }

        public <T> ListFieldBuilder<T> defineList(String name, List<T> defaultValue, Class<T> subType) {
            return new ListFieldBuilder<>(name, this.active, defaultValue, subType);
        }

        <T> ListFieldBuilder<T> defineList(String name, Field field, Object context, Class<T> subType) {
            return new ListFieldBuilder<>(name, this.active, field, context, subType);
        }

        public <T extends Enum<T>> EnumFieldBuilder<T> defineEnum(String name, T defaultValue) {
            return new EnumFieldBuilder<>(name, this.active, defaultValue);
        }

        <T extends Enum<T>> EnumFieldBuilder<T> defineEnum(String name, T defaultValue, Field field, Object context) {
            return new EnumFieldBuilder<>(name, this.active, defaultValue, field, context);
        }

        public StringFieldBuilder defineString(String name, String defaultValue) {
            return new StringFieldBuilder(name, this.active, defaultValue);
        }

        StringFieldBuilder defineString(String name, Field field, Object context) {
            return new StringFieldBuilder(name, this.active, field, context);
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
            if (field == null) {
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
        private final Field field;
        private final Object context;

        private EnumFieldBuilder(String name, ConfigGroup group, T defaultValue) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
            this.field = null;
            this.context = null;
        }

        private EnumFieldBuilder(String name, ConfigGroup group, T defaultValue, Field field, Object context) {
            this.name = name;
            this.group = group;
            this.defaultValue = defaultValue;
            this.field = field;
            this.context = context;
        }

        @Override
        public EnumField<T> end() {
            if (field == null) {
                return new EnumField<>(this.name, this.group, this.comments, this.defaultValue);
            } else {
                return new EnumField<>(this.name, this.group, this.comments, field, context);
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
            this.name = name;
            this.group = group;
            this.defaultValue = null;
            this.field = field;
            this.context = context;
        }

        public StringField end() {
            return new StringField(this.name, this.group, this.comments, this.field, this.comments);
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

        public BooleanField end() {
            return new BooleanField(this.name, this.group, this.comments, this.defaultValue);
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

        @Override
        public ByteField end() {
            return new ByteField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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

        @Override
        public ShortField end() {
            return new ShortField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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


        @Override
        public CharField end() {
            return new CharField(this.name, this.group, this.comments, this.defaultValue);
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

        @Override
        public IntField end() {
            return new IntField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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

        @Override
        public LongField end() {
            return new LongField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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

        @Override
        public FloatField end() {
            return new FloatField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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

        @Override
        public DoubleField end() {
            return new DoubleField(this.name, this.group, this.comments, this.math, this.strictMath, this.min, this.max, this.defaultValue);
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
