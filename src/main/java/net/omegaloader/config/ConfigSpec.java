package net.omegaloader.config;

import net.omegaloader.config.api.builder.*;
import net.omegaloader.config.core.Format;
import net.omegaloader.config.core.formats.IConfigFormat;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigSpec extends GroupField {

    private IConfigFormat format;
    private Object context;
    public final RandomAccessFile file;
    public final String suffix;
    boolean locked;

    private ConfigSpec(String filename, IConfigFormat format, Object context) {
        super(filename, null); // I AM THE PARENT
        this.format = format;
        this.context = context;
        this.file = format.generateFile(this);
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
        private final String format;
        private final List<BaseConfigField<?>> fields = new ArrayList<>();

        private GroupField currentGroup;
        private ConfigSpec endSpec; // just in case

        public Builder(String filename, String format) {
            this.filename = filename;
            this.format = format;
        }

        public ByteField defineByte(String name, byte defaultValue) {

        }

        public ByteField defineByte(String name, Field byteField) {

        }

        public ShortField defineShort(String name, short initalValue) {

        }

        public ShortField defineShort(String name, Field shortField) {

        }

        public IntField defineInt(String name, int defaultValue) {

        }

        public IntField defineInt(String name, Field intField) {

        }

        public LongField defineLong(String name, long defaultValue) {

        }

        public LongField defineLong(String name, Field longField) {

        }

        public FloatField defineFloat(String name, float defaultValue) {

        }

        public FloatField defineFloat(String name, Field fieldValue) {

        }

        public DoubleField defineDouble(String name, double defaultValue) {

        }

        public DoubleField defineDouble(String name, Field fieldValue) {

        }

        public StringField defineString(String name, String defaultValue) {

        }

        public StringField defineString(String name, Field fieldValue) {

        }

        public <T extends Enum<T>> EnumField<T> defineEnum(String name, Enum<T> defaultValue) {

        }

        public <T extends Enum<T>> EnumField<T> defineEnum(String name, Field field) {

        }

        public <T> ArrayField<T> defineList(String name, List<T> defaultValue) {

        }

        public <T> ArrayField<T> defineList(String name, Field field) {

        }

        public <T> MapField<String, T> defineMap(String name, Map<String, T> defaultValue) {

        }

        public <T> MapField<String, T> defineMap(String name, Field field) {

        }

//        public PathField definePath(String name, Path defaultValue) {
//
//        }
//
//        public PathField definePath(String name, Field field) {
//
//        }

        public Builder push(String name) {

        }

        public Builder popTo(String name) {

        }

        public Builder pop() {

        }

        public ConfigSpec build() {

        }

        ConfigSpec dispose() {
            fields.clear();

        }
    }
}