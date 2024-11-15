package net.omegaloader.config.builder;

import net.omegaloader.config.builder.field.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigSpecBuilder {
    private final String filename;
    private final ConfigFileFormat format;

    private final Map<String, BaseConfigField<?>> fields = new ArrayList<>();

    public ConfigSpecBuilder(String filename, ConfigFileFormat format) {
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

    public PathField definePath(String name, Path defaultValue) {

    }

    public PathField definePath(String name, Field field) {

    }

    public ConfigSpecBuilder push(String name) {

    }

    public ConfigSpecBuilder popTo(String name) {

    }

    public ConfigSpecBuilder pop() {

    }

    public ConfigSpec build() {

    }

    ConfigSpec dispose() {
        fields.clear();
        
    }

    private Map<String, BaseConfigField<?>> getContextFields(String builtName) {
        String[] names = builtName.split("\\.");
        if (names.length == 0) throw new Error("What the fuck!?");
        if (names.length == 1) return fields;

        Map<String, BaseConfigField<?>> context = fields;
        for (String name: names) {
            BaseConfigField<?> field = context.get(name);
            // missing? make a new one
            if (field == null) {
                GroupField f = new GroupField();
                context.put(name, f);
                return f.getFields();
            // existing? use it
            } else if (field.isGroup()) {
                context = field.getFields();
            // something is really wrong
            } else {
                throw new UnsupportedOperationException("Current context targets a non-group field");
            }
        }
        return context;
    }

}
