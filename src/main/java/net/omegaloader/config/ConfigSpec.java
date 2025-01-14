package net.omegaloader.config;

import net.omegaloader.config.api.builder.*;
import net.omegaloader.config.formats.IConfigFormat;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigSpec extends GroupField {

    private final IConfigFormat format;
    public final RandomAccessFile file;
    public final String suffix;
    private boolean locked;

    private ConfigSpec(String filename, String suffix, IConfigFormat format, Object context) {
        super(filename, null); // I AM THE PARENT
        this.format = format;
        this.file = format.generateFile(this);
        this.suffix = suffix;
    }

    public IConfigFormat format() {
        return format;
    }

    public boolean isLocked() {
        return locked;
    }

    public void save() {

    }

    public static class Builder {
        private final String filename;
        private final String suffix;
        private final String format;
        private final List<BaseConfigField<?>> fields = new ArrayList<>();

        private Object currentContext;
        private GroupField currentGroup;
        private ConfigSpec endSpec; // just in case

        public Builder(String filename, String suffix, String format, Object context) {
            this.filename = filename;
            this.suffix = suffix;
            this.format = format;
            this.currentContext = context;
            this.currentGroup = new ConfigSpec(this.filename, this.suffix, IConfigFormat.getByName(format), context);
        }

        public ByteField defineByte(String name, Field field) {
            return this.currentGroup.putField(new ByteField(name, this.currentGroup, this.currentContext, field));
        }

        public ShortField defineShort(String name, Field field) {
            return this.currentGroup.putField(new ShortField(name, this.currentGroup, this.currentContext, field));
        }

        public IntField defineInt(String name, Field field) {
            return this.currentGroup.putField(new IntField(name, this.currentGroup, this.currentContext, field));
        }

        public LongField defineLong(String name, Field field) {
            return this.currentGroup.putField(new LongField(name, this.currentGroup, this.currentContext, field));
        }

        public FloatField defineFloat(String name, Field field) {
            return this.currentGroup.putField(new FloatField(name, this.currentGroup, this.currentContext, field));
        }

        public DoubleField defineDouble(String name, Field field) {
            return this.currentGroup.putField(new DoubleField(name, this.currentGroup, this.currentContext, field));
        }

        public StringField defineString(String name, Field field) {
            return this.currentGroup.putField(new StringField(name, this.currentGroup, this.currentContext, field));
        }

        public <T extends Enum<T>> EnumField<T> defineEnum(String name, Field field) {
            return this.currentGroup.putField(new EnumField<>(name, this.currentGroup, this.currentGroup, field));
        }

        public <T> ArrayField<T> defineArray(String name, Field field) {
            return this.currentGroup.putField(new ArrayField<>(name, this.currentGroup, this.currentContext, field));
        }

        public ByteField defineByte(String name, byte defaultValue) {
            return this.currentGroup.putField(new ByteField(name, this.currentGroup, defaultValue));
        }

        public ShortField defineShort(String name, short defaultValue) {
            return this.currentGroup.putField(new ShortField(name, this.currentGroup, defaultValue));
        }

        public IntField defineInt(String name, int defaultValue) {
            return this.currentGroup.putField(new IntField(name, this.currentGroup, defaultValue));
        }

        public LongField defineLong(String name, long defaultValue) {
            return this.currentGroup.putField(new LongField(name, this.currentGroup, defaultValue));
        }

        public FloatField defineFloat(String name, float defaultValue) {
            return this.currentGroup.putField(new FloatField(name, this.currentGroup, defaultValue));
        }

        public DoubleField defineDouble(String name, double defaultValue) {
            return this.currentGroup.putField(new DoubleField(name, this.currentGroup, defaultValue));
        }

        public StringField defineString(String name, String defaultValue) {
            return this.currentGroup.putField(new StringField(name, this.currentGroup, defaultValue));
        }

        public <T extends Enum<T>> EnumField<T> defineEnum(String name, T defaultValue) {
            return this.currentGroup.putField(new EnumField<>(name, this.currentGroup, defaultValue));
        }

        public <T> ArrayField<T> defineArray(String name, T[] defaultValue) {
            return this.currentGroup.putField(new ArrayField<>(name, this.currentGroup, defaultValue));
        }

        public <T> MapField<T> defineMap(String name, Map<String, T> defaultValue) {
            return this.currentGroup.putField(new MapField<T>(name, this.currentGroup, defaultValue));
        }

        public <T> MapField<T> defineMap(String name, Field field) {
            return this.currentGroup.putField(new MapField<T>(name, this.currentGroup, this.currentContext, field));
        }

        public Builder push(String name) {
            GroupField group = new GroupField(name, this.currentGroup);

            this.currentGroup.putField(group);
            this.currentGroup = group;
            return this;
        }

        public Builder push(String name, Object context) {
            GroupField group = new GroupField(name, this.currentGroup);

            this.currentGroup.putField(group);
            this.currentGroup = group;
            this.currentContext = context;
            return this;
        }

        public Builder pop() {
            var group = this.currentGroup.getParent();

            if (currentGroup == group)
                throw new IllegalStateException("You can't pop above the spec");

            this.currentGroup = group;
            this.currentContext = group.context();
            return this;
        }

        public Builder pop(int times) {
            if (times < 1) throw new IllegalArgumentException("Times must be above 1");
            while (times != 0) {
                this.pop();
                times--;
            }
            return this;
        }

        public ConfigSpec build() {
            ConfigSpec spec = this.currentGroup.getSpec();
            spec.locked = true;
            this.dispose();
            return spec;
        }

        void dispose() {
            fields.clear();
        }
    }
}